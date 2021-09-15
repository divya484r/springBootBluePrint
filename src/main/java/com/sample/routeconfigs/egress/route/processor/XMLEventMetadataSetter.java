package com.sample.routeconfigs.egress.route.processor;

import com.sample.routeconfigs.common.model.pulse.EventContext;
import com.sample.routeconfigs.common.model.pulse.EventData;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Sets the Pulse payload event context metadata on Camel message headers. This data will be read by
 * {@link PulsePOSTPayloadProcessor} on egress to Pulse.
 *
 * This implementation of {@link EventMetadataSetter} to be used for XML payloads.
 *
 * Note that this implementation presumes that a camel header named {@link EventContext#BUSINESS_KEY_NAME} has been
 * set by the camel-pulse-traffic-routing ingress route or otherwise.
 */
@Component
public class XMLEventMetadataSetter implements EventMetadataSetter {

    public static final String EVENT_DATA_CONTENT_TYPE = "application/xml";

    /**
     * Example of eventContextName: ce_adapter_ship_cancel (i.e., topic name)
     * Example of eventContextType: EP_SHIP_CANCEL
     *
     * Example usage:
     * <code>
     *     from(direct:route)
     *          ...
     *          .bean(XMLEventMetadataSetter.class, String.format(EventMetadataSetter.METHOD_CALL_FORMAT,
     *              "ce_fmg_sc_canonical", "EP_SHIP_CANCEL"))
     *          ...
     *          .end();
     * </code>
     *
     * Note that if the String constructed with METHOD_CALL_FORMAT will be the same for each message being processed
     * by the route, it is more performant to construct it once as a static class-level String.
     *
     * @param exchange Camel Exchange object
     * @param eventContextName {@link EventContext#NAME}
     * @param eventContextType {@link EventContext#TYPE}
     */
    @Override
    public void setMetadata(Exchange exchange, String eventContextName, String eventContextType) {

        Message message = exchange.getIn();

        if (StringUtils.isEmpty((String) message.getHeader(EventContext.BUSINESS_KEY_NAME))) {
            throw new IllegalStateException("The Camel exchange message header " + EventContext.BUSINESS_KEY_NAME
                    + " must be set before using this method.");
        }

        message.setHeader(EventContext.NAME, eventContextName);
        message.setHeader(EventContext.RETENTION_DAYS, EVENT_CONTEXT_RETENTION_DAYS);
        message.setHeader(EventContext.TYPE, eventContextType);
        message.setHeader(EventContext.VERSION, EVENT_CONTEXT_VERSION);
        message.setHeader(EventData.CONTENT_TYPE, EVENT_DATA_CONTENT_TYPE);
        message.setHeader(EventData.ENCODING, EVENT_DATA_ENCODING);

    }

}
