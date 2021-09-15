package com.sample.routeconfigs.ingress.route.configuration;

import com.sample.routeconfigs.common.EventDataEncoding;
import com.sample.routeconfigs.common.model.pulse.EventData;
import samplecamel.processor.DistributedTraceProcessor;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Defines sub routes for {@link IngressFromPulseRouteBuilder}
 */
@Configuration
@ConfigurationProperties
public class IngressFromPulseSubRoutes extends RouteBuilder {

    public static final String DECODING_ROUTE_ID = "DecodingRoute";
    public static final String DIRECT_DECODING_ROUTE = "direct:" + DECODING_ROUTE_ID;

    public static final String ENCODED_DATA_HANDLING_ROUTE_ID = "EncodedDataHandlingRoute";
    public static final String DIRECT_ENCODED_DATA_HANDLING_ROUTE = "direct:" + ENCODED_DATA_HANDLING_ROUTE_ID;

    @Override
    public void configure() throws Exception {

        from(DIRECT_ENCODED_DATA_HANDLING_ROUTE)
            .id(ENCODED_DATA_HANDLING_ROUTE_ID)
            .description("Stashes the encoded data in the " + EventData.ENCODED_DATA + " header if ingress route's input stashEncodedData = true")
            .bean(DistributedTraceProcessor.class)
            .choice()
                .when(header(IngressFromPulseRouteBuilder.STASH_ENCODED_DATA_FLAG).isEqualTo("true"))
                    .setHeader(EventData.ENCODED_DATA, simple("${body}"))
                .endChoice()
            .end() //end of choice
            .end(); // end of route


        from(DIRECT_DECODING_ROUTE)
            .id(DECODING_ROUTE_ID)
            .description("Handles the decoding processing according to the EventData.ENCODING value")
            .bean(DistributedTraceProcessor.class)
            .choice()
                .when(header(EventData.ENCODING).isEqualTo(EventDataEncoding.GZIP_BASE64.name()))
                    .unmarshal().base64() // decode the payload
                    .log(DECODING_ROUTE_ID + " successfully decoded the payload")
                    .unmarshal().gzip() // decompress the payload to its original format
                    .log(DECODING_ROUTE_ID + " successfully completed unzip of the payload")
                .endChoice() // end of choice
                    .when(header(EventData.ENCODING).isEqualTo(EventDataEncoding.BASE64.name()))
                    .unmarshal().base64() // decode the payload
                    .convertBodyTo(String.class, IngressFromPulseRouteBuilder.UTF_8)
                    .log(DECODING_ROUTE_ID + " successfully decoded the payload:")
                .endChoice()
            .end() // end of choice
            .end(); // end of route

    }
}

