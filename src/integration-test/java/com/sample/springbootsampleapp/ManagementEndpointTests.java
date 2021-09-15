package com.sample.springbootsampleapp;

import io.restassured.RestAssured;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.net.MalformedURLException;
import java.net.URL;

import static io.restassured.RestAssured.when;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = Application.class)
@ActiveProfiles("local")
@TestPropertySource(properties = {"management.server.port=0", "releaseVersion=1"})   // releaseVersion: with SpringBoot 2.0, property parsing is more strict. Need this mock value for /info endpoint.
public class ManagementEndpointTests extends BaseIntegrationTest {

    @Value("${local.management.port}")
    int localManagementPort;

    @Value("${remote.management.port:8077}")
    int remoteManagementPort;

    @Before
    public void setup() throws Exception {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        if (instanceUrl != null) {
            RestAssured.baseURI = extractHost(instanceUrl);
            RestAssured.port = remoteManagementPort;
        } else {
            RestAssured.requestSpecification = null;  // use defaults
            RestAssured.port = localManagementPort;
        }
    }

    @Test
    public void testHealth() {
        when().
                get("/health").
                then().
                statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void testInfo() {
        when().
                get("/info").
                then().
                statusCode(HttpStatus.SC_OK);
    }

    /**
     * Extract only the host from the given {@code instanceUrl}, discarding any
     * additional path information included as part of the url.
     */
    private String extractHost(String instanceUrl) throws MalformedURLException {
        final URL url = new URL(instanceUrl);
        return url.getProtocol() + "://" + url.getHost();
    }

}
