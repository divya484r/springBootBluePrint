package com.sample.phylon.exception;

import samplephylon.jwt.auth.exception.JWTValidationException;
import wingtips.Span;
import wingtips.Tracer;
import wingtips.aws.general.util.SupplierWithTracing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayDeque;
import java.util.function.Supplier;
import static java.util.Collections.singleton;

public class BaseExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseExceptionHandler.class);
    @ExceptionHandler(JWTValidationException.class)
    @ResponseBody
    public ResponseEntity handleJWTException(HttpServletRequest req, JWTValidationException ex) {
        return withRequestTracingState(
                req,
                () -> {
                    LOGGER.error("JWTValidationException caught by handler", ex);
                    return ResponseEntity.status(ex.getStatusCode()).build();
                }
        ).get();
    }

    /**
     * If your endpoint is an async endpoint then this exception handler's code might run on a different thread and
     * therefore not have access to the tracing state for the request. This method inspects the given request's
     * attributes to find the overall request span associated with the request, and returns a supplier that executes
     * the given original supplier's logic but guarantees the request's tracing state is attached during execution.
     */
    protected <T> Supplier<T> withRequestTracingState(HttpServletRequest req, Supplier<T> orig) {
        try {
            Span requestSpan = (Span) req.getAttribute(Span.class.getName());

            if (requestSpan == null) {
                // Somehow this request did not get a span started for it. Nothing we can do.
                return orig;
            }

            Span currentSpan = Tracer.getInstance().getCurrentSpan();
            if (currentSpan != null && requestSpan.getTraceId().equals(currentSpan.getTraceId())) {
                // This thread already has the correct tracing state attached, so we don't need to do anything.
                return orig;
            } else {
                // This thread does *not* have the request's tracing state attached, so we need to return a
                //      SupplierWithTracing that uses the request's tracing state while it runs.
                return new SupplierWithTracing(orig, new ArrayDeque<>(singleton(requestSpan)), null);
            }
        } catch (Throwable t) {
            LOGGER.error("An unexpected error occurred while trying to attach correct tracing state.", t);
            return orig;
        }
    }
}
