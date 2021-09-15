package com.sample.springbootsampleapp.configuration;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.QueueNameExistsException;
import com.amazonaws.util.EC2MetadataUtils;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties
@Slf4j
public class AFSSAPSQSConfiguration {

    private static final String DLQ_ARN = "QueueArn";

    @Value("${com.sample.sqs.local:false}")
    private boolean isLocal;

    @Value("${sqs.afssapshipconfirm.queue.name}")
    private String afsSapShipConfirmQueue;

    @Value("${sqs.afssapshipconfirm.dlq.queue.name}")
    private String afsSapShipConfirmEventDlqQueue;

    @Value("${sqs.afssapshipconfirm.cancel.queue.name}")
    private String cancelEventQueue;

    @Value("${sqs.afssapshipconfirm.cancel.dlq.name}")
    private String cancelEventDlq;

    @Value("${sqs.afssapshipconfirm.nsp.queue.name}")
    private String afsSapPostToNspQueue;

    @Value("${sqs.afssapshipconfirm.nsp.dlq.name}")
    private String afsSapPostToNspDlqQueue;

    @Value("${sqs.local.endpoint:}")
    private String sqsEndpoint;

    private AmazonSQS sqs = null;

    @Bean
    public AmazonSQS amazonSQSClient() {

        log.debug("MessageSQSConfiguration.amazonSQSClient() isLocal={}", isLocal);
        if (sqs != null) {
            return sqs;
        }
        if (isLocal) {
            log.info("In local profile for sqs setup");
            val defaultRegion = "us-east-1";
            sqs = AmazonSQSClientBuilder.standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(sqsEndpoint, defaultRegion))
                    .build();
            createQueue(afsSapShipConfirmEventDlqQueue, null);
            createQueue(afsSapShipConfirmQueue, afsSapShipConfirmEventDlqQueue);

            createQueue(cancelEventDlq, null);
            createQueue(cancelEventQueue, cancelEventDlq);

            createQueue(afsSapPostToNspDlqQueue, null);
            createQueue(afsSapPostToNspQueue, afsSapPostToNspDlqQueue);
        } else {
            log.info("In non-local profile for sqs setup");
            sqs = AmazonSQSClientBuilder.standard().withRegion(EC2MetadataUtils.getEC2InstanceRegion()).build();
        }
        return sqs;
    }

    @Primary
    @Bean(name = "afsSapShipConfirmQueue")
    public String getAfsSapShipConfirmEventQueueUrl() {
        log.debug("Bean getAfsSapShipConfirmEventQueueUrl, queue={}", afsSapShipConfirmQueue);
        return amazonSQSClient().getQueueUrl(afsSapShipConfirmQueue).getQueueUrl();
    }

    @Primary
    @Bean(name = "cancelEventQueue")
    public String getCancelEventQueueUrl() {
        log.debug("Bean getCancelEventQueueUrl, queue={}", cancelEventQueue);
        return amazonSQSClient().getQueueUrl(cancelEventQueue).getQueueUrl();
    }

    @Bean(name = "afsSapShipConfirmEventDlqQueue")
    public String getSapShipConfirmEventDlqQueueUrl() {
        log.debug("Bean getSapShipConfirmEventDlqQueueUrl, queue={}", afsSapShipConfirmEventDlqQueue);
        return amazonSQSClient().getQueueUrl(afsSapShipConfirmEventDlqQueue).getQueueUrl();
    }

    @Bean(name = "cancelEventDlqUrl")
    public String getCancelEventDlqeUrl() {
        log.debug("Bean getCancelEventDlqUrl, queue={}", cancelEventDlq);
        return amazonSQSClient().getQueueUrl(cancelEventDlq).getQueueUrl();
    }
    @Bean(name = "afsSapPostToNspDlqQueue")
    public String getPosttopulseEventDlqeUrl() {
        log.debug("Bean getCancelEventDlqUrl, queue={}", afsSapPostToNspDlqQueue);
        return amazonSQSClient().getQueueUrl(afsSapPostToNspDlqQueue).getQueueUrl();
    }

    private void createQueue(String queueName, String dlQueueName) {

        try {
            Map<String, String> attributes = new HashMap<>();
            attributes.put("VisibilityTimeout", "60");
            attributes.put("DelaySeconds", "5");
            attributes.put("ReceiveMessageWaitTimeSeconds", "0");
            if (!Strings.isNullOrEmpty(dlQueueName)) {
                GetQueueAttributesRequest queueAttributesRequest = new GetQueueAttributesRequest(
                        sqs.getQueueUrl(dlQueueName).getQueueUrl()).withAttributeNames(DLQ_ARN);
                Map<String, String> sqsAttributeMap = sqs.getQueueAttributes(queueAttributesRequest).getAttributes();
                log.debug("DLQ ARN " + sqsAttributeMap.get(DLQ_ARN));
                String maxReceiveCount = isLocal ? "\"1\"" : "5";
                String redrivePolicy = "{\"maxReceiveCount\":" + maxReceiveCount + ", \"deadLetterTargetArn\":\""
                        + sqsAttributeMap.get(DLQ_ARN) + "\"}";
                attributes.put("RedrivePolicy", redrivePolicy);
            }
            CreateQueueRequest queueRequest = new CreateQueueRequest().withQueueName(queueName)
                    .withAttributes(attributes);
            CreateQueueResult createQueueResult = sqs.createQueue(queueRequest);
            log.info("{}, queue creation successful, createQueueResult = {} ", queueName, createQueueResult);
        } catch (QueueNameExistsException e) {
            log.warn("{} Queue already exists, Exception is : {} ", queueName, e);
        } catch (Exception e) {
            log.error("{} Unknown Exception is : {} ", queueName, e);
        }

    }

}
