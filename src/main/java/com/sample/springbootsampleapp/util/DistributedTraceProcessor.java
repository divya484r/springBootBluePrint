package com.sample.springbootsampleapp.util;

import com.amazonaws.services.sqs.model.Message;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import wingtips.Span;
import wingtips.Span.SpanPurpose;
import wingtips.TraceHeaders;
import wingtips.Tracer;
import wingtips.aws.general.util.WingtipsAwsGeneralUtil;
import wingtips.aws.sqs.WingtipsAwsSqsUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static wingtips.util.TracerManagedSpanStatus.MANAGED_CURRENT_ROOT_SPAN;
import static wingtips.util.TracerManagedSpanStatus.MANAGED_CURRENT_SUB_SPAN;

/**
 * Use this processor in your Camel route to automatically add Distributed Tracing headers to Camel message headers and
 * exchange properties, and to outgoing HTTP requests via Camel message headers.
 * <p>
 * The processor first looks for X-B3-TraceId and X-B3-SpanId headers that may already be on the camel message,
 * then if not found it looks for those values in the camel exchange properties, then if still not found it looks for a
 * Wingtips-XB3-TraceContext property on the camel message body, first assuming the body is an SNS notification and
 * then, if still not found, assuming the body is an SQS message as defined by
 * <code>com.amazonaws.services.sqs.model.Message</code>.
 * <p>
 * Trace ID values are set on Camel exchange properties, in addition to message headers, so they can be read when an
 * exchange Out message is thrown to an exception handler.
 * <p>
 * If no trace id is found in message headers, exchange properties, an SNS notification, or an SQS message, then a new
 * trace id and context are created with root span.
 */

@Slf4j
@Component("DistributedTraceProcessor")
public class DistributedTraceProcessor implements Processor {

    @Override
    public void process(final Exchange exchange) {
        val headers = getDistributedTraceValues(exchange);
        val in = exchange.getIn();
        for (Map.Entry entry : headers.entrySet()) {
            if (entry.getValue() == null || entry.getValue().toString().equals("null")) {
                log.warn("The value for " + entry.getKey() + " is null, so removing its header and property...");
                in.removeHeader((String) entry.getKey());
                exchange.removeProperty((String) entry.getKey());
            } else {
                in.setHeader((String) entry.getKey(), entry.getValue());
                exchange.setProperty((String) entry.getKey(), entry.getValue());
            }
        }
    }

    private Map<String, Object> getDistributedTraceValues(Exchange exchange) {
        String outgoingTraceId = null;
        String outgoingSpanId = null;
        String isSampleable = null;
        String outgoingParentSpanId = null;

        try {
            val incomingTraceId = getIncomingTraceId(exchange);

            outgoingTraceId = getOutgoingTraceId(incomingTraceId);
            Span currentSpan = Tracer.getInstance().getCurrentSpan();
            outgoingSpanId = currentSpan.getSpanId();
            isSampleable = (currentSpan.isSampleable()) ? "1" : "0";
            outgoingParentSpanId = Tracer.getInstance().getCurrentSpan().getParentSpanId();

        } catch (Exception e) {
            String errorMessage = "An Exception occurred while getting distributed trace values. Camel message headers and "
                    + " exchange properties for  uncollected trace values will be removed.";
            log.warn(errorMessage, e.getMessage());
            log.debug(errorMessage, e);
        } finally {
            // Incoming headers need to be replaced, so build map even with null values if exception occurred.
            return buildHeaderMap(outgoingTraceId, outgoingSpanId, isSampleable, outgoingParentSpanId);
        }
    }

    private Map<String, Object> buildHeaderMap(String traceId, String spanId, String isSampleable, String parentSpanId) {
        Map<String, Object> headers = new HashMap<>();

        headers.put(TraceHeaders.TRACE_ID, traceId);
        headers.put(TraceHeaders.SPAN_ID, spanId);
        headers.put(TraceHeaders.TRACE_SAMPLED, isSampleable);
        headers.put(TraceHeaders.PARENT_SPAN_ID, parentSpanId);

        return headers;
    }

    private String getOutgoingTraceId(Object incomingTraceId) {
        if (incomingTraceId == null) {
            // Even if there is a current span but there is no traceId in Camel headers or SNS/SQS message attributes,
            // we need to start a new span stack from the root.
            return getTraceIdFromNewRootSpan();
        } else {
            Span span = createSpanWithIncomingTraceId(incomingTraceId);
            return span.getTraceId();
        }
    }

    private String getTraceIdFromNewRootSpan() {
        Tracer tracer = Tracer.getInstance();
        completeCurrentSpanStack();
        val span = tracer.startRequestWithRootSpan("CamelRouteRootSpan");
        return span.getTraceId();
    }

    private Span createSpanWithIncomingTraceId(Object incomingTraceId) {
        Tracer tracer = Tracer.getInstance();
        Span currentSpan = tracer.getCurrentSpan();
        if (currentSpan != null && currentSpan.getTraceId().equals(incomingTraceId)) {
            // The existing traceId will automatically propagate to the new sub span.
            return tracer.startSubSpan("CamelRouteSubSpan", SpanPurpose.UNKNOWN);
        } else {
            // If there is no current span that has the incoming traceId that came in the Camel headers or SNS/SQS message,
            // attributes, then we need to complete the existing span stack and create a new one from the root, with the
            // incoming traceId.
            completeCurrentSpanStack();
            return tracer.startRequestWithSpanInfo(incomingTraceId.toString(), null,
                    "CamelRouteRootSpan", true, null, SpanPurpose.UNKNOWN);
        }
    }

    /**
     * Completes all spans in the stack, including the root span.
     */
    public static void completeCurrentSpanStack() {
        Tracer tracer = Tracer.getInstance();
        Span currentSpan = tracer.getCurrentSpan();
        while (currentSpan != null) {
            if (currentSpan.getCurrentTracerManagedSpanStatus().equals(MANAGED_CURRENT_ROOT_SPAN)) {
                tracer.completeRequestSpan();
            } else {
                completeSubSpans();
            }
            currentSpan = tracer.getCurrentSpan();
        }
    }

    /**
     * Completes all sub spans in the stack. Does not complete the root span.
     *
     * Example use case: closing all sub spans created by Camel routes that are behind a web endpoint that is configured
     * with a web trace filter, leaving the root span intact to be handled by the web trace filter.
     */
    public static void completeSubSpans() {
        Tracer tracer = Tracer.getInstance();
        Span currentSpan = tracer.getCurrentSpan();
        while (currentSpan != null) {
            if (currentSpan.getCurrentTracerManagedSpanStatus().equals(MANAGED_CURRENT_ROOT_SPAN)) {
                return;
            } else if (currentSpan.getCurrentTracerManagedSpanStatus().equals(MANAGED_CURRENT_SUB_SPAN)) {
                tracer.completeSubSpan();
            }
            currentSpan = tracer.getCurrentSpan();
        }
    }

    private Object getIncomingTraceId(Exchange exchange) {
        Object incomingTraceId = exchange.getIn().getHeader(TraceHeaders.TRACE_ID);

        if (incomingTraceId == null) {
            incomingTraceId = exchange.getProperty(TraceHeaders.TRACE_ID);
        }
        if (incomingTraceId == null) {
            incomingTraceId = getSnsNotificationTraceId(exchange);
        }
        if (incomingTraceId == null) {
            incomingTraceId = getSqsMessageTraceId(exchange);
        }
        if (incomingTraceId == null) {
            incomingTraceId = getSqsMessageAttributesTraceId(exchange);
        }
        return incomingTraceId;
    }

    private String getSnsNotificationTraceId(Exchange exchange) {
        try {
            return getSnsNotificationTraceContext(exchange).getTraceId();
        } catch (Exception e) {
            String warnMsg = "Wingtips traceId not found in SNS notification";
            String debugMsg = String.format(warnMsg + ": %s: ", exchange.getIn().getBody().toString());
            log.warn(warnMsg, e.getMessage());
            log.debug(debugMsg, e);
        }
        return null;
    }

    private String getSqsMessageTraceId(Exchange exchange) {
        try {
            Message sqsMessage = getSqsMessage(exchange);
            Span sqsMessageTraceContext = WingtipsAwsSqsUtil.extractTraceContext(sqsMessage);
            return sqsMessageTraceContext.getTraceId();
        } catch (Exception e) {
            String warnMsg = "Wingtips traceId not found in SQS message";
            String debugMsg = String.format(warnMsg + ": %s: ", exchange.getIn().getBody().toString());
            log.warn(warnMsg, e.getMessage());
            log.debug(debugMsg, e);
        }
        return null;
    }

    private Span getSnsNotificationTraceContext(Exchange exchange) {
        String traceContext = getSnsNotification(exchange).get("MessageAttributes").get("Wingtips-XB3-TraceContext")
                .get("Value").textValue();
        return WingtipsAwsGeneralUtil.fromTraceContextMessageAttributeValue(traceContext);
    }

    private JsonNode getSnsNotification(Exchange exchange) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
            return mapper.readValue((String) exchange.getIn().getBody(), JsonNode.class);

        } catch (IOException e) {
            String warnMsg = "Incoming message not serializable to com.fasterxml.jackson.databind.JsonNode";
            String debugMsg = String.format(warnMsg + ": %s: ",
                    exchange.getIn().getBody().toString());
            log.warn(warnMsg, e.getMessage());
            log.debug(debugMsg, e);
        }
        return null;
    }

    private Message getSqsMessage(Exchange exchange) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
            return mapper.readValue((String) exchange.getIn().getBody(), Message.class);

        } catch (IOException e) {
            String warnMsg = "Incoming message not serializable to com.amazonaws.services.sqs.model.Message";
            String debugMsg = String.format(warnMsg + ": %s: ",
                    exchange.getIn().getBody().toString());
            log.warn(warnMsg, e.getMessage());
            log.debug(debugMsg, e);
        }
        return null;
    }

    private String getSqsMessageAttributesTraceId(Exchange exchange) {
        try {
            String traceId = null;
            String traceContext = exchange.getIn().getHeader("Wingtips-XB3-TraceContext", String.class);
            if (traceContext != null) {
                traceId = WingtipsAwsGeneralUtil.fromTraceContextMessageAttributeValue(traceContext).getTraceId();
            }
            return traceId;
        } catch (Exception e) {
            String warnMsg = "Exception while retrieving Camel Exchange header property Wingtips-XB3-TraceContext ";
            String debugMsg = String.format(warnMsg + ": %s: ",
                    exchange.getIn().getBody().toString());
            log.warn(warnMsg, e.getMessage());
            log.debug(debugMsg, e);
        }
        return null;
    }
}
