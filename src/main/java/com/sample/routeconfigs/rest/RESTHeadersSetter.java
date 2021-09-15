package com.sample.routeconfigs.rest;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.cloud.ServiceCallConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;

/**
 * Places security headers on a Camel exchange message in preparation for an HTTP call executed by Camel.
 *
 * It uses a REST security interceptor to sign the headers with JWT authorization.
 *
 * The headers populated by the component are:
 *
 * - Authorization - provided by the REST security interceptor at runtime
 * - X-sample-Authorization - is used instead of Authorization in case of "jwt.use.alternate.jwt.header=true"
 * - X-sample-AppId - provided by the REST security interceptor at runtime, per the application "info.app.group.name" property
 * - instanceId - provided by the REST security interceptor at runtime
 * - CamelHttpMethod - input argument to the `RESTHeadersSetter.setHeaders` method
 * - Content-Type - hard coded to `application/json; charset=utf-8`
 * - Accept - hard coded to "*&#47;*'
 * - CamelServiceCallServiceName -vipName input argument to the `RESTHeadersSetter.setHeaders` method
 */
// Accept - hard coded to "*/*"
@Slf4j
@Component
@ConfigurationProperties
@ComponentScan("com.sample.routeconfigs.rest")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RESTHeadersSetter {

    private static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
    private static final String ACCEPT_VALUE = "*/*";

    @Value("${jwt.use.alternate.jwt.header:false}")
    private boolean useAlternativeJWTHeader;

    @Autowired
    private ApplicationContext context;

    private RESTSecurityInterceptor restSecurityInterceptor;

    public void setHeaders(Exchange exchange, RequestMethod httpMethod, String vipName) throws IOException {

        restSecurityInterceptor = context.getBean(RESTSecurityInterceptor.class);

        Message message = exchange.getIn();
        HttpHeaders headers = new HttpHeaders();

        restSecurityInterceptor.sign(headers);

        message.setHeader(useAlternativeJWTHeader ?
                HTTPJwtHeaders.ALT_AUTHORIZATION : HttpHeaders.AUTHORIZATION, headers.get(HttpHeaders.AUTHORIZATION));
        message.setHeader(HTTPJwtHeaders.ALT_APP_ID, headers.get(HTTPJwtHeaders.ALT_APP_ID));
        message.setHeader(HTTPJwtHeaders.INSTANCE_ID, headers.get(HTTPJwtHeaders.INSTANCE_ID));

        message.setHeader(Exchange.HTTP_METHOD, httpMethod.name());
        message.setHeader(HttpHeaders.CONTENT_TYPE, JSON_CONTENT_TYPE);
        message.setHeader(HttpHeaders.ACCEPT, ACCEPT_VALUE);
        message.setHeader(ServiceCallConstants.SERVICE_NAME, vipName);
    }
}
