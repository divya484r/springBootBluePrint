package com.sample.springbootsampleapp.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.http.common.HttpOperationFailedException;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.stereotype.Component;

/**
 * Supports logging response body of <code>HttpOperationFailedException</code> if CamelExceptionCaught or its root cause
 * is of this type
 * <p>
 * Logs messages of all exception types
 * <p>
 * Where cause is not null, logs message of root cause
 */
@Slf4j
@Component("ExceptionLoggingProcessor")
public class ExceptionLoggingProcessor implements Processor {

    private static final String EXCEPTION_CAUGHT_MESSAGE = "Camel exception caught: {}";
    private static final String RESPONSE_BODY_MESSAGE = " Response body: {}";
    private static final String CAUSE_EXCEPTION_MESSAGE = " Cause: {}";

    @Override
    public void process(Exchange exchange) {
        Exception camelExceptionCaught = (Exception) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        Throwable rootCause = ExceptionUtils.getRootCause(camelExceptionCaught);
        String body = exchange.getIn().getBody().toString();
        HttpOperationFailedException httpOperationFailedException;

        if (camelExceptionCaught instanceof HttpOperationFailedException) {
            httpOperationFailedException = (HttpOperationFailedException) camelExceptionCaught;
            log.error(EXCEPTION_CAUGHT_MESSAGE + RESPONSE_BODY_MESSAGE,
                    httpOperationFailedException, httpOperationFailedException.getResponseBody());
        } else if (rootCause instanceof HttpOperationFailedException) {
            httpOperationFailedException = (HttpOperationFailedException) rootCause;
            log.error(EXCEPTION_CAUGHT_MESSAGE + CAUSE_EXCEPTION_MESSAGE + RESPONSE_BODY_MESSAGE,
                    camelExceptionCaught, httpOperationFailedException, httpOperationFailedException.getResponseBody());
        } else if (rootCause != null) {
            log.error(EXCEPTION_CAUGHT_MESSAGE + CAUSE_EXCEPTION_MESSAGE,
                    camelExceptionCaught, rootCause.toString());
        } else {
            log.error(EXCEPTION_CAUGHT_MESSAGE,
                    camelExceptionCaught.toString());
        }
    }
}
