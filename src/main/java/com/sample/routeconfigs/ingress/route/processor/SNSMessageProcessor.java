package com.sample.routeconfigs.ingress.route.processor;


import com.sample.routeconfigs.common.exception.PulseTrafficRoutingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Stashes event id from the incoming SNS notification to the EventId Camel exchange message header.
 * <p>
 * Stashes MessageAttributes of the notification as a {@Link com.fasterxml.jackson.databind.JsonNode} to
 * the SNSMessageAttributes Camel exchange property.
 * <p>≤
 * Notice that we use @Component on the class to make the processor
 * automatically discovered by discovered by Spring Boot
 */
@Slf4j
@Component
public class SNSMessageProcessor implements Processor {

    public static final String PULSE_EVENT_ID = "PulseEventId";
    public static final String PULSE_SNS_MESSAGE_ATTRIBUTES = "PulseSNSMessageAttributes";
    public static final String PULSE_SNS_MESSAGE = "PulseSNSMessage";

    @Override
    public void process(Exchange exchange) throws IOException, PulseTrafficRoutingException {

        ObjectMapper mapper = new ObjectMapper();

        JsonNode snsMessage = mapper.readValue((String) exchange.getIn().getBody(), JsonNode.class);

        String snsMessageId = getMessageId(snsMessage);
        JsonNode messageAttributes = snsMessage.get("MessageAttributes");
        String eventId = getEventId(snsMessageId, messageAttributes);

        log.info("Successfully read SNS message for eventId ='{}' ", eventId);

        exchange.setProperty(PULSE_EVENT_ID, eventId);
        exchange.setProperty(PULSE_SNS_MESSAGE_ATTRIBUTES, messageAttributes);
        exchange.setProperty(PULSE_SNS_MESSAGE, snsMessage);
    }

    private String getMessageId(JsonNode snsMessage) {
        JsonNode messageIdNode = snsMessage.get("MessageId");
        if (messageIdNode != null) {
            return messageIdNode.textValue();
        }
        log.warn("No MessageId was found in incoming Camel exchange body.");
        return null;
    }

    private String getEventId(String snsMessageId, JsonNode messageAttributes) throws PulseTrafficRoutingException {
        JsonNode idAttribute;
        JsonNode type;
        JsonNode eventId;
        String eventIdTextValue;

        if (messageAttributes != null) {
            idAttribute = messageAttributes.get("id");
        } else {
            throw new PulseTrafficRoutingException(String.format("No MessageAttributes were found in Camel exchange body. "
                    + "SNS MessageId: = %s.", snsMessageId));
        }
        if (idAttribute != null) {
            type = idAttribute.get("Type");
        } else {
            throw new PulseTrafficRoutingException(String.format("No event id node was found in the message attributes "
                    + "in the Camel exchange body. SNS MessageId: %s.", snsMessageId));
        }
        if (type != null && type.textValue().equals("String")) {
            eventId = idAttribute.get("Value");
        } else {
            throw new PulseTrafficRoutingException(String.format("No Type node of type String was found in the event "
                    + "id node in the Camel exchange body. SNS MessageId: %s", snsMessageId));
        }
        if (eventId != null) {
            eventIdTextValue = eventId.textValue();
        } else {
            throw new PulseTrafficRoutingException(String.format("The event id Value node was missing for snsMessageId = %s.",
                    snsMessageId));
        }
        if (!StringUtils.isEmpty(eventIdTextValue)) {
            return eventIdTextValue;
        } else {
            throw new PulseTrafficRoutingException(String.format("The event id text value was empty for snsMessageId = %s.",
                    snsMessageId));
        }
    }
}
