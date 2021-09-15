package com.sample.routeconfigs.egress.route.processor;

import com.sample.routeconfigs.common.EventDataEncoding;
import com.sample.routeconfigs.common.model.pulse.EventContext;
import org.apache.camel.Exchange;


/**
 * Interface for setting the Pulse payload event context metadata on Camel message headers.
 *
 * This data will be read by {@link PulsePOSTPayloadProcessor}
 * on egress to Pulse.
 *
 * Implementations of this interface to be posted to Pulse through eventmanagement v1.
 */
public interface EventMetadataSetter {

    String EVENT_CONTEXT_VERSION = EventContext.Version.V1_0.value();
    String EVENT_CONTEXT_RETENTION_DAYS = "90";
    String EVENT_DATA_ENCODING = EventDataEncoding.GZIP_BASE64.name();
    String METHOD_CALL_FORMAT = "setMetadata(${exchange}, %s, %s)";

    /**
     * Example of eventContextName: ce_adapter_ship_cancel (i.e., topic name)
     * Example of eventContextType: EP_SHIP_CANCEL
     *
     * Example usage:
     *
     * <code>
     *     from(direct:route)
     *          ...
     *          .bean(EventMetadataSetter.class, String.format(EventMetadataSetter.METHOD_CALL_FORMAT,
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
    void setMetadata(Exchange exchange, String eventContextName, String eventContextType);
}
