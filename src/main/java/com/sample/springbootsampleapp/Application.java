package com.sample.springbootsampleapp;


import samplephylon.jwt.auth.JWTAuthenticator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

import org.springframework.context.ConfigurableApplicationContext;

@Slf4j
@EnableEurekaClient
@SpringBootApplication(scanBasePackages = { "com.sample"} )
public class Application {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(Application.class, args);
        JWTAuthenticator jwtAuthBean = ctx.getBean(JWTAuthenticator.class);

        if (jwtAuthBean.isJwtEnabled()) {
            try {
                jwtAuthBean.configureWithDefaultSettings();
            } catch (Exception e) {
                log.error("Status=Error Event=JWTConfiguration", e);
            }
        }
        log.info("Application springbootsampleapp is up");
    }
}
