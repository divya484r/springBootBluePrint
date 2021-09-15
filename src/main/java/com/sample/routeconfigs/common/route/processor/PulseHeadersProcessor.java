package com.sample.routeconfigs.common.route.processor;


import com.sample.routeconfigs.common.RouteConstants;

import com.sample.routeconfigs.rest.RESTHeadersSetter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;

/**
 * Signs exchange message headers with JWT authorization.
 * <p>
 * Notice that we use @Component on the class to make the processor
 * automatically discovered by discovered by Spring Boot
 */
@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PulseHeadersProcessor implements Processor {

    @Autowired
    private ApplicationContext context;

    private RESTHeadersSetter restHeadersSetter;

    @Value("${ship.pulse.vipName}")
    private String shipPulseVipName;

    @Override
    public void process(Exchange exchange) throws IOException {

        restHeadersSetter = context.getBean(RESTHeadersSetter.class);
        restHeadersSetter.setHeaders(exchange, (RequestMethod) exchange.getIn().getHeader(RouteConstants.PULSE_HTTP_REQUEST_METHOD),
                shipPulseVipName);
    }
}
