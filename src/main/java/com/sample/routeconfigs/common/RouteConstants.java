package com.sample.routeconfigs.common;

public final class RouteConstants {

    private RouteConstants() { }

    // Encoding
    public static final String UTF_8 = "UTF-8";

    // SQS Client
    public static final String SQS_SCHEMA = "aws-sqs://";
    public static final String SQS_CLIENT_SUFFIX = "?amazonSQSClient=#amazonSQSClient";

    // HTTP
    public static final String PULSE_HTTP_REQUEST_METHOD = "PulseHttpRequestMethod";
}
