package com.sample.springbootsampleapp.route.configuration;

import com.sample.springbootsampleapp.util.ApplicationConstants;
import com.sample.routeconfigs.ingress.route.configuration.IngressFromPulseRouteBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Adds the route that reads from the queue and gets the order event data from Pulse
 */
@Slf4j
@Component
public class IntakeRoute implements InitializingBean {

    public static final String SHIP_CONFIRM_FMG_TO_DOD_ROUTE = "ShipConfirmRoute";
    public static final String SHIP_STATUS_FMG_TO_DOD_ROUTE = "ShipStatusFRoute";


    @Autowired
    private IngressFromPulseRouteBuilder ingressFromPulseRouteBuilder;

    @Autowired
    private CamelContext camelContext;

    @Value("${sqs.afssapshipconfirm.queue.name}")
    private String shipConfirmQueue;

    @Value("${sqs.afssapshipconfirm.cancel.queue.name}")
    private String shipStausQueue;



    @Override
    public void afterPropertiesSet() throws Exception {

        final RouteBuilder shipConfirmRouteBuilder = ingressFromPulseRouteBuilder.createIngressFromPulseRouteBuilder(
                SHIP_CONFIRM_FMG_TO_DOD_ROUTE, shipConfirmQueue, ApplicationConstants.SHIP_CONFIRM_ROUTER);
        log.info("Inside IntakeRoute - Setting shipConfirmRouteBuilder");

        final RouteBuilder shipCancelUpdateRouteBuilder = ingressFromPulseRouteBuilder.createIngressFromPulseRouteBuilder(
                SHIP_STATUS_FMG_TO_DOD_ROUTE, shipStausQueue, ApplicationConstants.SHIP_CANCEL_ROUTER);
        log.info("Inside IntakeRoute - Setting shipCancelUpdateRouteBuilder");


        camelContext.addRoutes(shipConfirmRouteBuilder);
        camelContext.addRoutes(shipCancelUpdateRouteBuilder);

    }
}
