package com.sample.phylon.s3;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.netflix.config.ConfigurationManager;
import org.apache.commons.configuration.AbstractConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Guice Module to bind the Amazon S3 interface to either a default implementation or an in-memory implementation
 * that loads assets from the local file system.
 */
public class AmazonS3Module extends AbstractModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmazonS3Module.class);
    private static final AbstractConfiguration CONFIG = ConfigurationManager.getConfigInstance();

    @Override
    protected void configure() {
        Class client = isLocal() ? LocalS3Client.class : AmazonS3Client.class;
        LOGGER.info("Binding S3 Client: " + client.getCanonicalName());
        bind(AmazonS3.class).to(client).asEagerSingleton();
    }

    @Provides
    AmazonS3Client provideAmazon() {
        return new AmazonS3Client(new DefaultAWSCredentialsProviderChain());
    }

    @Provides
    LocalS3Client provideLocal() {
        return new LocalS3Client(CONFIG.getString("com.sample.s3.local.path", null));
    }

    private boolean isLocal() {
        return "local".equalsIgnoreCase(
                ConfigurationManager.getDeploymentContext().getDeploymentEnvironment()) &&
                CONFIG.getBoolean("com.sample.s3.local", false);
    }
}
