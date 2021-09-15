package wingtips.aws.sqs;

import wingtips.aws.general.util.WingtipsAwsGeneralUtil;
import wingtips.aws.sns.WingtipsAwsSnsUtil;
import com.amazonaws.services.sqs.model.*;
import wingtips.Span;
import wingtips.Tracer;

import java.util.*;

import static wingtips.aws.general.util.WingtipsAwsGeneralUtil.fromTraceContextMessageAttributeValue;
import static wingtips.aws.general.util.WingtipsAwsGeneralUtil.toTraceContextMessageAttributeValue;

/**
 * Contains SQS-specific static helper methods for working with Wingtips in an environment that utilizes the AWS
 * SQS SDK.
 *
 * <p><b>DEPENDENCY WARNING: Using this class requires the {@code com.amazonaws:wingtips.aws-java-sdk-sqs} AWS SDK dependency</b>
 * (or the full AWS SDK dependency). If you try to use this class without pulling in that dependency you will get
 * class-not-found or similar runtime errors. You must pull in that dependency explicitly in your project since this
 * module does not publish it as a transitive dependency.
 *
 * <p><b>USAGE:</b> Typical usage of this class might look like:
 * 
 * <ul>
 *     <li>
 *          <b>Add the Wingtips trace context to a SQS publish-message request:</b>
 *          <pre>
 *              import static samplewingtips.wingtips.aws.sqs.WingtipsAwsSqsUtil.withTraceContext;
 *              // ...
 *              Span traceContextSpan = Tracer.getInstance().getCurrentSpan();
 *              sqsClient.sendMessage(withTraceContext(someSendMessageRequest, traceContextSpan));
 *          </pre>
 *
 *          (NOTE: For publishing messages via SNS->SQS instead of directly to SQS, see {@link
 *          WingtipsAwsSnsUtil WingtipsAwsSnsUtil}, and in particular {@link
 *          WingtipsAwsSnsUtil#withTraceContext(com.amazonaws.services.sns.model.PublishRequest, Span)
 *          WingtipsAwsSnsUtil.withTraceContext(snsPublishRequest, traceContextSpan)}.)
 *
 *          <p>(ALSO NOTE: There are helpers for working with batch messages as well - see {@link
 *          #withTraceContext(SendMessageBatchRequestEntry, Span)} and similar methods.)
 *     </li>
 *     <li>
 *          <b>Retrieve SQS message with the trace context message attribute included:</b>
 *          <pre>
 *              import static samplewingtips.wingtips.aws.sqs.WingtipsAwsSqsUtil.withTraceContextMessageAttributeName;
 *              // ...
 *              ReceiveMessageResult receiveMessageResult = sqsClient.receiveMessage(
 *                  withTraceContextMessageAttributeName(someReceiveMessageRequest)
 *              );
 *          </pre>
 *
 *          (NOTE: This is technically not necessary if your SQS messages are coming from SNS and the SNS subscription
 *          is set to normal delivery mode (the default). It is absolutely necessary if you are publishing directly
 *          to SQS, or if you're doing SNS->SQS and the SNS subscription is set to
 *          <a href="http://docs.aws.amazon.com/sns/latest/dg/large-payload-raw-message.html">raw delivery mode</a>.
 *          It never hurts to do this, even if it's not technically necessary, so when in doubt do this when retrieving
 *          SQS messages.)
 *     </li>
 *     <li>
 *          <b>Extract the trace context from SQS message:</b>
 *          <pre>
 *              import static samplewingtips.wingtips.aws.sqs.WingtipsAwsSqsUtil.extractTraceContext;
 *              // ...
 *              Span sqsMessageTraceContext = extractTraceContext(sqsMessage);
 *          </pre>
 *     </li>
 *     <li>
 *          <b>Surround SQS message processing with a child span based on the SQS message's trace context:</b>
 *          <pre>
 *              import static samplewingtips.wingtips.aws.sqs.WingtipsAwsSqsUtil.continueTraceFromParentSpanOnThisThread;
 *              import static samplewingtips.wingtips.aws.sqs.WingtipsAwsSqsUtil.extractTraceContext;
 *              // ...
 *              Span traceContext = extractTraceContext(sqsMessage);
 *              someExecutorService.execute(() -> {
 *                  Span sqsMessageProcessingChildSpan = continueTraceFromParentSpanOnThisThread(
 *                      traceContext, "helpfulNameForSqsMessageProcessing", userId // userId is optional
 *                  );
 *
 *                  try {
 *                      // Process the SQS message normally.
 *                      // ...
 *                  }
 *                  finally {
 *                      Tracer.getInstance().completeRequestSpan();
 *                  }
 *              });
 *          </pre>
 *
 *          (NOTE: The only difference between the above code and normal SQS message processing is the trace context
 *          extraction using {@code extractTraceContext(sqsMessage)}, the call to {@code
 *          continueTraceFromParentSpanOnThisThread(...)} to create a child span from the trace context and attach it
 *          to the processing thread, and the try/finally block to guarantee that the child span is completed properly
 *          when the SQS message processing is done. In the case where the SQS message does not contain a trace context
 *          the above code will cause a new trace/root span to be created for the SQS message processing.)
 *
 *          <p>(ALSO NOTE: You do not need to use an {@code ExecutorService} to process the message as shown in the
 *          example above. You could use a {@code CompletableFuture}, raw {@code Thread}, RxJava, any other mechanism
 *          to process on a different thread, or even inline in the same thread that received the SQS message. However
 *          you are currently processing your SQS messages is fine, you just need to extract the trace context for the
 *          message, continue it when processing begins, and surround the processing with the try/finally block above.)
 *     </li>
 * </ul>
 *
 * @author Nic Munroe
 */
public class WingtipsAwsSqsUtil {

    /**
     * The message attribute name when passing trace context along as a SQS message attribute. See {@link
     * #traceContextMessageAttributeValueForSpan(Span)} for a helper method that generates a trace context message
     * attribute value from a {@link Span} for use when publishing SQS messages, {@link #extractTraceContext(Message)}
     * for a helper method that deserializes and extracts a trace context {@link Span} from a SQS message, and the
     * various {@code withTraceContext(...)} methods in this class for helpers that utilize this attribute name
     * to set trace context message attributes on SQS messages.
     */
    public static final String TRACE_CONTEXT_MESSAGE_ATTR_NAME = WingtipsAwsGeneralUtil.TRACE_CONTEXT_MESSAGE_ATTR_NAME;

    /**
     * Extracts the {@link #TRACE_CONTEXT_MESSAGE_ATTR_NAME} message attribute from the given SQS message and turns it
     * into a Wingtips {@link Span}. The returned span should be used as the parent span for continuing a Wingtips
     * trace by calling {@link #continueTraceFromParentSpanOnThisThread(Span, String, String)} and passing it in as
     * the parentSpan argument (NOTE - refer to that method's javadocs for proper usage info and warnings). If the
     * trace context could not be extracted from the given message then null will be returned.
     *
     * <p>This method uses {@link #extractMessageAttribute(Message, String)} to retrieve the trace context message attribute
     * value, so it should work for SNS->SQS messages in both SNS raw message delivery mode and SNS normal message
     * delivery mode. It should also work for direct-to-SQS published messages.
     *
     * <p><b>IMPORTANT NOTE:</b> When using SNS raw message delivery mode or direct-to-SQS message publishing you
     * must explicitly tell the SQS client that you want it to include whatever message attributes you're interested in.
     * i.e. For the trace context message attribute to show up in the given message object you must use a {@link
     * ReceiveMessageRequest} and tell it you want the {@link #TRACE_CONTEXT_MESSAGE_ATTR_NAME} attribute via one of
     * the {@code ReceiveMessageRequest.withMessageAttributeNames(...)} methods. e.g.:
     *
     * <pre>
     *      ReceiveMessageResult receiveMessageResult = sqsClient.receiveMessage(
     *          new ReceiveMessageRequest(sqsQueueUrl)
     *              .withMessageAttributeNames(TRACE_CONTEXT_MESSAGE_ATTR_NAME)
     *      );
     * </pre>
     *
     * It does not hurt to specify that you want this attribute even if it won't do anything (i.e. you're using
     * SNS->SQS in normal delivery mode), so if you're not sure whether you need it go ahead and use
     * {@link ReceiveMessageRequest}s and request the trace context message attribute as shown in the code example
     * above - again it won't hurt anything if it ends up not being necessary.
     *
     * <p>You can use {@link #withTraceContextMessageAttributeName(ReceiveMessageRequest)} as a shortcut when requesting
     * the Wingtips trace context message attribute.
     *
     * @param message The SQS message to extract trace context from.
     * @return The {@link Span} represented by the Wingtips trace context message attribute ({@link
     * #TRACE_CONTEXT_MESSAGE_ATTR_NAME}) contained in the given SQS message
     */
    public static wingtips.Span extractTraceContext(Message message) {
        return fromTraceContextMessageAttributeValue(extractMessageAttribute(message, TRACE_CONTEXT_MESSAGE_ATTR_NAME));
    }

    /**
     * Adds the Wingtips trace context message attribute name ({@link #TRACE_CONTEXT_MESSAGE_ATTR_NAME}) to the given
     * original request's {@link ReceiveMessageRequest#getMessageAttributeNames()}. This is necessary to receive
     * the Wingtips trace context message attribute when working with messages published direct-to-SQS or
     * SNS->SQS messages using the SNS raw delivery mode. SNS->SQS messages using the SNS normal delivery mode do
     * not need this since all message attributes end up embedded in the message body, but using this method to
     * add the trace context message attribute name will not cause any problems even if it's unnecessary.
     *
     * <p>USAGE EXAMPLE:
     *
     * <pre>
     *      ReceiveMessageResult receiveMessageResult = sqsClient.receiveMessage(
     *          withTraceContextMessageAttributeName(
     *              new ReceiveMessageRequest(sqsQueueUrl)
     *              .withMessageAttributeNames("someOtherAttr")
     *          )
     *      );
     * </pre>
     *
     * Note that this doesn't clobber existing message attribute names - in the example above both the Wingtips
     * trace context message attribute name *and* "someOtherAttr" would end up in the request's message attribute
     * names.
     *
     * @param orig The original {@link ReceiveMessageRequest} to add the Wingtips trace context message attribute to.
     * @return The given original {@link ReceiveMessageRequest}, but with the Wingtips trace context message attribute
     * name added.
     */
    public static ReceiveMessageRequest withTraceContextMessageAttributeName(ReceiveMessageRequest orig) {
        List<String> messageAttributeNames = new ArrayList<>();
        // Add any existing message attribute names.
        messageAttributeNames.addAll(orig.getMessageAttributeNames());
        // Add the trace context message attribute name.
        messageAttributeNames.add(TRACE_CONTEXT_MESSAGE_ATTR_NAME);

        return orig.withMessageAttributeNames(messageAttributeNames);
    }

    /**
     * Continues the trace represented by the given parentSpan by creating a new child span from the given parent
     * and attaching it to the current thread as the {@link Tracer#getCurrentSpan()}. <b>NOTE: You must call {@link
     * Tracer#completeRequestSpan()} when you're done doing the work for this child span (i.e. when you're done
     * processing the SQS message that contained parentSpan as its trace context)!</b> Otherwise you will (1) fail
     * to complete the child span leading to incorrect tracing data in your logs, and (2) leave tracing info on this
     * thread which could pollute other future requests that are processed on this thread.
     *
     * <p>WARNING: This method is built for SQS message processing and assumes there is no current trace
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
        return WingtipsAwsGeneralUtil.continueTraceFromParentSpanOnThisThread(
            parentSpan, newChildSpanName, optionalUserId
        );
    }

    /**
     * @param span The span to create the trace context message attribute value for.
     * @return A SQS {@link MessageAttributeValue} containing the trace context value for the given span. This should
     * be used along with {@link #TRACE_CONTEXT_MESSAGE_ATTR_NAME} as the message attribute name. You can use
     * the {@link #withTraceContext(SendMessageRequest, Span)} and {@link #withTraceContext(Map, Span)} helper methods
     * to automate using the correct attribute name and value for including trace context when publishing a SQS message.
     */
    public static MessageAttributeValue traceContextMessageAttributeValueForSpan(Span span) {
        return new MessageAttributeValue()
            .withDataType("String")
            .withStringValue(toTraceContextMessageAttributeValue(span));
    }

    /**
     * Adds the trace context message attribute to the given {@link SendMessageRequest} when publishing a SQS message.
     * The trace context can be extracted on the other end when receiving the message by calling {@link
     * #extractTraceContext(Message)}.
     *
     * <p>NOTE: You must pass in the {@link Span} that represents the trace context for the message. If you're not
     * sure what this should be, then it's likely the "current" Wingtips span retrieved via {@link
     * Tracer#getCurrentSpan()}.
     *
     * <p>Example use case for this method:
     * <pre>
     *      Span traceContextSpan = Tracer.getInstance().getCurrentSpan();
     *      sqsClient.sendMessage(withTraceContext(someSendMessageRequest, traceContextSpan));
     * </pre>
     *
     * @param original The original request.
     * @param traceContextSpan The Wingtips {@link Span} representing the trace context for the SQS message.
     * @return The given original request, but with a Wingtips trace context message attribute added.
     */
    public static SendMessageRequest withTraceContext(SendMessageRequest original, Span traceContextSpan) {
        return original.addMessageAttributesEntry(TRACE_CONTEXT_MESSAGE_ATTR_NAME,
                                                  traceContextMessageAttributeValueForSpan(traceContextSpan));
    }

    /**
     * Adds the trace context message attribute to the given collection of {@link SendMessageBatchRequestEntry} for use
     * when creating a SQS batch message request. The trace context can be extracted on the other end when receiving the
     * message by calling {@link #extractTraceContext(Message)}. This method is for adding the same trace context
     * attribute to an entire collection of {@link SendMessageBatchRequestEntry} messages. If you only want to add
     * trace context to a single batch message entry you can call {@link
     * #withTraceContext(SendMessageBatchRequestEntry, Span)} instead.
     *
     * <p>NOTE: You must pass in the {@link Span} that represents the trace context for the batch message entries. If
     * you're not sure what this should be, then it's likely the "current" Wingtips span retrieved via {@link
     * Tracer#getCurrentSpan()}.
     *
     * <p>Example use case for this method:
     * <pre>
     *      Span traceContextSpan = Tracer.getInstance().getCurrentSpan();
     *      SendMessageBatchRequest batchRequest = new SendMessageBatchRequest(sqsQueueUrl).withEntries(
     *          withTraceContext(
     *              someCollectionOfBatchMessageEntries,
     *              traceContextSpan
     *          )
     *      );
     *
     *      sqsClient.sendMessageBatch(batchRequest);
     * </pre>
     *
     * @param originalCollection The original collection of batch message entries.
     * @param traceContextSpan The Wingtips {@link Span} representing the trace context for all the given SQS batch
     * message entries.
     * @return The given original collection of batch message entries, but with a Wingtips trace context message
     * attribute added.
     */
    public static <C extends Collection<SendMessageBatchRequestEntry>> C withTraceContext(
        C originalCollection, Span traceContextSpan
    ) {
        MessageAttributeValue traceContextMessageAttrValue = traceContextMessageAttributeValueForSpan(traceContextSpan);
        for (SendMessageBatchRequestEntry message : originalCollection) {
            message.addMessageAttributesEntry(TRACE_CONTEXT_MESSAGE_ATTR_NAME, traceContextMessageAttrValue);
        }
        return originalCollection;
    }

    /**
     * Adds the trace context message attribute to the given vararg array of {@link SendMessageBatchRequestEntry} for use
     * when creating a SQS batch message request. The trace context can be extracted on the other end when receiving the
     * message by calling {@link #extractTraceContext(Message)}. This method is for adding the same trace context
     * attribute to an entire vararg array of {@link SendMessageBatchRequestEntry} messages. If you only want to add
     * trace context to a single batch message entry you can call {@link
     * #withTraceContext(SendMessageBatchRequestEntry, Span)} instead.
     *
     * <p>NOTE: You must pass in the {@link Span} that represents the trace context for the batch message entries. If
     * you're not sure what this should be, then it's likely the "current" Wingtips span retrieved via {@link
     * Tracer#getCurrentSpan()}.
     *
     * <p>Example use case for this method:
     * <pre>
     *      Span traceContextSpan = Tracer.getInstance().getCurrentSpan();
     *      SendMessageBatchRequest batchRequest = new SendMessageBatchRequest(sqsQueueUrl).withEntries(
     *          withTraceContext(
     *              traceContextSpan,
     *              batchMessageEntry1,
     *              batchMessageEntry2
     *          )
     *      );
     *
     *      sqsClient.sendMessageBatch(batchRequest);
     * </pre>
     *
     * @param originalEntries The original vararg array of batch message entries.
     * @param traceContextSpan The Wingtips {@link Span} representing the trace context for all the given SQS batch
     * message entries.
     * @return The given original vararg array of batch message entries, but with a Wingtips trace context message
     * attribute added.
     */
    public static SendMessageBatchRequestEntry[] withTraceContext(
        Span traceContextSpan, SendMessageBatchRequestEntry... originalEntries
    ) {
        if (originalEntries == null) {
            return null;
        }

        MessageAttributeValue traceContextMessageAttrValue = traceContextMessageAttributeValueForSpan(traceContextSpan);
        for (SendMessageBatchRequestEntry message : originalEntries) {
            message.addMessageAttributesEntry(TRACE_CONTEXT_MESSAGE_ATTR_NAME, traceContextMessageAttrValue);
        }
        return originalEntries;
    }

    /**
     * Adds the trace context message attribute to the given {@link SendMessageBatchRequestEntry} for use when
     * creating a SQS batch message request. The trace context can be extracted on the other end when receiving the
     * message by calling {@link #extractTraceContext(Message)}. This method is for adding trace context attribute
     * to a single {@link SendMessageBatchRequestEntry}. If you already have a collection of the batch messages
     * you can call {@link #withTraceContext(Collection, Span)} instead.
     *
     * <p>NOTE: You must pass in the {@link Span} that represents the trace context for the batch message entry. If
     * you're not sure what this should be, then it's likely the "current" Wingtips span retrieved via {@link
     * Tracer#getCurrentSpan()}.
     *
     * <p>Example use case for this method:
     * <pre>
     *      Span traceContextSpan = Tracer.getInstance().getCurrentSpan();
     *      SendMessageBatchRequest batchRequest = new SendMessageBatchRequest(sqsQueueUrl).withEntries(
     *          withTraceContext(new SendMessageBatchRequestEntry(fooMessageId, fooBody), traceContextSpan),
     *          withTraceContext(new SendMessageBatchRequestEntry(barMessageId, barBody), traceContextSpan)
     *      );
     *
     *      sqsClient.sendMessageBatch(batchRequest);
     * </pre>
     *
     * @param original The original batch message entry.
     * @param traceContextSpan The Wingtips {@link Span} representing the trace context for the SQS batch message entry.
     * @return The given original batch message entry, but with a Wingtips trace context message attribute added.
     */
    public static SendMessageBatchRequestEntry withTraceContext(SendMessageBatchRequestEntry original,
                                                                Span traceContextSpan) {
        return original.addMessageAttributesEntry(TRACE_CONTEXT_MESSAGE_ATTR_NAME,
                                                  traceContextMessageAttributeValueForSpan(traceContextSpan));
    }

    /**
     * Adds the trace context message attribute name and value to the given map, used when publishing a SQS message
     * with multiple attributes where you already have an attribute map that you want to use.
     *
     * <p>NOTE: The given map must be mutable as {@link Map#put(Object, Object)} will be called to add the trace
     * context attribute.
     *
     * @param origAttrs The original map of message attributes.
     * @param traceContextSpan The Wingtips {@link Span} representing the trace context for the SQS message.
     * @return The given original map of SQS message attributes, but with the Wingtips tracing context message
     * attribute added.
     */
    public static Map<String, MessageAttributeValue> withTraceContext(Map<String, MessageAttributeValue> origAttrs,
                                                                      Span traceContextSpan) {
        if (origAttrs == null) {
            origAttrs = new HashMap<>();
        }
        
        origAttrs.put(TRACE_CONTEXT_MESSAGE_ATTR_NAME, traceContextMessageAttributeValueForSpan(traceContextSpan));
        return origAttrs;
    }

    /**
     * Returns the message-attribute value from the given message with the given message-attribute-name, or null if no
     * such message attribute could be extracted. This method attempts to support direct-to-SQS publishing and both
     * of the SNS->SQS delivery modes (raw delivery mode and normal delivery mode). In direct-to-SQS publishing or
     * SNS raw delivery mode the message attribute will show up in {@link Message#getMessageAttributes()} if and only
     * if the request specified the attribute name, e.g. by adding it via {@link
     * ReceiveMessageRequest#withAttributeNames(String...)}. In SNS normal delivery mode the message attributes show up
     * in the {@link Message#getBody()} of the SNS->SQS message in a section that looks like:
     *
     * <pre>
     *      "MessageAttributes" : {
     *          "attrName" : {"Type":"String","Value":"attrValue"},
     *          ... other attributes
     *      }
     * </pre>
     *
     * This method first attempts to retrieve the desired message attribute via {@link Message#getMessageAttributes()}.
     * If that fails, then this method falls back to string hacking the attribute value out of the {@link
     * Message#getBody()}. If both of those attempts fail then this method returns null to indicate the desired
     * message attribute could not be found.
     *
     * @param message The message to extract the desired message attribute from.
     * @param attrName The name of the message attribute to extract.
     * @return The message-attribute value from the given message with the given message-attribute-name, or null if no
     * such message attribute could be extracted.
     */
    public static String extractMessageAttribute(Message message, String attrName) {
        // Try to get it as an official message attribute (generally only possible when SNS publishes to SQS in
        //      "raw message delivery message" mode, or if a message is published directly to SQS instead of routing
        //      through SNS.
        MessageAttributeValue attr = message.getMessageAttributes().get(attrName);
        if (attr != null) {
            return attr.getStringValue();
        }

        // It wasn't in the message as an official message attribute. It might still be found in the message body
        //      though - this occurs when the message is routed through SNS->SQS in "normal" delivery mode instead
        //      of raw delivery mode. Check the body.
        String body = message.getBody();
        if (body == null) {
            return null;
        }

        // In this case we're looking for the desired message attribute in the SNS JSON message envelope format.
        //      String hack it for maximum compatibility and speed with minimum dependencies.

        // Find the `"MessageAttributes" : {` string.
        int indexOfMessageAttributes = body.indexOf("\"MessageAttributes\" : {");
        if (indexOfMessageAttributes < 0) {
            return null;
        }

        // Find the `"attrName" : {` string following the start of MessageAttributes.
        int indexOfAttrName = body.indexOf("\"" + attrName + "\" : {", indexOfMessageAttributes);
        if (indexOfAttrName < 0) {
            return null;
        }

        // Find the `"Value":"` string following the start of the attribute.
        String valueStartText = "\"Value\":\"";
        int indexOfAttrValueStart = body.indexOf(valueStartText, indexOfAttrName);
        if (indexOfAttrValueStart < 0) {
            return null;
        }

        // We now know where the attr value starts. All we need now is the end of the attr value so search for
        //      the first `"}` following the attr value start.
        int indexOfAttrValueEnd = body.indexOf("\"}", indexOfAttrValueStart);
        if (indexOfAttrValueEnd < 0) {
            return null;
        }

        // We know where the attr value starts and where it ends, so return the substring that
        //      represents just the attr value.
        return body.substring(indexOfAttrValueStart + valueStartText.length(), indexOfAttrValueEnd);
    }

}
