package com.sample.springbootsampleapp;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = Application.class)
@ActiveProfiles("local")
@TestPropertySource(properties = {"management.server.port=0"})
public class ApplicationTests extends BaseIntegrationTest {

    @Test
    public void testExampleController_example() throws Exception {
        given().
            when().
            get("/ship/springbootsampleapp/v1/").
            then().
            statusCode(HttpStatus.SC_OK)
            .and().body("success", equalTo("true"));
    }

}
