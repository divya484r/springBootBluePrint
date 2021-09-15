package com.sample.routeconfigs.ingress.route.processor;


import com.sample.routeconfigs.common.model.pulse.EventContext;
import com.sample.routeconfigs.common.model.pulse.EventData;
import com.sample.routeconfigs.common.model.pulse.Pulse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Accepts the Event message(JSON) , extracts the payload section(data.value) and sets it in the exchange body.
 * Note the payload will still be in gzip and encoded format.
 * <p>
 * Notice that we use @Component on the class to make the processor
 * automatically discovered by discovered by Spring Boot
 */
@Slf4j
@Component
public class PulsePayloadDataExtractionProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        Pulse pulse = mapper.readValue((String) exchange.getIn().getBody(), Pulse.class);

        if (null != pulse.getData()) {
            String pulseEncodedPayload = pulse.getData().getValue();

            //Get BusinessKeyName and set it in the header
            exchange.getIn().setHeader(EventContext.BUSINESS_KEY_NAME, pulse.getEventContext().getBusinessKeyName());

            // Stashes the business key value in a header
            exchange.getIn().setHeader(EventContext.BUSINESS_KEY_VALUE, pulse.getEventContext().getBusinessKeyValue());

            log.info("Successfully extracted Pulse payload data for businessKeyName ='{}', businessKeyValue ='{}' ",
                    pulse.getEventContext().getBusinessKeyName(), pulse.getEventContext().getBusinessKeyValue());

            //Get Encoding value and set in the header to unmarshal based on this value
            exchange.getIn().setHeader(EventData.ENCODING, pulse.getData().getEncoding());

            //Get FilterMap and set in the header so that it can be accessible for later use
            exchange.getIn().setHeader(EventContext.EVENT_CONTEXT_FILTER_MAP_ENTRY_SETS, pulse.getEventContext().getFilterMap());
            exchange.getIn().setBody(pulseEncodedPayload);
        } else {
            log.warn("Pulse event message has no data section to process for businessKeyName ='{}', businessKeyValue ='{}' ",
                    pulse.getEventContext().getBusinessKeyName(), pulse.getEventContext().getBusinessKeyValue());
        }
    }
}
