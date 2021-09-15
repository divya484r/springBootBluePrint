package com.sample.routeconfigs.exception;

import com.netflix.hystrix.exception.HystrixRuntimeException;

import com.sample.springbootsampleapp.util.DistributedTraceProcessor;
import com.sample.springbootsampleapp.util.ExceptionLoggingProcessor;
import lombok.Getter;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.common.HttpOperationFailedException;
import org.apache.camel.model.OnExceptionDefinition;

/**
 * This library provides an abstract Camel exception handling configuration. The configuration defines three
 * exception handers:
 *
 * - OnHttpOperationFailedExceptionDefinition
 * - OnHystrixRuntimeExceptionDefinition
 * - OnExceptionDefintion
 *
 * The HttpOperationFailedExceptionDefinition and OnHystrixRuntimeExceptionDefinition support retry handling. When the
 * exception is of type HttpOperationFailedException or HystrixRuntimeException and the HTTP status code is other than
 * 400, 403 or 409, Camel will retry the HTTP call up to the maxRedeliveryCount.
 *
 * HTTP status codes 400, 403 and 409, and all other exceptions, will not retry.
 *
 * The HttpExceptionRetryPredicate returns false if the status code is 409. If corrected retries are required for 409 then we might need to create seperate predicate.
 *
 * When the attempts are exhausted, the original message will be sent to the DLQ defined by the class that implements
 * ExceptionHandlerRouteBuilder.
 *
 * The extending class must call this class's configure method and set the dead letter queue. For example:
 *
 * <code>
 *          public class ShipConfirmThinOMRoute extends ExceptionHandlerRouteBuilder {
 *         ...
 *         @Override
 *         public void configure() throws Exception {
 *
 *             super.initialize(maxRedeliveryCount, redeliveryDelayMs, backOffMultiplier);
 *             super.configure();
 *
 *             super.getOnHttpOperationFailedExceptionDefinition()
 *                     .to(dlqUri);
 * </code>
 */
@Getter
public abstract class ExceptionHandlerRouteBuilder extends RouteBuilder {

    private OnExceptionDefinition onHttpOperationFailedExceptionDefinition;
    private OnExceptionDefinition onHystrixRuntimeExceptionDefinition;
    private OnExceptionDefinition onExceptionDefinition;

    private int _maxRedeliveryCount = -1;
    private long _redeliveryDelayMs = -1;
    private int _backoffMultiplier = -1;

    /**
     * This constructor is necessary for directly extending this class since a non-default constructor is defined.
     */
    public ExceptionHandlerRouteBuilder() { }

    /**
     * Use this constructor for anonymous class instantiations.
     *
     * @param maxRedeliveryCount
     * @param redeliveryDelayMs
     * @param backOffMultiplier
     */
    public ExceptionHandlerRouteBuilder(int maxRedeliveryCount, long redeliveryDelayMs, int backOffMultiplier) {
        _maxRedeliveryCount = maxRedeliveryCount;
        _redeliveryDelayMs = redeliveryDelayMs;
        _backoffMultiplier = backOffMultiplier;
    }

    /**
     * When this class is being directly extended, this method must be called from with the extending class's configure
     * method prior to the call to configure. For example,
     *
     *  super.intialize(maxRedeliveryCount, redeliveryDelayMs, backOffMultiplier);
     *  super.configure();
     *
     * @param maxRedeliveryCount
     * @param redeliveryDelayMs
     * @param backOffMultiplier
     * @throws Exception
     */
    protected void initialize(int maxRedeliveryCount, long redeliveryDelayMs, int backOffMultiplier) {
        _maxRedeliveryCount = maxRedeliveryCount;
        _redeliveryDelayMs = redeliveryDelayMs;
        _backoffMultiplier = backOffMultiplier;
    }

    /**
     * When this class is being directly extended, first call this class's initialize method with the required queue properties.
     *
     * @throws Exception
     */
    @Override
    public void configure() throws Exception {

        if (_maxRedeliveryCount < 0 || _redeliveryDelayMs < 0 || _backoffMultiplier < 0) {
            throw new IllegalStateException("Queue properties have not been set in abstract class " + this.getClass().getName() + ". "
                    + "Call initialize(int maxRedeliveryCount, long redeliveryDelayMs, int backOffMultiplier) "
                    + "prior to calling configure().");
        }

        onHttpOperationFailedExceptionDefinition = onException(HttpOperationFailedException.class)
                .onWhen(new HttpExceptionRetryPredicate())
                .bean(DistributedTraceProcessor.class)
                .maximumRedeliveries(_maxRedeliveryCount)
                .redeliveryDelay(_redeliveryDelayMs)
                .backOffMultiplier(_backoffMultiplier)
                .asyncDelayedRedelivery()
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .retriesExhaustedLogLevel(LoggingLevel.ERROR)
                .useOriginalMessage()
                .bean(ExceptionLoggingProcessor.class);
                // The appropriate DLQ URI must be added in the inheriting class

        onHystrixRuntimeExceptionDefinition = onException(HystrixRuntimeException.class)
                .onWhen(new HttpExceptionRetryPredicate())
                .bean(DistributedTraceProcessor.class)
                .maximumRedeliveries(_maxRedeliveryCount)
                .redeliveryDelay(_redeliveryDelayMs)
                .backOffMultiplier(_backoffMultiplier)
                .asyncDelayedRedelivery()
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .retriesExhaustedLogLevel(LoggingLevel.ERROR)
                .useOriginalMessage()
                .bean(ExceptionLoggingProcessor.class);
                // The appropriate DLQ URI must be added in the inheriting class

        onExceptionDefinition = onException(Exception.class)
                .bean(DistributedTraceProcessor.class)
                .useOriginalMessage()
                .bean(ExceptionLoggingProcessor.class);
                // The appropriate DLQ URI must be added in the inheriting class
    }

    /**
     * Configures the exception handlers to route to a DLQ.
     *
     * The URI must contain the aws-sqs protocol and a reference to an AmazonSQS client. For example:
     *
     *  aws-sqs://ship-mp_vom_work_order_to_boxlabel-work-dlq?amazonSQSClient=#amazonSQSClient
     *
     * @param dlqUri
     */
    protected void configureDlq(String dlqUri) {

        onHttpOperationFailedExceptionDefinition
                .to(dlqUri);

        onHystrixRuntimeExceptionDefinition
                .to(dlqUri);

        onExceptionDefinition
                .to(dlqUri);
    }
}
