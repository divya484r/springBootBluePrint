package com.sample.routeconfigs.ingress.route.configuration;

import com.sample.routeconfigs.common.RouteConstants;
import com.sample.routeconfigs.common.model.pulse.EventData;
import com.sample.routeconfigs.common.route.OutgoingRESTCallRouteConfiguration;
import com.sample.routeconfigs.common.route.RouteUtil;
import com.sample.routeconfigs.common.route.processor.PulseHeadersProcessor;
import com.sample.routeconfigs.exception.ExceptionHandlerRouteBuilder;
import com.sample.routeconfigs.ingress.route.processor.PulsePayloadDataExtractionProcessor;
import com.sample.routeconfigs.ingress.route.processor.SNSMessageProcessor;

import com.sample.springbootsampleapp.util.DistributedTraceProcessor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * This class is a generic Camel route for ingress of Pulse event data. The route reads an event notification
 * from a queue, gets the event from Pulse, and decodes the event data for processing by downstream routes. The encoded
 * event data can optionally be stashed in a Camel exchange message header for downstream access.
 *
 * To use this class, in the consuming project create a Sping Boot component that implements
 * {@link org.springframework.beans.factory.InitializingBean}. The InitializingBean implementation should do the
 * following (see example below):
 *
 *     1. Inject the `IngressFromPulseRouteBuilder` from this library
 *     2. Call `IngressFromPulseRouteBuilder.createIngressFromPulseRouteBuilder`, with the following arguments:
 *
 *         1. `routeId` - this must be unique among the routes in the consuming app
 *         2. `queueName`
 *         3. `dlqName`
 *         4. `consumerUri` - this is the String formatted endpoint to which the route will send the pulse event data, as
 *         `.to(consumerUri).` The endpoint indicated by `consumerUri` can be either another route (e.g., "direct:anotherRouteId"),
 *         a queue endpoint (e.g., "aws-sqs://ship-queue-url..."), or any other String formatted endpoint supported by Camel.
 *
 *      3. Add the RouteBuilder instance returned by `IngressFromPulseRouteBuilder.createIngressFromPulseRouteBuilder` to the
 *      `CamelContext`.
 *
 * Example Implementation
 * <code>
 *     import com.sample.ingress.route.configuration.IngressFromPulseRouteBuilder;
 * import lombok.extern.slf4j.Slf4j;
 * import org.apache.camel.CamelContext;
 * import org.apache.camel.builder.RouteBuilder;
 * import org.springframework.beans.factory.InitializingBean;
 * import org.springframework.beans.factory.annotation.Autowired;
 * import org.springframework.beans.factory.annotation.Value;
 * import org.springframework.stereotype.Component;
 *
 * @Slf4j
 * @Component
 * public class IntakeRouteConfiguration implements InitializingBean {
 *
 *     public static final String sampleID_SHIP_CREATE_ROUTE = "sampleIDShipCreateRoute";
 *     public static final String sampleID_SHIP_STATUS_ROUTE = "sampleIDShipStatusRoute";
 *     public static final String sampleID_SHIP_CONFIRM_ROUTE = "sampleIDShipConfirmRoute";
 *
 *     @Autowired
 *     private IngressFromPulseRouteBuilder ingressFromPulseRouteBuilder;
 *
 *     @Autowired
 *     private CamelContext camelContext;
 *
 *     @Value("${sqs.frp.sampleid-shipcreate.work.queue.name}")
 *     private String sampleIdShipCreateQueueName;
 *
 *     @Value("${sqs.frp.sampleid-shipstatus.work.queue.name}")
 *     private String sampleIdShipStatusQueueName;
 *
 *     @Value("${sqs.frp.sampleid-shipconfirm.work.queue.name}")
 *     private String sampleIdShipConfirmQueueName;
 *
 *     @Value("${sqs.frp.sampleid-shipconfirm.work.dlqueue.name}")
 *     private String sampleIdShipConfirmDlqName;
 *
 *     @Override
 *     public void afterPropertiesSet() throws Exception {
 *
 *         final RouteBuilder sampleIdShipCreateRouteBuilder = ingressFromPulseRouteBuilder.createIngressFromPulseRouteBuilder(
 *                 sampleID_SHIP_CREATE_ROUTE, sampleIdShipCreateQueueName, "mock:TODO");
 *
 *         final RouteBuilder sampleIdShipStatusRouteBuilder = ingressFromPulseRouteBuilder.createIngressFromPulseRouteBuilder(
 *                 sampleID_SHIP_STATUS_ROUTE, sampleIdShipStatusQueueName, "mock:TODO");
 *
 *         final RouteBuilder sampleIdShipConfirmRouteBuilder = ingressFromPulseRouteBuilder.createIngressFromPulseRouteBuilder(
 *                 sampleID_SHIP_CONFIRM_ROUTE, sampleIdShipConfirmQueueName, sampleIdShipConfirmDlqName, "mock:TODO");
 *
 *         camelContext.addRoutes(sampleIdShipCreateRouteBuilder);
 *         camelContext.addRoutes(sampleIdShipStatusRouteBuilder);
 *         camelContext.addRoutes(sampleIdShipConfirmRouteBuilder);
 *
 *     }
 * }
 * </code>
 */
@Configuration
@ConfigurationProperties
public class IngressFromPulseRouteBuilder {

    public static final String PULSE_GET_CALL_ROUTE_ID = "PulseGETCallRoute";
    public static final String DIRECT_PULSE_GET_CALL_ROUTE = "direct:" + PULSE_GET_CALL_ROUTE_ID;

    public static final String UTF_8 = "UTF-8";
    public static final String STASH_ENCODED_DATA_FLAG = "StashEncodedDataFlag";

    private static final String SQS_SCHEMA = "aws-sqs://";
    private static final String SQS_CLIENT_SUFFIX = "?amazonSQSClient=#amazonSQSClient";

    @Autowired(required = false)
    private PulsePayloadDataExtractionProcessor pulsePayloadDataExtractionProcessor;

    @Autowired(required = false)
    private PulseHeadersProcessor pulseHeadersProcessor;

    @Autowired(required = false)
    private SNSMessageProcessor snsMessageProcessor;

    @Autowired(required = false)
    private OutgoingRESTCallRouteConfiguration restCallRouteConfiguration;

    @Value("${ship.pulse.vipName:ship-internal_events-v1}")
    private String shipPulseVipName;

    @Value("${ship.pulse.urlSuffix:/ship/internal_events/v1/}")
    private String shipUrlSuffix;

    @Value("${sqs.no.consumers:1}")
    private String numberOfConsumers;

    @Value("${sqs.max.no.messages:10}")
    private int maxNumberOfMessages;

    @Value("${sqs.attributeNames:}")
    private String attributeNames;

    @Value("${sqs.messageAttributeNames:All}")
    private String messageAttributeNames;

    @Value("${sqs.initialDelay:1000}")
    private String initialDelay;

    @Value("${sqs.receiveMessageWaitTimeSeconds:0}")
    private String receiveMessageWaitTimeSeconds;

    @Value("${sqs.visibilityTimeout:30}")
    private String visibilityTimeout;

    @Value("${sqs.extendMessageVisibility:false}")
    private String extendMessageVisibility;

    /**
     * The maximum number of times Camel will retry after an exception
     *
     * Maps to Apache Camel `maximumRedeliveries`
     *
     * Defaults to 5
     */
    @Value("${camel.maxRedeliveryCount:5}")
    private int maxRedeliveryCount;

    /**
     * The base interval between redelivery attempts, in milliseconds
     *
     * Maps to Apache Camel `redeliveryDelay`
     *
     * Defaults to 2000
     */
    @Value("${camel.redeliveryDelayMs:2000}")
    private long redeliveryDelayMs;

    /**
     * The number by which the previous redelivery delay will be multiplied for the next retry attempt
     *
     * Maps to Apache Camel `backOffMultiplier`
     *
     * Defaults to 2
     */
    @Value("${camel.backOffMultiplier:2}")
    private int backOffMultiplier;

    /**
     * Deprecated as of version 2.3
     *
     * Use camel.maxRedeliveryCount instead
     *
     * Application will use sqs.maxRedeliveryCount instead of camel.maxRedeliveryCount if property file sets
     * sqs.maxRedeliveryCount to a value greater than 0.
     */
    @Deprecated
    @Value("${sqs.maxRedeliveryCount:0}")
    private int maxRedeliveryCount_deprecated;

    /**
     * Deprecated as of 2.3
     *
     * Use camel.redeliveryDelayMs instead
     *
     * Application will use sqs.redeliveryDelayMs instead of camel.redeliveryDelayMs if property file sets
     * sqs.redeliveryDelayMs to a value greater than 0.
     */
    @Deprecated
    @Value("${sqs.redeliveryDelayMs:0}")
    private long redeliveryDelayMs_deprecated;

    /**
     * Deprecated as of 2.3
     *
     * Use camel.backOffMultiplier instead
     *
     * Application will use sqs.backOffMultiplier instead of camel.backOffMultiplier if property file sets
     * sqs.backOffMultiplier to a value greater than 0.
     */
    @Deprecated
    @Value("${sqs.backOffMultiplier:0}")
    private int backOffMultiplier_deprecated;

    @Value("${sqs.delayMs:500}")
    private int delayMs;

    @Value("${sqs.backoffErrorThreshold:0}")
    private int backoffErrorThreshold;

    @Value("${sqs.backoffIdleThreshold:0}")
    private int backoffIdleThreshold;

    @Value("${sqs.deleteAfterRead:true}")
    private String deleteAfterRead;

    @Value("${sqs.deleteIfFiltered:true}")
    private String deleteIfFiltered;

    @Value("${sqs.messageRetentionPeriodSeconds:1209600}")
    private String messageRetentionPeriodSeconds;

    @Value("${sqs.greedy:false}")
    private String greedy;

    @Value("${sqs.runLoggingLevel:TRACE}")
    private String runLoggingLevel;

    @Value("${sqs.sendEmptyMessageWhenIdle:false}")
    private String sendEmptyMessageWhenIdle;

    @Value("${sqs.useFixedDelay:true}")
    private String useFixedDelay;

    @Value("${sqs.pollStrategy:}")
    private String pollStrategy;

    @Value("${sqs.scheduledExecutorService:}")
    private String scheduledExecutorService;

    @Value("${sqs.scheduler:}")
    private String scheduler;

    @Value("${sqs.scheduler.xxx:}")
    private String schedulerXxx;

    @Value("${sqs.startScheduler:true}")
    private String startScheduler;

    @Value("${suppressBusinessKeyValueException:true}")
    private boolean suppressBusinessKeyValueException;

    /**
     * Use this route builder method when on failure you want to send the messages to a DLQ that is preconfigured as the
     * dead letter queue of the given queueName, when there is no need to stash the encoded Pulse data in a header, and when a header
     * filter strategy is not needed.
     *
     * @param routeId
     * @param queueName
     * @return
     */
    public RouteBuilder createIngressFromPulseRouteBuilder(final String routeId, final String queueName,
                                                           final String consumerUri) {
        return createIngressFromPulseRouteBuilder(routeId, queueName, null, consumerUri);
    }

    /**
     * Use this route builder method when on failure you want to send the messages to a queue (dlqName) that is not preconfigured as
     * the dead letter queue of the given queueName, when there is no need to stash the encoded Pulse data in a header, and when a header
     * filter strategy is not needed.
     *
     * If dlqName is not null or empty string AND the queue with the name indicated by the value of the variable
     * dlqName is also configured as the dead letter queue of the queue indicated by the name of the variable
     * queueName, then on exception the message will be sent to the dlq twice, once per the configuration
     * and once per the explicitly dlqName passed in here. Since in nearly all cases the message should be sent
     * to the dlq only once, then if dlqName is not null the queue it indicates should not also be configured
     * as the dead letter queue of the queue indicated by queueName.
     *
     * @param routeId
     * @param queueName
     * @param dlqName
     * @return
     */
    public RouteBuilder createIngressFromPulseRouteBuilder(final String routeId, final String queueName, final String dlqName,
                                                           final String consumerUri) {
        return createIngressFromPulseRouteBuilder(routeId, queueName, dlqName, consumerUri, false, null );
    }

    /**
     * Use this method when configuring FIFO queue
     * To poll on a fifo queue it is mandatory to set messageGroupIdStrategy
     *
     * @param routeId
     * @param queueName
     * @param dlqName
     * @return
     */
    public RouteBuilder createIngressFromPulseRouteBuilder(final String routeId, final String queueName, final String dlqName,
                                                           final String consumerUri, final boolean isFifoQueue, final String messageGroupIdStrategy) {
        return createIngressFromPulseRouteBuilder(routeId, queueName, dlqName, consumerUri, isFifoQueue, messageGroupIdStrategy, false);
    }

    /**
     * Use this route builder method when a header filter strategy is not needed.
     * <p>
     * If the queueName has a preconfigured DLQ, enter null for dlqName to avoid sending messages to the DLQ twice.
     * If dlqName is not null or empty string AND the queue with the name indicated by the value of the variable
     * dlqName is also configured as the dead letter queue of the queue indicated by the name of the variable
     * queueName, then on exception the message will be sent to the dlq twice, once per the configuration
     * and once per the explicitly dlqName passed in here. Since in nearly all cases the message should be sent
     * to the dlq only once, then if dlqName is not null the queue it indicates should not also be configured
     * as the dead letter queue of the queue indicated by queueName.
     *
     * @param routeId
     * @param queueName
     * @param dlqName
     * @param stashEncodedData If true, encoded data from Pulse will be stashed in the {@value EventData#ENCODED_DATA} header
     * @return
     */
    public RouteBuilder createIngressFromPulseRouteBuilder(final String routeId, final String queueName, final String dlqName,
                                                           final String consumerUri, final boolean isFifoQueue, final String messageGroupIdStrategy, final boolean stashEncodedData) {
        return createIngressFromPulseRouteBuilder(routeId, queueName, dlqName, consumerUri, isFifoQueue, messageGroupIdStrategy, stashEncodedData, "");
    }

    /**
     * Use this route builder method when a filter header strategy or other HTTP4 parameters are needed.
     * <p>
     * A header filter strategy filters out exchange message headers from being added to the HTTP headers for
     * the call to Pulse.
     * <p>
     * An example header filter strategy, which assumes there is a loaded httpHeaderFilterStrategy bean, is:
     * <p>
     * "headerFilterStrategy=#httpHeaderFilterStrategy"
     * <p>
     * The first parameter should not start with & but subsequent parameters should be concatenated with &. For example:
     * <p>
     * "headerFilterStrategy=#httpHeaderFilterStrategy&connectionClose=true"
     * <p>
     * If the queueName has a preconfigured DLQ, enter null for dlqName to avoid sending messages to the DLQ twice.
     * If dlqName is not null or empty string AND the queue with the name indicated by the value of the variable
     * dlqName is also configured as the dead letter queue of the queue indicated by the name of the variable
     * queueName, then on exception the message will be sent to the dlq twice, once per the configuration
     * and once per the explicitly dlqName passed in here. Since in nearly all cases the message should be sent
     * to the dlq only once, then if dlqName is not null the queue it indicates should not also be configured
     * as the dead letter queue of the queue indicated by queueName.
     *
     * @param routeId
     * @param queueName
     * @param dlqName
     * @param stashEncodedData If true, encoded data from Pulse will be stashed in the {@value EventData#ENCODED_DATA} header
     * @param http4URLParametersSuffix Can include any valid URL parameter, including business parameters, Camel http4 parameters,
     *                                 Spring headers such as {@link org.apache.camel.http.common.HttpHeaderFilterStrategy}
     *                                 or other implementation of {@link org.apache.camel.spi.HeaderFilterStrategy} to
     *                                 prevent select header data from being added to REST and SQS headers. Value will be set in Camel header
     *                                 {@link OutgoingRESTCallRouteConfiguration#URL_PARAMETERS_SUFFIX}
     *                                 for HTTP call.
     * @return
     */
    public RouteBuilder createIngressFromPulseRouteBuilder(final String routeId, final String queueName, final String dlqName,
                                                           final String consumerUri, final boolean isFifoQueue, final String messageGroupIdStrategy, final boolean stashEncodedData,
                                                           final String http4URLParametersSuffix) {

        /**
         * First looks for non-zero values in deprecated properties
         *
         *      sqs.maxRedeliveryCount
         *      sqs.redeliveryDelayMs
         *      sqs.backOffMultiplier
         *
         * Values will initialize to zero if they are not set in the application properties file. If deprecated property
         * values are zero, then the non-deprecated properties will be used:
         *
         *      camel.maxRedeliveryCount
         *      camel.redeliveryDelayMs
         *      camel.backOffMultiplier
         *
         * These camel properties have their own defaults if they are not set in the properties file.
         */
        return new ExceptionHandlerRouteBuilder(
                maxRedeliveryCount_deprecated > 0 ? maxRedeliveryCount_deprecated : maxRedeliveryCount,
                redeliveryDelayMs_deprecated > 0L ? redeliveryDelayMs_deprecated : redeliveryDelayMs,
                backOffMultiplier_deprecated > 0 ? backOffMultiplier_deprecated : backOffMultiplier) {

            @Override
            public void configure() throws Exception {

                final String urlParametersSuffix = RouteUtil.constructURLParametersSuffix(http4URLParametersSuffix);

                String fromUri = SQS_SCHEMA + queueName + SQS_CLIENT_SUFFIX
                        + "&concurrentConsumers=" + numberOfConsumers
                        + "&maxMessagesPerPoll=" + maxNumberOfMessages
                        + "&messageAttributeNames=" + messageAttributeNames
                        + "&initialDelay=" + initialDelay
                        + "&receiveMessageWaitTimeSeconds=" + receiveMessageWaitTimeSeconds
                        + "&deleteAfterRead=" + deleteAfterRead
                        + "&deleteIfFiltered=" + deleteIfFiltered
                        + "&visibilityTimeout=" + visibilityTimeout
                        + "&extendMessageVisibility=" + extendMessageVisibility
                        + "&delay=" + delayMs
                        + "&backoffErrorThreshold=" + backoffErrorThreshold
                        + "&backoffIdleThreshold=" + backoffIdleThreshold
                        + "&greedy=" + greedy
                        + "&runLoggingLevel=" + runLoggingLevel
                        + "&sendEmptyMessageWhenIdle=" + sendEmptyMessageWhenIdle
                        + (StringUtils.isEmpty(attributeNames) ? "" : "&attributeNames=" + attributeNames)
                        + (StringUtils.isEmpty(messageRetentionPeriodSeconds) ? "" : "&messageRetentionPeriod=" + messageRetentionPeriodSeconds)
                        + (StringUtils.isEmpty(pollStrategy) ? "" : "&pollStrategy=#" + pollStrategy)
                        + (StringUtils.isEmpty(scheduledExecutorService) ? "" : "&scheduledExecutorService=#" + scheduledExecutorService)
                        + (StringUtils.isEmpty(scheduler) ? "" : "&scheduler=#" + scheduler)
                        + (StringUtils.isEmpty(schedulerXxx) ? "" : "&" + schedulerXxx);

                if(isFifoQueue && !StringUtils.isBlank(messageGroupIdStrategy)) {
                    fromUri += "&messageGroupIdStrategy=" + messageGroupIdStrategy;
                }

                super.configure();

                // If dlqName is not null or empty string AND the queue with the name indicated by the value of the variable
                // dlqName is also configured as the dead letter queue of the queue indicated by the name of the variable
                // queueName, then on exception the message will be sent to the dlq twice, once per the configuration
                // and once per the explicitly ".to(dlqUri)" defined here. Since in nearly all cases the message should be sent
                // to the dlq only once, then if dlqName is not null the queue it indicates should not also be configured
                // as the dead letter queue of the queue indicated by queueName.
                if (!StringUtils.isEmpty(dlqName)) {
                    configureDlq(SQS_SCHEMA + dlqName + SQS_CLIENT_SUFFIX);
                }

                includeRoutes(pulseGETCallRouteBuilder());

                from(fromUri)
                        .routeId(routeId)
                        .routeDescription("Reads message from queue and gets associated event from Pulse")
                        .bean(DistributedTraceProcessor.class)

                        // Saves the SNS message id, Pulse event id, and node values to headers
                        .process(snsMessageProcessor)

                        // Signs and sets JWT authorization headers
                        .setHeader(RouteConstants.PULSE_HTTP_REQUEST_METHOD, constant(RequestMethod.GET))
                        .process(pulseHeadersProcessor)

                        // Gets ship confirm from Pulse
                        .setHeader(OutgoingRESTCallRouteConfiguration.URL_PARAMETERS_SUFFIX, simple(urlParametersSuffix))
                        .setHeader(OutgoingRESTCallRouteConfiguration.URL_SUFFIX, header(SNSMessageProcessor.PULSE_EVENT_ID))
                        .log(routeId + " executing GET call to Pulse for eventId: ${header." + SNSMessageProcessor.PULSE_EVENT_ID + "}")
                        .enrich(DIRECT_PULSE_GET_CALL_ROUTE)
                        .convertBodyTo(String.class, UTF_8)
                        .removeHeader(OutgoingRESTCallRouteConfiguration.URL_PARAMETERS_SUFFIX)
                        .removeHeader(OutgoingRESTCallRouteConfiguration.URL_SUFFIX) // header must be removed to avoid its presence for subsequent POST calls
                        .log(routeId + " retrieved payload from Pulse for eventId ${header." + SNSMessageProcessor.PULSE_EVENT_ID + "}")


                        // Extracts the payload data from Pulse Event message
                        .process(pulsePayloadDataExtractionProcessor)

                        // Stashes the encoded data in a header before decoding it, if requested by the stashEncodedData flag
                        .setHeader(STASH_ENCODED_DATA_FLAG, simple(String.valueOf(stashEncodedData)))
                        .to(IngressFromPulseSubRoutes.DIRECT_ENCODED_DATA_HANDLING_ROUTE)

                        // Decodes the data per the EventData.ENCODING value (GZIP_BASE64 or BASE64)
                        .to(IngressFromPulseSubRoutes.DIRECT_DECODING_ROUTE)
                        .to(consumerUri);

            }
        };
    }

    /**
     * Adds the route configuration for the Hystrix REST calls to the Camel context.
     *
     * @return
     */
    private RouteBuilder pulseGETCallRouteBuilder() {
        return restCallRouteConfiguration.outgoingRESTRouteBuilder(shipPulseVipName, shipUrlSuffix,
                DIRECT_PULSE_GET_CALL_ROUTE, PULSE_GET_CALL_ROUTE_ID);
    }
}

