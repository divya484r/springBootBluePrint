package com.sample.routeconfigs.egress.route.configuration;

import com.sample.routeconfigs.common.RouteConstants;
import com.sample.routeconfigs.common.model.pulse.EventContext;
import com.sample.routeconfigs.common.model.pulse.EventData;
import com.sample.routeconfigs.common.route.OutgoingRESTCallRouteConfiguration;
import com.sample.routeconfigs.common.route.RouteUtil;
import com.sample.routeconfigs.common.route.processor.PulseHeadersProcessor;
import com.sample.routeconfigs.egress.route.processor.PulsePOSTPayloadProcessor;
import com.jayway.jsonpath.PathNotFoundException;

import com.sample.routeconfigs.exception.ExceptionHandlerRouteBuilder;
import com.sample.springbootsampleapp.util.DistributedTraceProcessor;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;

import static com.sample.routeconfigs.common.RouteConstants.*;


/**
 * This class is a generic Camel route for posting event data to Pulse. The route gzips and base64 encodes the incoming
 * exchange message body, creates a Pulse event payload with the resulting body, and posts the message to Pulse.
 * <p>
 *
 * Required incoming headers:
 * <ul>
 *     <li>EventContextBusinessKeyValue; See {@value EventContext#BUSINESS_KEY_VALUE}</li>
 *     <li>EventContextBusinessKeyName; See {@value EventContext#BUSINESS_KEY_NAME}</li>
 *     <li>EventContextName; See {@value EventContext#NAME}</li>
 *     <li>EventDataContentType; See {@value EventData#CONTENT_TYPE}</li>
 *     <li>EventDataEncoding; See {@value EventData#ENCODING}</li>
 * </ul>
 *
 * Optional incoming headers:
 * <ul>
 *     <li>EventContextRetentionDays; See {@value EventContext#RETENTION_DAYS}</li>
 *     <li>EventContextType; See {@value EventContext#TYPE}</li>
 *     <li>EventContextVersion; See {@value EventContext#VERSION}</li>
 *     <li>EventContextFilterMapEntrySets; See {@value EventContext#EVENT_CONTEXT_FILTER_MAP_ENTRY_SETS}</li>
 * </ul>
 *
 * <p>
 * This class will automatically be used when you call {@link PostToPulseEndRoute}.
 * <p>
 * Example Implementation:
 * <code>
 *   from(...)
 *      ...
 *      .setHeader(EventContext.BUSINESS_KEY_VALUE, simple("12345"))
 *      .setHeader(EventContext.BUSINESS_KEY_NAME, simple("purchaseOrderNumber"))
 *      .setHeader(EventContext.NAME, simple(".setHeader(EventContext.NAME, simple("ce_fmgr_shiprequest"))"))
 *      .setHeader(EventData.CONTENT_TYPE, simple("application/json")
 *      .setHeader(EventData.ENCODING, simple("GZIP_BASE64"))
 *      .to(PostToPulseEndRoute.DIRECT_POST_TO_PULSE_END_ROUTE)
 *      ...
 * </code>
 *
 * Example implementation using this library's XMLEventMetadataSetter:
 * <code>
 *     from(...)
 *      ...
 *      .bean(XMLEventMetadataSetter.class, "setMetadata(${exchange}, \"ce_fmgr_shiprequest\", \"SHIP_CANCEL\""),
 *      .to(PostToPulseEndRoute.DIRECT_POST_TO_PULSE_END_ROUTE)
 *      ...
 * </code>
 */
@Configuration
@ConfigurationProperties
public class EgressToPulseRouteBuilder {

    public static final String PULSE_POST_CALL_ROUTE_ID = "PulsePOSTCallRoute";
    public static final String DIRECT_PULSE_POST_CALL_ROUTE = "direct:" + PULSE_POST_CALL_ROUTE_ID;
    public static final String PULSE_RESPONSE_EVENTID = "PulseResponseEventId";

    @Autowired(required = false)
    private PulsePOSTPayloadProcessor pulsePOSTPayloadProcessor;

    @Autowired(required = false)
    private PulseHeadersProcessor pulseHeadersProcessor;

    @Autowired(required = false)
    private OutgoingRESTCallRouteConfiguration restCallRouteConfiguration;

    @Value("${ship.pulse.vipName}")
    private String shipPulseVipName;

    @Value("${ship.pulse.urlSuffix}")
    private String shipUrlSuffix;

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

    /**
     * Use this route builder method when no header filter strategy or other URL parameters are needed, and this route
     * should not route to a next route, and no DLQ needs to be configured.
     *
     * @param fromUri this is from URI for the route, for this endpoint direct is appended so when calling this method
     *                  just mention the name of the route for example 'ShipConfirmRoute' instead of 'direct:ShipConfirmRoute'
     *
     * @param routeId this will be the routeId for this route
     * @return
     */
    public RouteBuilder createEgressToPulseRouteBuilder(String fromUri, String routeId) {
        return createEgressToPulseRouteBuilder(fromUri, routeId, null);
    }

    /**
     * Use this route builder method when this route should not route to a next route and no DLQ needs to be configured.
     *
     * @param http4URLParametersSuffix Can include any valid URL parameter, including business parameters, Camel http4 parameters,
     *                                 Spring headers such as {@link org.apache.camel.http.common.HttpHeaderFilterStrategy}
     *                                 or other implementation of {@link org.apache.camel.spi.HeaderFilterStrategy} to
     *                                 prevent select header data from being added to REST and SQS headers. Value will be set in Camel header
     *                                 {@link OutgoingRESTCallRouteConfiguration#URL_PARAMETERS_SUFFIX}
     *                                 for HTTP call.
     *@param fromUri this is from URI for the route, for this endpoint direct is appended so when calling this method
     *                 just mention the name of the route for example 'ShipConfirmRoute' instead of 'direct:ShipConfirmRoute'
     *@param routeId this will be the routeId for this route
     * @return
     */
    public RouteBuilder createEgressToPulseRouteBuilder(String fromUri, String routeId, String http4URLParametersSuffix) {
        return createEgressToPulseRouteBuilder(fromUri, routeId, http4URLParametersSuffix, null);
    }


    /**
     * Use this route builder method when a filter header strategy or other HTTP4 parameters are needed, a next route
     * should be added to the end of this route, and exception handlers should route to a DLQ URI.
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
     * Provide a dlqName when on failure you want to send the messages to a queue (dlqName) that is not preconfigured as
     * the dead letter queue of the given queueName, when there is no need to stash the encoded Pulse data in a header, and when a header
     * filter strategy is not needed.
     * <p>
     * If dlqName is not null or empty string AND the queue with the name indicated by the value of the variable
     * dlqName is also configured as the dead letter queue of the queue indicated by the name of the variable
     * queueName, then on exception the message will be sent to the dlq twice, once per the configuration
     * and once per the explicitly dlqName passed in here. Since in nearly all cases the message should be sent
     * to the dlq only once, then if dlqName is not null the queue it indicates should not also be configured
     * as the dead letter queue of the queue indicated by queueName.
     *
     * @param http4URLParametersSuffix Can include any valid URL parameter, including business parameters, Camel http4 parameters,
     *                                 Spring headers such as {@link org.apache.camel.http.common.HttpHeaderFilterStrategy}
     *                                 or other implementation of {@link org.apache.camel.spi.HeaderFilterStrategy} to
     *                                 prevent select header data from being added to REST and SQS headers. Value will be set in Camel header
     *                                 {@link OutgoingRESTCallRouteConfiguration#URL_PARAMETERS_SUFFIX}
     *                                 for HTTP call.
     * @param fromUri this is from URI for the route, for this endpoint direct is appended so when calling this method
     * just mention the name of the route for example 'ShipConfirmRoute' instead of 'direct:ShipConfirmRoute'
     * @param routeId this will be the routeId for this route
     * @param dlqName this should be a DLQ Name, not a DLQ URI
     *
     * @return PulsePOSTCAllRoute
     */
    public RouteBuilder createEgressToPulseRouteBuilder(final String fromUri, final String routeId, final String http4URLParametersSuffix, final String dlqName) {

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
                super.configure();

                onException(PathNotFoundException.class)
                        .bean(DistributedTraceProcessor.class)
                        .log(LoggingLevel.WARN, "Failed to retrieve eventId from pulse response")
                        .handled(true);


                // If dlqName is not null or empty string AND the queue with the name indicated by the value of the variable
                // dlqName is also configured as the dead letter queue of the queue indicated by the name of the variable
                // queueName, then on exception the message will be sent to the dlq twice, once per the configuration
                // and once per the explicitly ".to(dlqUri)" defined here. Since in nearly all cases the message should be sent
                // to the dlq only once, then if dlqName is not null the queue it indicates should not also be configured
                // as the dead letter queue of the queue indicated by queueName.
                if (!StringUtils.isEmpty(dlqName)) {
                    configureDlq(SQS_SCHEMA + dlqName + SQS_CLIENT_SUFFIX);
                }

                includeRoutes(pulsePOSTCallRouteBuilder());

                from(fromUri)
                        .routeId(routeId)
                        .routeDescription("Base64 encodes and gzips body and posts to Pulse")
                        .bean(DistributedTraceProcessor.class)

                        // Prepares the payload for posting to Pulse
                        .setHeader(RouteConstants.PULSE_HTTP_REQUEST_METHOD, constant(RequestMethod.POST))
                        .process(pulseHeadersProcessor)
                        .marshal().gzip()
                        .marshal().base64()
                        .convertBodyTo(String.class, UTF_8)
                        .process(pulsePOSTPayloadProcessor)
                        .marshal().json(JsonLibrary.Jackson)

                        .setHeader(OutgoingRESTCallRouteConfiguration.URL_PARAMETERS_SUFFIX, simple(urlParametersSuffix))

                        // Posts the processed payload to Pulse
                        .log("Posting event data to Pulse for business key=${header."
                                + EventContext.BUSINESS_KEY_VALUE + "}")
                        .to(DIRECT_PULSE_POST_CALL_ROUTE)
                        .setHeader(PULSE_RESPONSE_EVENTID).jsonpath("$.links.self.ref")
                        .log("Successfully posted event data to Pulse for business key =${header."
                                + EventContext.BUSINESS_KEY_VALUE + "} with eventId: ${header." + PULSE_RESPONSE_EVENTID + "}")
                        .end();

            }
        };
    }

    private RouteBuilder pulsePOSTCallRouteBuilder() {
        return restCallRouteConfiguration.outgoingRESTRouteBuilder(shipPulseVipName, shipUrlSuffix,
                DIRECT_PULSE_POST_CALL_ROUTE, PULSE_POST_CALL_ROUTE_ID);
    }
}

