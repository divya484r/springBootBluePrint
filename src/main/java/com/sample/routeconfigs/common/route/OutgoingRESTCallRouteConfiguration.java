package com.sample.routeconfigs.common.route;



import com.sample.springbootsampleapp.util.DistributedTraceProcessor;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class OutgoingRESTCallRouteConfiguration {

    // This is the name of the Camel header to set if you want to add a dynamic suffix to the URL.  For example,
    // when doing a GET call where the base URL needs to be followed by a UUID that is not known until runtime.
    public static final String URL_SUFFIX = "outgoingRestURLSuffix";

    // This is the name of the Camel header to set if you want to add parameters to the URLConstructor.
    public static final String URL_PARAMETERS_SUFFIX = "outgoingRestURLParametersSuffix";

    private static final String URL_SUFFIX_HEADER_VALUE = "${header." + URL_SUFFIX + "}";
    private static final String URL_PARAMETERS_SUFFIX_HEADER_VALUE = "${header." + URL_PARAMETERS_SUFFIX + "}";
    public static final String EVENT_MANAGER_HEADER = "X-sample-AppName";


    @Value("${info.app.name:}")
    private String appName;

    @Value("${camel.route.rest.to.http.maxRedeliveryCount:5}")
    private int maxRedeliveryCount;

    @Value("${camel.route.rest.to.http.redeliveryDelayMs:100}")
    private long redeliveryDelayMs;

    @Value("${camel.route.rest.to.vip.deliveryTimeOutMs:3000}")
    private int hystrixTimeout;

    @Value("${hystrix.http.protocol:http4}")
    private String httpProtocol;

    @Value("${camel.hystrix.max.queue.size:-1}")
    private int hystrixMaxQueueSize;

    @Value("${camel.hystrix.queue.rejection.threshold:5}")
    private int hystrixQueueSizeRejectionThreshold;

    @Value("${camel.hystrix.threadpool.coreSize:10}")
    private int hystrixPoolCoreSize;

    @Value("${camel.hystrix.threadpool.maximumSize:10}")
    private int hystrixPoolMaxSize;

    @Value("${camel.hystrix.threadpool.allowMaximumSizeToDivergeFromCoreSize:false}")
    private boolean isMaximumSizeToDivergeFromCoreSize;

    public RouteBuilder outgoingRESTRouteBuilder(final String vipName, final String baseURL, final String
            routeName, final String routeId) {
        return new RouteBuilder() {

            @Override
            public void configure() {

                from(routeName)
                        .routeId(routeId)
                        .description("Makes an outgoing REST call to " + vipName + baseURL
                                + " using Hystrix and Eureka")
                        .bean(DistributedTraceProcessor.class)

                        // Configure hystrix and make REST call
                        .setHeader(EVENT_MANAGER_HEADER, constant(appName))
                        .hystrix()
                        .hystrixConfiguration()
                        .executionTimeoutInMilliseconds(hystrixTimeout)
                        .maxQueueSize(hystrixMaxQueueSize)
                        .queueSizeRejectionThreshold(hystrixQueueSizeRejectionThreshold)
                        .corePoolSize(hystrixPoolCoreSize)
                        .maximumSize(hystrixPoolMaxSize)
                        .allowMaximumSizeToDivergeFromCoreSize(isMaximumSizeToDivergeFromCoreSize)
                        .end()// end of configuration block
                        .serviceCall().name(vipName)
                        .expression(method(URLConstructor.class,
                                "construct(" + httpProtocol + ":${header.CamelServiceCallServiceHost}"
                                        + ":${header.CamelServiceCallServicePort}"
                                        + baseURL + URL_SUFFIX_HEADER_VALUE + ", "
                                        + URL_PARAMETERS_SUFFIX_HEADER_VALUE + ")"))
                        .end()// end service call
                        .end();// end hystrix block
            }

        };
    }

    public static class URLConstructor {

        public String construct(String path, String parametersSuffix) {
            return path
                    + "?httpClient.SocketTimeout=10000"
                    + "&httpClient.ConnectTimeout=2000"
                    + "&httpClientConfigurer=#customJWTNoRetryConfigurer"
                    + handleConnectionClose(parametersSuffix)
                    + formatParametersSuffix(parametersSuffix);
        }
    }

    /**
     * Adds ampersand to beginning of parameters suffix if it is not already there
     *
     * @param parametersSuffix
     * @return formatted parameters suffix
     */
    public static String formatParametersSuffix(String parametersSuffix) {
        // Converts potential null to empty string
        parametersSuffix = StringUtils.isEmpty(parametersSuffix) ? "" : parametersSuffix;

        // Adds starting ampersand if it does not exist
        if (!parametersSuffix.isEmpty()) {
            parametersSuffix = parametersSuffix.startsWith("&") ? parametersSuffix : "&" + parametersSuffix;
        }
        return parametersSuffix;
    }

    /**
     * Returns connectionClose=true if parametersSuffix does not already include this parameter
     *
     * @param parametersSuffix
     * @return connectionClose=true if parameter not provided by client
     */
    public static String handleConnectionClose(String parametersSuffix) {
        // Converts potential null to empty string
        parametersSuffix = StringUtils.isEmpty(parametersSuffix) ? "" : parametersSuffix;

        // Adds parameter connectionClose=true if the parameter was not provided by the client
        if (!parametersSuffix.contains("connectionClose")) {
            return "&connectionClose=true";
        }
        // connectionClose parameter was provided by the client so do not add
        return "";
    }
}


