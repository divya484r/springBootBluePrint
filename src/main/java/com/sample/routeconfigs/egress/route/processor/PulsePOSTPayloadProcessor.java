package com.sample.routeconfigs.egress.route.processor;

import com.sample.routeconfigs.common.exception.PulseTrafficRoutingException;
import com.sample.routeconfigs.common.model.pulse.EventContext;
import com.sample.routeconfigs.common.model.pulse.EventData;
import com.sample.routeconfigs.common.model.pulse.Pulse;
import wingtips.TraceHeaders;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static com.sample.routeconfigs.common.model.pulse.EventContext.EVENT_CONTEXT_FILTER_MAP_ENTRY_SETS;


/**
 * This processor builds the payload that is sent to Pulse.
 * <p>
 * Some pre-set camel exchange message headers are required; see the documentation of the constants in this class for details.
 * Also required are headers set by these fields:
 *
 * <ul>
 *     <li>{@link EventContext#BUSINESS_KEY_NAME}</li>
 *     <li>{@link EventContext#BUSINESS_KEY_VALUE}</li>
 *     <li>{@link EventContext#NAME}</li>
 *     <li>{@link EventData#CONTENT_TYPE}</li>
 *     <li>{@link EventData#ENCODING}</li>
 * </ul>
 *
 * Optional headers:
 * <ul>
 *     <li>{@link EventContext#RETENTION_DAYS}</li>
 *     <li>{@link EventContext#TYPE}</li>
 *     <li>{@link EventContext#VERSION}</li>
 * </ul>
 *
 * The client application is responsible for setting headers prior to this processor being used.
 */
@Slf4j
@Component
public class PulsePOSTPayloadProcessor implements Processor {


    @Override
    public void process(Exchange exchange) throws Exception {
        Message message = exchange.getIn();
        String businessKey = (String) message.getHeader(EventContext.BUSINESS_KEY_VALUE);

        Pulse request = buildPulseRequest(exchange);
        validate(request, businessKey);
        message.setBody(request);
    }

    private Pulse buildPulseRequest(Exchange exchange) {
        Message message = exchange.getIn();

        String traceId = (String) message.getHeader(TraceHeaders.TRACE_ID);
        String businessKey = (String) message.getHeader(EventContext.BUSINESS_KEY_VALUE);
        String businessKeyName = (String) message.getHeader(EventContext.BUSINESS_KEY_NAME);
        Map<String, String> filterMapEntrySets = (HashMap<String, String>) message.getHeader(EVENT_CONTEXT_FILTER_MAP_ENTRY_SETS);
        String eventContextType = (String) message.getHeader(EventContext.TYPE);
        String eventContextName = (String) message.getHeader(EventContext.NAME);
        String eventContextRetentionDays = (String) message.getHeader(EventContext.RETENTION_DAYS);
        String eventContextVersion = (String) message.getHeader(EventContext.VERSION);
        String eventDataContentType = (String) message.getHeader(EventData.CONTENT_TYPE);
        String eventDataEncoding = (String) message.getHeader(EventData.ENCODING);

        String body = (String) message.getBody();

        String date = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .withZone( ZoneId.of("UTC") ).format(Instant.now());

        EventContext eventContext = new EventContext();
        eventContext.setBusinessKeyValue(businessKey);
        eventContext.setDate(date);
        eventContext.setBusinessKeyName(businessKeyName);

        if (!StringUtils.isEmpty(eventContextType)) {
            eventContext.setType(eventContextType);
        }

        eventContext.setName(eventContextName);
        eventContext.setRetentionDays(eventContextRetentionDays);
        eventContext.setVersion(eventContextVersion);


        if(filterMapEntrySets!=null && filterMapEntrySets.size() > 0) {
            eventContext.getFilterMap().putAll(filterMapEntrySets);
        }

        eventContext.getMetaData().put(TraceHeaders.TRACE_ID, traceId);

        Pulse pulseRequest = new Pulse(eventContext, new EventData(eventDataContentType, eventDataEncoding, body));

        log.info("Built Event Management request payload for traceId = {}, businessKey = {} with date = {}",
                traceId, businessKey, date);

        return pulseRequest;
    }

    private void validate(Pulse request, String businessKey) throws Exception {
        if (StringUtils.isEmpty(request.getEventContext().getBusinessKeyName())) {
            throw new PulseTrafficRoutingException(String.format("The business key name was not properly set on the Pulse "
                    + "event context for businessKey = %s.", businessKey));
        }
        if (StringUtils.isEmpty(request.getEventContext().getBusinessKeyValue())) {
            throw new PulseTrafficRoutingException(String.format("The business key value was not properly set on the Pulse "
                    + "event context for businessKey = %s.", businessKey));
        }
        if (StringUtils.isEmpty(request.getEventContext().getName())) {
            throw new PulseTrafficRoutingException(String.format("The event context name was not properly set on the Pulse "
                    + "event context for businessKey = %s.", businessKey));
        }
        if (StringUtils.isEmpty(request.getData().getContentType())) {
            throw new PulseTrafficRoutingException(String.format("The event data content type was not properly set on the Pulse "
                    + "eventdata  payload for businessKey = %s.", businessKey));
        }
        if (StringUtils.isEmpty(request.getData().getEncoding())) {
            throw new PulseTrafficRoutingException(String.format("The event data encoding was not properly set on the Pulse "
                    + "event data payload for businessKey = %s.", businessKey));
        }
        if (StringUtils.isEmpty(request.getData().getValue())) {
            throw new PulseTrafficRoutingException(String.format("The data value was not properly set on the Pulse payload "
                    + " for businessKey = %s.", businessKey));
        }
    }


}
