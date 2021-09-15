package com.sample.routeconfigs.egress.route.configuration;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Provides a route that performs a POST call to Pulse.
 *
 * Does not provide routing to a subsequent route.
 *
 * Camel error handling retries are based on these application properties
 *
 *      camel.maxRedeliveryCount (default 5)
 *      camel.redeliveryDelayMs (default 2000)
 *      camel.backOffMultiplier (default 2)
 */
@Slf4j
@Component
public class PostToPulseEndRoute implements InitializingBean {

    public static final String POST_TO_PULSE_END_ROUTE_ID = "PostToPulseEndRoute";
    public static final String DIRECT_POST_TO_PULSE_END_ROUTE = "direct:" + POST_TO_PULSE_END_ROUTE_ID;

    @Autowired
    private EgressToPulseRouteBuilder egressToPulseRouteBuilder;

    @Autowired
    private CamelContext camelContext;

    @Override
    public void afterPropertiesSet() throws Exception {

        final RouteBuilder consumerOrderRequestRouteBuilder = egressToPulseRouteBuilder.createEgressToPulseRouteBuilder(DIRECT_POST_TO_PULSE_END_ROUTE, POST_TO_PULSE_END_ROUTE_ID);
        camelContext.addRoutes(consumerOrderRequestRouteBuilder);

    }
}
