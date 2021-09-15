package com.sample.routeconfigs.common.route;

import org.apache.commons.lang.StringUtils;

/**
 * Utility to assist route configuration
 */
public final class RouteUtil {

    private RouteUtil() {}

    /**
     * Adds connectionClose=true parameter if a connectionClose parameter is not provided. This ensures available
     * sockets are not exhausted due to retries.
     *
     * Also removes a beginning & (ampersand) character since {@link camel.routebuilder.OutgoingRESTCallRouteConfiguration}
     * will add one to the beginning of these parameters.
     *
     * @param http4URLParametersSuffix
     * @return
     */
    public static String constructURLParametersSuffix(String http4URLParametersSuffix) {
        // Converts potential null to empty string if null
        String urlParametersSuffix = StringUtils.isEmpty(http4URLParametersSuffix) ? "" : http4URLParametersSuffix;

        // Adds parameter connectionClose=true if the parameter is not found
        if (!urlParametersSuffix.contains("connectionClose")) {
            urlParametersSuffix += "&connectionClose=true";
        }

        // Removes any starting ampersand because OutgoingRESTCallRouteConfiguration will add the first one
        urlParametersSuffix = urlParametersSuffix.replaceFirst("^&", "");
        return urlParametersSuffix;
    }
}
