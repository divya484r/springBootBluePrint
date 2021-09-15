package com.sample.routeconfigs.exception;

import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.http.common.HttpOperationFailedException;


/***
 * This predicate returns false if the status code is 409. If corrected retries are required for 409 then we might need to create seperate predicate.
 */
public class HttpExceptionRetryPredicate implements Predicate {

    private static final String STATUS_CODE_400 = "statusCode: 400";
    private static final String STATUS_CODE_403 = "statusCode: 403";
    private static final String STATUS_CODE_409 = "statusCode: 409";


    @Override
    public boolean matches(Exchange exchange) {

        Throwable t = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);

        if (isHystrixRuntimeException(t) && isNonRetriableHttpOperationFailedException(t)) {
            return false;
        }

        if (isHttpOperationFailedException(t) && isHttpStatusCodeNonRetriable(t)) {
            return false;
        }

        return true;
    }

    private boolean isHystrixRuntimeException(Throwable t) {
        return t instanceof HystrixRuntimeException;
    }

    private boolean isHttpOperationFailedException(Throwable t) {
        return t instanceof HttpOperationFailedException;
    }

    private boolean isNonRetriableHttpOperationFailedException(Throwable t) {
        return isCauseHttpOperationFailedException(t) && isCauseHttpStatusCodeNonRetriable(t);
    }

    private boolean isCauseHttpOperationFailedException(Throwable t) {
        return t.getCause() instanceof HttpOperationFailedException;
    }

    private boolean isCauseHttpStatusCodeNonRetriable(Throwable t) {
        return isHttpStatusCodeNonRetriable(t.getCause());
    }

    private boolean isHttpStatusCodeNonRetriable(Throwable t) {
        return t.getMessage().contains(STATUS_CODE_400)
                || t.getMessage().contains(STATUS_CODE_403)
                || t.getMessage().contains(STATUS_CODE_409);
    }
}
