package com.sample.springbootsampleapp.configuration;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.*;
import com.amazonaws.util.EC2MetadataUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.util.HashMap;
import java.util.Map;



@Configuration
@ConfigurationProperties
@ComponentScan("com.sample.springbootsampleapp.configuration")
@Slf4j
public class SQSConfiguration {

    private static final String DLQ_ARN = "QueueArn";

    /**
     * This is the SQS endpoint variable that will be used to create local queues. It will be set dynamically
     * as a choice between localSQSEndpoint and localstackSQSEndpoint at runtime.
     */
    private String usedLocalSQSEndpoint;

    @Value("${isLocal:false}")
    private boolean isLocal;

    @Value("${isLocalStack:false}")
    private boolean isLocalStack;

    @Value("${wingtips.aws.region:us-east-1}")
    private String awsRegion;

    /**
     * The visibility timeout for the queue, in seconds. Valid values: an integer from 0 to 43,200 (12 hours). Default: 30.
     */
    @Value("${sqs.visibilityTimeout:30}")
    private String visibilityTimeout;

    /**
     * The length of time, in seconds, for which the delivery of all messages in the queue is delayed. Valid values: An
     * integer from 0 to 900 (15 minutes). Default: 0.
     */
    @Value("${sqs.delaySeconds:0}")
    private String delaySeconds;

    /**
     * The length of time, in seconds, for which a `` ReceiveMessage `` action waits for a message to arrive. Valid
     * values: an integer from 0 to 20 (seconds). Default: 0.
     */
    @Value("${sqs.receiveMessageWaitTimeSeconds:0}")
    private String receiveMessageWaitTimeSeconds;

    /**
     * The number of times a message is delivered to the source queue before being moved to the dead-letter queue. When
     * the ReceiveCount for a message exceeds the maxReceiveCount for a queue, Amazon SQS moves the message to the dead-letter-queue.
     */
    @Value("${sqs.maxReceiveCount:1}")
    private String maxReceiveCount;

    /**
     * The length of time, in seconds, for which Amazon SQS retains a message. Valid values: An integer representing
     * seconds, from 60 (1 minute) to 1,209,600 (14 days). Default: 1209600 (14 days).
     */
    @Value("${sqs.messageRetentionPeriodSeconds:1209600}")
    private String messageRetentionPeriodSeconds;

    /**
     * Provided from the application-local.properties file
     */
    @Value("${sqs.local.endpoint:http://localhost:4576}") // for localstack use 4576; for standalone elasticMQ use 9324
    private String localSQSEndpoint;

    /**
     * Provided when a test starts the application with code like this:
     *
     * <code>new SpringApplicationBuilder(Application.class).profiles("local")
     *                 .properties("localstackSNSEndpoint=" + snsEndpoint, "localstackSQSEndpoint=" + sqsEndpoint).run();</code>
     *
     * This provisioning method is used when running dockerized localstack, which uses dynamic ports.
     */
    @Value("${localstackSQSEndpoint:}")
    private String localstackSQSEndpoint;

    @Autowired(required = false)
    private Map<String, MessagingResourcesConfiguration.MessagingResources> messagingResources;

    private AmazonSQS sqs = null;

    @Bean
    @DependsOn(MESSAGING_RESOURCES)
    public AmazonSQS amazonSQSClient() {
        if (sqs != null) {
            log.info("SQSConfiguration.amazonSQSClient: Using existing SQS instance.");
            return sqs;
        }
        provisionSQS();
        return sqs;
    }

    // CHECKSTYLE IGNORE HiddenField
    // CHECKSTYLE IGNORE ParameterNumber
    @VisibleForTesting
    public AmazonSQS amazonSQSClient(boolean isLocal, boolean isLocalStack, Map<String, MessagingResourcesConfiguration.MessagingResources> messagingResources,
                                     String localstackSQSEndpoint, String visibilityTimeout, String delaySeconds,
                                     String receiveMessageWaitTimeSeconds, String maxReceiveCount) {
        this.isLocal = isLocal;
        this.isLocalStack = isLocalStack;
        this.messagingResources = messagingResources;
        this.usedLocalSQSEndpoint = localstackSQSEndpoint; // not all tests provide the endpoint through the localstackSQSEndpoint property so this is necessary
        this.visibilityTimeout = visibilityTimeout;
        this.delaySeconds = delaySeconds;
        this.receiveMessageWaitTimeSeconds = receiveMessageWaitTimeSeconds;
        this.maxReceiveCount = maxReceiveCount;
        return amazonSQSClient();
    }
    // CHECKSTYLE END IGNORE HiddenField
    // CHECKSTYLE END IGNORE ParameterNumber

    private void provisionSQS() {

        log.debug("SQSConfiguration.provisionSQS: isLocal={}", isLocal);
        log.debug("SQSConfiguration.provisionSQS: isLocalStack={}", isLocalStack);

        if (isLocal) {
            provisionLocalQueues();
        } else {
            provisionCloudQueues();
        }
    }

    private void createQueue(String queueName) {

        try {
            log.info("Checking if queue already exists: {}.", queueName);
            GetQueueUrlResult result = sqs.getQueueUrl(queueName);
            String url = result.getQueueUrl();
            log.info("Queue already exists; will not attempt to re-create: {}; url: {}.", queueName, url);
        } catch (QueueDoesNotExistException e) {
            log.warn("Queue does not already exist; creating: {}.", queueName);
            CreateQueueRequest queueRequest = new CreateQueueRequest()
                    .withQueueName(queueName);
            CreateQueueResult result = sqs.createQueue(queueRequest);
            log.info("Queue creation successful: {}; url: {}.", queueName, result.getQueueUrl());
        } catch (Exception e) {
            log.error("Exception occurred in the createQueue method: {}.", queueName, e);
        }
    }

    private void configureQueue(String queueName, String dlQueueName) {

        try {
            String queueUrl = sqs.getQueueUrl(queueName).getQueueUrl();
            Map<String, String> attributes = buildAttributes(dlQueueName);
            sqs.setQueueAttributes(queueUrl, attributes);
            log.info("Queue configuration successful for queue: {}.", queueName);
        } catch (QueueDoesNotExistException e) {
            String msg = String.format("Queue to configure does not exist: %s.", queueName);
            throw new IllegalStateException(msg, e);
        } catch (Exception e) {
            String msg = String.format("Exception occurred while configuring queue: %s.", queueName);
            throw new RuntimeException(msg, e);
        }
    }

    private Map<String, String> buildAttributes(String dlQueueName) {
        Map<String, String> attributes = new HashMap<>();
        try {
            attributes.put("VisibilityTimeout", visibilityTimeout);
            attributes.put("DelaySeconds", delaySeconds);
            attributes.put("ReceiveMessageWaitTimeSeconds", receiveMessageWaitTimeSeconds);
            attributes.put("MessageRetentionPeriod", messageRetentionPeriodSeconds);

            if (!Strings.isNullOrEmpty(dlQueueName)) {
                attributes.put("RedrivePolicy", buildRedrivePolicy(dlQueueName));
            }
        } catch (QueueDoesNotExistException e) {
            String msg = String.format("Dead letter queue %s does not exist.", dlQueueName);
            throw new IllegalStateException(msg, e);
        } catch (Exception e) {
            String msg = String.format("Exception occurred while building queue attributes with dlQueue: %s.", dlQueueName);
            throw new RuntimeException(msg, e);
        }
        return attributes;
    }

    private String buildRedrivePolicy(String dlQueueName) {
        GetQueueAttributesRequest queueAttributesRequest = new GetQueueAttributesRequest(
                sqs.getQueueUrl(dlQueueName).getQueueUrl()).withAttributeNames(DLQ_ARN);
        Map<String, String> sqsAttributeMap = sqs.getQueueAttributes(queueAttributesRequest).getAttributes();
        log.debug("SQSConfiguration.createQueue: DLQ ARN " + sqsAttributeMap.get(DLQ_ARN));
        String maxReceiveCt = isLocalStack ? "\"" + maxReceiveCount + "\"" : maxReceiveCount;
        String redrivePolicy = "{\"maxReceiveCount\":" + maxReceiveCt + ", \"deadLetterTargetArn\":\"" + sqsAttributeMap.get(DLQ_ARN) + "\"}";

        return redrivePolicy;
    }

    private void provisionLocalQueues() {
        usedLocalSQSEndpoint = chooseSQSEndpoint();
        log.info("SQSConfiguration.provisionSQS: Configuring SQS for local profile.");
        sqs = AmazonSQSClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(usedLocalSQSEndpoint, awsRegion)).build();
        createQueues();
        configureQueues();

    }

    private void provisionCloudQueues() {
        log.info("SQSConfiguration.provisionSQS: Configuring SQS for cloud profile.");
        sqs = AmazonSQSClientBuilder.standard().withRegion(EC2MetadataUtils.getEC2InstanceRegion())
                .build();
        createQueues();
        configureQueues();
    }

    private void createQueues() {
        messagingResources.forEach((name, resources) -> {
            createQueue(resources.getDlQueueName());
            createQueue(resources.getQueueName());
        });
    }

    private void configureQueues() {
        messagingResources.forEach((name, resources) -> {
            configureQueue(resources.getDlQueueName(), null);
            configureQueue(resources.getQueueName(), resources.getDlQueueName());
        });
    }

    private String chooseSQSEndpoint() {
        if (usedLocalSQSEndpoint != null) {
            // This may have already been set by a test class calling the amazonSQSClient method with parameters.
            return usedLocalSQSEndpoint;
        }
        if (isLocalStack && !StringUtils.isEmpty(localstackSQSEndpoint)) {
            // Not all tests call the amazonSQSClient method with parameters but pass the value in through
            // the localstackSQSEndpoint property at runtime for dynamic ports on docker.
            return localstackSQSEndpoint;
        }
        // This is the value from the application-local.properties file. This scenario may occur during development when
        // localstack is not dockerized but has been manually started or elasticMQ is being used.
        return localSQSEndpoint;
    }
}
