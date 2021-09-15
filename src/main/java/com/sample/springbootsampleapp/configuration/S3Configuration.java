package com.sample.springbootsampleapp.configuration;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.netflix.config.ConfigurationManager;
import com.sample.phylon.s3.LocalS3Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Slf4j
public class S3Configuration {

    @Value("${com.sample.s3.local.path:#{null}}")
    private String localS3Path;
    @Value("${com.sample.s3.local.write:false}")
    private boolean localWrite;


    @Bean
    @Profile("local")
    public AmazonS3 amazonLocalS3Client() {
        AmazonS3 client = new LocalS3Client(localS3Path);
        ConfigurationManager.getConfigInstance().setProperty("com.sample.s3.local.write", localWrite);
        log.info("AmazonS3Client is created in local with bucket: {} with write access {}", localS3Path, localWrite);
        return client;
    }

    @Bean
    @Profile("!local")
    public AmazonS3 amazonS3Client() {
        AmazonS3 client = AmazonS3ClientBuilder.defaultClient();
        log.info("AmazonS3Client is created");
        return client;
    }

}
