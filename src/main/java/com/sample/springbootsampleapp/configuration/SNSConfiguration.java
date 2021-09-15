package com.sample.springbootsampleapp.configuration;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.*;
import com.amazonaws.services.sns.util.Topics;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.util.EC2MetadataUtils;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;



@Configuration
@ConfigurationProperties
@ComponentScan("com.sample.springbootsampleapp.configuration")
@Slf4j
public class SNSConfiguration {

    public static final String FILTER_POLICY_ATTRIBUTE_NAME = "FilterPolicy";

    @Value("${isLocal:false}")
    private boolean isLocal;

    @Value("${isLocalStack:false}")
    private boolean isLocalStack;

    @Value("${localstackSNSEndpoint:http://localhost:4575}")
    private String localstackSNSEndpoint;

    @Value("${wingtips.aws.region:us-east-1}")
    private String awsRegion;

    @Autowired(required =false)
    private Map<String, MessagingResourcesConfiguration.MessagingResources> messagingResources;

    @Autowired
    private AmazonSQS amazonSQS;

    private AmazonSNS amazonSNS;

    @Bean
    @DependsOn(MESSAGING_RESOURCES)
    public AmazonSNS amazonSNSClient() {
        if (amazonSNS == null) {
            return amazonSNSClient(isLocal, isLocalStack, localstackSNSEndpoint, amazonSQS, messagingResources);
        }
        return amazonSNS;
    }

    /**
     * Creates an SNS client for either local development or the Amazon cloud, depending on isLocal setting.
     *
     * @param isLocal
     * @param isLocalStack
     * @param localstackSNSEndpoint
     * @param amazonSQS
     * @param messagingResources
     * @return AmazonSNS client
     */
    // CHECKSTYLE IGNORE HiddenField
    // allow endpoints etc. to be passed into constructor for use with LocalstackDockerTestRunner
    @VisibleForTesting
    public AmazonSNS amazonSNSClient(boolean isLocal, boolean isLocalStack, String localstackSNSEndpoint,
                                     AmazonSQS amazonSQS, Map<String, MessagingResourcesConfiguration.MessagingResources> messagingResources) {
        this.isLocal = isLocal;
        this.isLocalStack = isLocalStack;
        this.localstackSNSEndpoint = localstackSNSEndpoint;
        this.amazonSQS = amazonSQS;
        this.messagingResources = messagingResources;

        if (this.isLocal) {
            log.info("isLocal = true");
            amazonSNS = createAmazonSNSForLocalEnvironment();
        } else {
            log.info("isLocal = false");
            amazonSNS = AmazonSNSClientBuilder.standard().withRegion(EC2MetadataUtils.getEC2InstanceRegion()).build();
        }

        if (amazonSNS != null) {
            configureSNS();
        }
        return amazonSNS;
    }
    // CHECKSTYLE END IGNORE HiddenField

    /**
     * Creates a localstack SNS client if isLocalStack=true. Does nothing if isLocalStack=false.
     *
     * @return AmazonSNS instance, or null
     */
    private AmazonSNS createAmazonSNSForLocalEnvironment() {
        if (isLocalStack) {
            log.info("Assuming that localstack is providing a local SNS service, because isLocalStack=true");
            return createLocalStackAmazonSNS();
        } else {
            log.info("Assuming that there is no local SNS service available, because isLocalStack=false");
            // TODO: In future provide local standalone SNS service
            return null;
        }
    }

    /**
     * For each <code>MessagingResources</code> that has a topic name, subscribes the resources' queue to the topic, if
     * a subscription of the queue to the topic does not exist.
     *
     * Updates an existing or newly created subscription with the resources' filterPolicy, if the filterPolicy is not null.
     */
    private void configureSNS() {
        log.info("Running the configurations for SNS");
        messagingResources.forEach((name, resources) -> {
            String topicName = resources.getTopicName();
            if (!StringUtils.isEmpty(topicName)) {
                String resourcesName = resources.getResourcesName();
                String queueName = resources.getQueueName();
                String filterPolicy = resources.getFilterPolicy();
                String subscriptionArn = getExistingSubscription(resourcesName, topicName, queueName);

                if (StringUtils.isEmpty(subscriptionArn)) {
                    subscriptionArn = subscribeQueueToTopic(name, topicName, queueName);
                }
                /**
                 * Defaults filter policy to empty if not provided.
                 */
                if (StringUtils.isEmpty(filterPolicy)) {
                    filterPolicy = "{}";
                }
                filterSubscription(subscriptionArn, filterPolicy);
            }
        });
    }

    /**
     * Sets the given filterPolicy on the given subscription.
     *
     * @param subscriptionArn
     * @param filterPolicy
     */
    private void filterSubscription(String subscriptionArn, String filterPolicy) {
        log.info("Setting {} subscription attribute {} = {}...", subscriptionArn, FILTER_POLICY_ATTRIBUTE_NAME,
                filterPolicy);
        SetSubscriptionAttributesRequest subscriptionAttributesRequest = new SetSubscriptionAttributesRequest(
                subscriptionArn, FILTER_POLICY_ATTRIBUTE_NAME, filterPolicy);
        amazonSNS.setSubscriptionAttributes(subscriptionAttributesRequest);
        log.info("The {} subscription attribute {} was successfully set to {}.", subscriptionArn,
                FILTER_POLICY_ATTRIBUTE_NAME, filterPolicy);
    }

    /**
     * Subscribes the given queue to the given topic.
     *
     * @param resourcesName
     * @param topicName
     * @param queueName
     * @return
     */
    private String subscribeQueueToTopic(String resourcesName, String topicName, String queueName) {
        String subscriptionArn;
        String topicArn = getTopicArn(resourcesName, topicName);

        if (!StringUtils.isEmpty(subscriptionArn = getExistingSubscription(resourcesName, topicArn, queueName))) {
            log.info("Queue {} is already subscribed to topic {}; will not attempt re-subscription.", topicArn, queueName);
        } else {
            subscriptionArn = Topics.subscribeQueue(amazonSNS, amazonSQS, topicArn, getQueueUrl(queueName));
            log.info("Subscribed queue {} to topic ARN {} with resulting subscription ARN: {}", queueName, topicArn, subscriptionArn);
        }
        return subscriptionArn;
    }

    /**
     * Creates a topic with the given name on localstack and sets the topic ARN on the resources map for later configuration
     * reference.
     *
     * @param resourcesName
     * @param topicName
     * @throws IllegalStateException if isLocalStack=false
     * @return
     */
    private String createLocalstackTopic(String resourcesName, String topicName) throws IllegalStateException {
        if (isLocalStack == true) {
            log.info("Creating topic {} on localstack SNS because isLocalStack=true", topicName);
            CreateTopicResult topic = amazonSNS.createTopic(topicName);
            String topicArn = topic.getTopicArn();
            messagingResources.get(resourcesName).setTopicARN(topicArn);
            log.info("Created topic {} on localstack; ARN: {}", topicName, topicArn);
            return topicArn;
        }
        throw new IllegalStateException(String.format("Failed to create localstack topic %s because isLocalStack=false",
                topicName));
    }

    /**
     * Returns the existing ARN of the given queue's subscription to the given topic. If an existing subscription of this
     * queue to the topic is not found, returns null.
     *
     * @param resourcesName
     * @param topicName
     * @param queueName
     * @return subscription ARN
     */
    private String getExistingSubscription(String resourcesName, String topicName, String queueName) {
        String topicArn = getTopicArn(resourcesName, topicName);
        try {
            ListSubscriptionsByTopicResult subscriptionsByTopicResult = amazonSNS.listSubscriptionsByTopic(topicArn);
            List<Subscription> subscriptions = subscriptionsByTopicResult.getSubscriptions();
            if (subscriptions != null && subscriptions.size() > 0) {
                for (Subscription subscription : subscriptions) {
                    String endpoint = subscription.getEndpoint();
                    //Check for the exact queue name and then return subscriptionArn
                    StringTokenizer stringTokenizer = new StringTokenizer(endpoint, ":");
                    while (stringTokenizer.hasMoreTokens()) {
                        if (stringTokenizer.nextToken().trim().equalsIgnoreCase(queueName)) {
                                String subscriptionArn = subscription.getSubscriptionArn();
                                log.info("Found ARN for subscription of queue {} to topic ARN {}: {}.", queueName, topicArn, subscriptionArn);
                                return subscriptionArn;
                        }
                    }
                }
            }
        } catch (NotFoundException e) {
            String msg = String.format("No ARN for given topic was found: %s", topicName);
            throw new IllegalStateException(msg, e);
        }
        log.warn("No ARN found for subscription of queue {} to topic ARN {}.", queueName, topicArn);
        return null;
    }

    /**
     * Returns newly created localstack SNS client.
     *
     * @return
     * @throws IllegalStateException if isLocalStack=false
     */
    private AmazonSNS createLocalStackAmazonSNS() throws IllegalStateException {
        if (isLocalStack == true) {
            AmazonSNS localStackAmazonSNS = AmazonSNSClientBuilder.standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(localstackSNSEndpoint,
                            awsRegion))
                    .build();
            return localStackAmazonSNS;
        }
        throw new IllegalStateException(String.format("Failed to create SNS client on localstack because isLocalStack=false"));
    }

    /**
     * Gets the ARN for the given topic name. If the topic does not exist and isLocalstack=true, creates a new
     * topic on localstack and returns its ARN.
     *
     * If no ARN is found
     * @param resourcesName
     * @param topicName
     * @throws IllegalStateException if no ARN is finally found
     * @return existing or localstack created topic ARN
     */
    private String getTopicArn(String resourcesName, String topicName) throws IllegalStateException {
        String topicArn = getExistingTopicArn(topicName, amazonSNS);

        if (topicArn == null && isLocal && isLocalStack) {
            log.info("Creating localstack topic: {}.", topicName);
            topicArn = createLocalstackTopic(resourcesName, topicName);
        }

        if (topicArn == null) {
            String msg = String.format("topicArn is null; cannot proceed with subscription to topic %s.", topicName);
            log.error(msg);
            throw new IllegalStateException(msg);
        }
        return topicArn;
    }

    /**
     * Gets the existing ARN of the given topic. If no existing ARN is found, returns null.
     *
     * @param topicName
     * @param amazonSNS
     * @return existing topic ARN
     */
    @VisibleForTesting
    public static String getExistingTopicArn(String topicName, AmazonSNS amazonSNS) {
        String topicArn = null;
        String nextToken = null;
        do {
            ListTopicsResult listTopics;
            if (nextToken == null) {
                listTopics = amazonSNS.listTopics();
            } else {
                listTopics = amazonSNS.listTopics(nextToken);
            }
            List<Topic> topics = listTopics.getTopics();
            for (Topic topic : topics) {
                if (topic.getTopicArn().endsWith(topicName)) {
                    topicArn = topic.getTopicArn();
                    log.info("Topic {} exists:", topicArn);
                    break;
                }
            }
            nextToken = listTopics.getNextToken();
        } while (topicArn == null && nextToken != null);

        return topicArn;
    }

    private String getQueueUrl(String queueName) {
        log.debug("SNSConfiguration.getQueueUrl: queueName={}", queueName);
        return amazonSQS.getQueueUrl(queueName).getQueueUrl();
    }
}

