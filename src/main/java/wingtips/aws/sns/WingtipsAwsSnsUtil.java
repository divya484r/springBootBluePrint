package wingtips.aws.sns;

import wingtips.aws.general.util.WingtipsAwsGeneralUtil;
import wingtips.aws.sqs.WingtipsAwsSqsUtil;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;
import wingtips.Span;
import wingtips.Tracer;

import static wingtips.aws.general.util.WingtipsAwsGeneralUtil.toTraceContextMessageAttributeValue;

/**
 * Contains SNS-specific static helper methods for working with Wingtips in an environment that utilizes the AWS
 * SNS SDK.
 *
 * <p><b>DEPENDENCY WARNING: Using this class requires the {@code com.amazonaws:wingtips.aws-java-sdk-sns} AWS SDK dependency</b>
 * (or the full AWS SDK dependency). If you try to use this class without pulling in that dependency you will get
 * class-not-found or similar runtime errors. You must pull in that dependency explicitly in your project since this
 * module does not publish it as a transitive dependency.
 *
 * <p><b>USAGE:</b> Typical usage of this class looks like:
 *
 * <ul>
 *     <li>
 *          <b>Add trace context to a SNS publish message:</b>
 *          <pre>
 *              import static wingtips.wingtips.aws.sns.WingtipsAwsSnsUtil.withTraceContext;
 *
 *              // ...
 *              
 *              Span traceContextSpan = Tracer.getInstance().getCurrentSpan();
 *              snsClient.publish(withTraceContext(somePublishRequest, traceContextSpan));
 *          </pre>
 *     </li>
 *     <li>
 *          <b>Retrieving the trace context on the other end of a SNS->SQS message:</b> See {@link
 *          WingtipsAwsSqsUtil WingtipsAwsSqsUtil}, and in particular {@link
 *          WingtipsAwsSqsUtil#extractTraceContext(com.amazonaws.services.sqs.model.Message)
 *          WingtipsAwsSqsUtil.extractTraceContext(sqsMessage)}.
 *     </li>
 * </ul>
 *
 * @author Nic Munroe
 */
public class WingtipsAwsSnsUtil {

    /**
     * The message attribute name when passing trace context along as a SNS message attribute. See {@link
     * #traceContextMessageAttributeValueForSpan(Span)} for a helper method that generates a trace context
     * message attribute value from a {@link Span} for use when publishing SNS messages, and {@link
     * #withTraceContext(PublishRequest, Span)} for a helper that utilizes this attribute name to set the trace context
     * message attribute on SNS publish messages. For extracting the trace context on the other end when SNS pushes
     * to SQS queues, see {@link
     * WingtipsAwsSqsUtil#extractTraceContext(com.amazonaws.services.sqs.model.Message)}
     */
    public static final String TRACE_CONTEXT_MESSAGE_ATTR_NAME = WingtipsAwsGeneralUtil.TRACE_CONTEXT_MESSAGE_ATTR_NAME;

    /**
     * Adds the trace context message attribute to the given {@link PublishRequest} when publishing a SNS message.
     * If this SNS message is sent to SQS queues then the trace context can be extracted on the other end when
     * receiving the SQS message by calling {@link
     * WingtipsAwsSqsUtil#extractTraceContext(com.amazonaws.services.sqs.model.Message)}.
     *
     * <p>NOTE: You must pass in the {@link Span} that represents the trace context for the message. If you're not
     * sure what this should be, then it's likely the "current" Wingtips span retrieved via {@link
     * Tracer#getCurrentSpan()}.
     *
     * <p>Example use case for this method:
     * <pre>
     *      Span traceContextSpan = Tracer.getInstance().getCurrentSpan();
     *      snsClient.publish(withTraceContext(somePublishRequest, traceContextSpan));
     * </pre>
     *
     * @param original The original publish request.
     * @param traceContextSpan The Wingtips {@link Span} representing the trace context for the SNS message.
     * @return The given original publish request, but with a Wingtips trace context message attribute added.
     */
    public static PublishRequest withTraceContext(PublishRequest original, Span traceContextSpan) {
        return original.addMessageAttributesEntry(TRACE_CONTEXT_MESSAGE_ATTR_NAME,
                                                  traceContextMessageAttributeValueForSpan(traceContextSpan));
    }

    /**
     * @param span The span to create the trace context message attribute value for.
     * @return A SNS {@link MessageAttributeValue} containing the trace context value for the given span. This should
     * be used along with {@link #TRACE_CONTEXT_MESSAGE_ATTR_NAME} as the message attribute name. You can use
     * the {@link #withTraceContext(PublishRequest, Span)} helper method to automate using the correct attribute name
     * and value for including trace context when publishing a SNS message.
     */
    public static MessageAttributeValue traceContextMessageAttributeValueForSpan(Span span) {
        return new MessageAttributeValue()
            .withDataType("String")
            .withStringValue(toTraceContextMessageAttributeValue(span));
    }
    
}
