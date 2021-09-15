package wingtips.aws.general.util;

import wingtips.aws.sns.WingtipsAwsSnsUtil;
import wingtips.aws.sqs.WingtipsAwsSqsUtil;
import wingtips.Span;
import wingtips.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wingtips.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Contains general-purpose static helper methods for working with Wingtips in an AWS SDK environment. There are
 * other classes for handling dependency-specific AWS SDK things, e.g. {@link
 * WingtipsAwsSqsUtil WingtipsAwsSqsUtil} for SQS-specific things that require the SQS SDK
 * dependency, or {@link WingtipsAwsSnsUtil WingtipsAwsSnsUtil} for SNS-specific things that
 * require the SNS SDK dependency.
 *
 * <p>Using this class does not require any AWS SDK dependency.
 *
 * @author Nic Munroe
 */
@SuppressWarnings("WeakerAccess")
public class WingtipsAwsGeneralUtil {

    private static final Logger logger = LoggerFactory.getLogger(WingtipsAwsGeneralUtil.class);

    /**
     * The message attribute name when passing trace context along as a SNS/SQS message attribute. See {@link
     * #toTraceContextMessageAttributeValue(Span)} for a helper method that generates a trace context message
     * attribute value from a {@link Span}, and {@link #fromTraceContextMessageAttributeValue(String)} for a helper
     * method that deserializes a trace context message attribute value back to a {@link Span}.
     */
    public static final String TRACE_CONTEXT_MESSAGE_ATTR_NAME = "Wingtips-XB3-TraceContext";

    /**
     * Intentionally protected constructor - use the public static methods.
     */
    protected WingtipsAwsGeneralUtil() {
        // Do nothing.
    }

    /**
     * Returns the given span, converted to a single "trace context" string suitable for sending to SNS/SQS as a
     * message attribute. This method returns the message attribute value; see {@link #TRACE_CONTEXT_MESSAGE_ATTR_NAME}
     * for the message attribute name. This trace context string can be fed into {@link
     * #fromTraceContextMessageAttributeValue(String)} to deserialize it back into a {@link Span} with the relevant
     * fields set to the values stored in the trace context.
     *
     * <p>The format for the returned trace context is:
     * <pre>
     *     [FormatVersion]:[TraceID]:[SpanID]:[Sampled]
     * </pre>
     *
     * <ul>
     *     <li>
     *         [FormatVersion] - The version of the trace context format - will always be "v1" until we have need of
     *         other formats.
     *     </li>
     *     <li>[TraceID] - The {@link Span#getTraceId()} from the given span - should never be empty.</li>
     *     <li>[SpanID] - The {@link Span#getSpanId()} from the given span - should never be empty.</li>
     *     <li>[Sampled] - This will be "1" if {@link Span#isSampleable()} is true, otherwise "0"</li>
     * </ul>
     *
     * Fields in the trace context are separated by a colon (':'). For example, for a sampled span this method would
     * return:
     * <pre>
     *     v1:20f78a4c0a1c7662:c0bb9d174f23ae9d:1
     * </pre>
     *
     * The same span but not sampled would look like:
     * <pre>
     *     v1:20f78a4c0a1c7662:c0bb9d174f23ae9d:0
     * </pre>
     *
     * @param span The span to convert to a trace context.
     * @return The given span, converted to a single "trace context" string suitable for sending to SNS/SQS as a
     * message attribute.
     */
    public static String toTraceContextMessageAttributeValue(Span span) {
        String sampledValue = (span.isSampleable()) ? "1" : "0";

        return "v1:" +
               escapeColons(span.getTraceId()) + ":" +
               escapeColons(span.getSpanId()) + ":" +
               sampledValue;
    }

    protected static final String URL_ENCODED_COLON;

    static {
        try {
            URL_ENCODED_COLON = URLEncoder.encode(":", StandardCharsets.UTF_8.name());
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    protected static String escapeColons(String unescaped) {
        if (unescaped == null) {
            return null;
        }

        return unescaped.replace(":", URL_ENCODED_COLON);
    }

    protected static String unescapeColons(String escaped) {
        if (escaped == null) {
            return null;
        }

        return escaped.replace(URL_ENCODED_COLON, ":");
    }

    /**
     * Returns a {@link Span} with trace ID, span ID, and sampleable values set based on the given trace context
     * string, or null if the given string is not a valid trace context. This is the other half of {@link
     * #toTraceContextMessageAttributeValue(Span)}, which converts spans to trace context strings for the
     * purpose of sending to SNS/SQS as message attributes. The trace context is the message attribute value;
     * see {@link #TRACE_CONTEXT_MESSAGE_ATTR_NAME} for the message attribute name.
     *
     * <p>The format for the trace context is:
     * <pre>
     *     [FormatVersion]:[TraceID]:[SpanID]:[Sampled]
     * </pre>
     *
     * <ul>
     *     <li>
     *         [FormatVersion] - The version of the trace context format - will always be "v1" until we have need of
     *         other formats.
     *     </li>
     *     <li>[TraceID] - The {@link Span#getTraceId()} from the trace context - should never be empty.</li>
     *     <li>[SpanID] - The {@link Span#getSpanId()} from the trace context - should never be empty.</li>
     *     <li>
     *         [Sampled] - This will be "1" if the trace context's {@link Span#isSampleable()} is true,
     *         otherwise "0"
     *     </li>
     * </ul>
     *
     * Fields in the trace context are separated by a colon (':'). For example, for a sampled span this method would
     * expect a trace context that looks like:
     * <pre>
     *     v1:20f78a4c0a1c7662:c0bb9d174f23ae9d:1
     * </pre>
     *
     * The same trace context but not sampled would look like:
     * <pre>
     *     v1:20f78a4c0a1c7662:c0bb9d174f23ae9d:0
     * </pre>
     *
     * <p>Since trace contexts don't contain span name, the returned {@link Span#getSpanName()} will be defaulted to
     * something arbitrary and should not be relied on for anything specific. If the given traceContext string cannot
     * be converted to a span using the specified format then null will be returned.
     *
     * @param traceContext The trace context to convert to a span.
     * @return A {@link Span} with trace ID, span ID, and sampleable values set based on the given trace context
     * string, or null if the given string is not a valid trace context.
     */
    @SuppressWarnings("RedundantConditionalExpression")
    public static wingtips.Span fromTraceContextMessageAttributeValue(String traceContext) {
        if (StringUtils.isBlank(traceContext)) {
            return null;
        }

        String[] parts = traceContext.split(":");
        if (parts.length != 4) {
            logger.warn("Invalid trace context - did not contain 4 parts separated by a colon ':'. "
                        + "invalid_trace_context={}", traceContext);
            return null;
        }

        String versionId = parts[0];
        String traceId = unescapeColons(parts[1]);
        String spanId = unescapeColons(parts[2]);
        boolean sampleable = ("0".equals(parts[3])) ? false : true;

        if (!"v1".equals(versionId)) {
            logger.warn("Unhandled trace context version. Returning null. "
                        + "unhandled_trace_context_version={}, unhandled_trace_context={}", versionId, traceContext);
            return null;
        }

        if (StringUtils.isBlank(traceId) || StringUtils.isBlank(spanId)) {
            logger.warn("Invalid trace context - Trace ID and Span ID must both be non-empty. "
                        + "invalid_trace_context={}", traceContext);
            return null;
        }

        return Span.newBuilder("syntheticParentTraceContext", Span.SpanPurpose.CLIENT)
                   .withTraceId(traceId)
                   .withSpanId(spanId)
                   .withSampleable(sampleable)
                   .build();
    }

    /**
     * Continues the trace represented by the given parentSpan by creating a new child span from the given parent
     * and attaching it to the current thread as the {@link Tracer#getCurrentSpan()}. <b>NOTE: You must call {@link
     * Tracer#completeRequestSpan()} when you're done doing the work for this child span (i.e. when you're done
     * processing the SQS message that contained parentSpan as its trace context)!</b> Otherwise you will (1) fail
     * to complete the child span leading to incorrect tracing data in your logs, and (2) leave tracing info on this
     * thread which could pollute other future requests that are processed on this thread.
     *
     * <p>WARNING: This method is built mainly for SQS message processing and assumes there is no current trace
     * running for the current thread. It expects you to call this method when you're running on the thread that is
     * supposed to execute the SQS message processing, with a *guarantee* that {@link Tracer#completeRequestSpan()}
     * will be called on this thread (i.e. in a finally block) when message processing is done to finish the returned
     * span and clean things up on this thread. If this thread is "polluted" with a span already existing on {@link
     * Tracer#getCurrentSpan()} when this method is called then the existing span will be tossed aside so that this
     * method can do what it's supposed to do.
     *
     * @param parentSpan The parent span that this method will create a child span from. This should not be null -
     * if null is passed for this argument then a warning will be logged and a new root span (not child span) will be
     * created and used instead.
     * @param newChildSpanName The {@link Span#getSpanName()} you want for the new child span. Cannot be null - if
     * you pass null then a {@link IllegalArgumentException} will be thrown.
     * @param optionalUserId The user ID that should be associated with the new child span, or you can safely pass null
     * if you don't have a user ID (or don't want a user ID) for the new child span.
     * @return The new child span that has been created from the given parent span and attached to this thread so
     * that {@link Tracer#getCurrentSpan()} returns the same {@link Span}.
     */
    public static Span continueTraceFromParentSpanOnThisThread(
        Span parentSpan, String newChildSpanName, String optionalUserId
    ) {
        if (parentSpan == null) {
            Span returnVal = Tracer.getInstance().startRequestWithRootSpan(newChildSpanName, optionalUserId);
            logger.warn("Call to continueTraceFromParentSpanOnThisThread(...) with a null parentSpan. Starting a new "
                        + "trace (root span) instead. new_root_span_trace_id={}", returnVal.getTraceId());
            return returnVal;
        }

        return Tracer.getInstance().startRequestWithSpanInfo(
            parentSpan.getTraceId(),
            parentSpan.getSpanId(),
            newChildSpanName,
            parentSpan.isSampleable(),
            optionalUserId,
            Span.SpanPurpose.SERVER
        );
    }

}
