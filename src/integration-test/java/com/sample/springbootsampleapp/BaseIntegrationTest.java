package com.sample.springbootsampleapp;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Value;

/**
 * Base class which performs common
 * setup tasks for integration tests.
 */
public class BaseIntegrationTest {

    @Value("${instanceUrl:#{null}}")
    protected String instanceUrl;

    @Value("${local.server.port}")
    private int serverPort;

    @Before
    public void setup() throws Exception {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RestAssured.port = serverPort;

        if (instanceUrl != null) {
            RestAssured.baseURI = instanceUrl;
        } else {
            RestAssured.requestSpecification = new RequestSpecBuilder()
                    .setBaseUri(System.getProperty("APP_HOST", "http://localhost:" + serverPort))
                    .build();
        }
    }

}
