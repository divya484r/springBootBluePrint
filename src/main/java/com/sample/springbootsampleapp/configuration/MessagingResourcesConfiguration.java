package com.sample.springbootsampleapp.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Messaging resources are topics and queues.
 * <p>
 *
 * Each project that imports this library module must extend this class. The getMessagingResources method implementation
 * must return a Map of MessagingResources instances that correspond to a topic and the queue that subscribes to that
 * topic. A typical implementing class might contain content similar to code snippet below.
 *
 * Rules for implementing:
 * <ol>
 *     <le>When a queue is internal (that is, does not subscribe to a topic) enter null for the topic name.</le>
 *     <le>For all MessagingResources configurations, the topicARN should not be set; it will be populated at runtime.</le>
 * </ol>
 *
 * <pre>
 * {@code
 *     public static final String TRANSFER_ORDER_SHIP_REQUEST_RESOURCES = "TransferOrderShipRequestResources";
 *     public static final String SHIP_CONFIRM_THIN_OM_RESOURCES = "ShipConfirmThinOMResources";
 *     public static final String SHIP_CONFIRM_INTAKE_RESOURCES = "ShipConfirmIntakeResources";
 *     public static final String SHIP_CONFIRM_FUSION_RESOURCES = "ShipConfirmFusionResources";
 *
 *     @Value("${sns.transfer-order.ship-request.topic.name}")
 *     private String transferOrderShipRequestTopicName;
 *
 *     @Value("${sqs.transferorder-shipreq.queue.name}")
 *     private String transferOrderShipRequestQueue;
 *
 *     @Value("${sqs.transferorder-shipreq.dlq.name}")
 *     private String transferOrderShipRequestDlQueue;
 *
 *     @Value("${sns.ship-confirm.topic.name}")
 *     private String shipConfirmTopicName;
 *
 *     @Value("${sqs.fmg-shipconf-intake.queue.name}")
 *     private String shipConfirmIntakeQueue;
 *
 *     @Value("${sqs.fmg-shipconf-intake-dlq.queue.name}")
 *     private String shipConfirmIntakeDlQueue;
 *
 *     @Value("${sqs.fmg-shipconf-thin-om.queue.name}")
 *     private String shipConfirmThinOMQueue;
 *
 *     @Value("${sqs.fmg-shipconf-thin-om-dlq.queue.name}")
 *     private String shipConfirmThinOMDlQueue;
 *
 *     @Value("${sqs.fmg-shipconf-fusion.queue.name}")
 *     private String shipConfirmFusionQueue;
 *
 *     @Value("${sqs.fmg-shipconf-fusion-dlq.queue.name}")
 *     private String shipConfirmFusionDlQueue;
 *
 *     @Bean(name = MESSAGING_RESOURCES)
 *     @Override
 *     public Map<String, MessagingResources> getMessagingResources() {
 *         return Arrays.asList(getTransferOrderShipRequestMessagingResources(),
 *                 getShipConfirmThinOMMessagingResources(),
 *                 getShipConfirmIntakeMessagingResources(),
 *                 getShipConfirmFusionMessagingResources())
 *                 .stream().collect(Collectors.toMap(MessagingResources::getResourcesName, messagingResources -> messagingResources));
 *     }
 *
 *     public MessagingResources getTransferOrderShipRequestMessagingResources() {
 *         return new MessagingResources(TRANSFER_ORDER_SHIP_REQUEST_RESOURCES, transferOrderShipRequestTopicName,
 *                 transferOrderShipRequestQueue, transferOrderShipRequestDlQueue);
 *     }
 *
 *     public MessagingResources getShipConfirmIntakeMessagingResources() {
 *         return new MessagingResources(SHIP_CONFIRM_INTAKE_RESOURCES, shipConfirmTopicName,
 *                 shipConfirmIntakeQueue, shipConfirmIntakeDlQueue);
 *     }
 *
 *     public MessagingResources getShipConfirmThinOMMessagingResources() {
 *         return new MessagingResources(SHIP_CONFIRM_THIN_OM_RESOURCES, null,
 *                 shipConfirmThinOMQueue, shipConfirmThinOMDlQueue);
 *     }
 *
 *     public MessagingResources getShipConfirmFusionMessagingResources() {
 *         return new MessagingResources(SHIP_CONFIRM_FUSION_RESOURCES, null,
 *                 shipConfirmFusionQueue, shipConfirmFusionDlQueue);
 *     }
 * </pre>
 *
 */
@Configuration
public abstract class MessagingResourcesConfiguration {

    public static final String MESSAGING_RESOURCES = "MessagingResources";

    @Bean(name = MESSAGING_RESOURCES)
    public abstract Map<String, MessagingResources> getMessagingResources();

    @Getter
    @Setter
    public static class MessagingResources {
        private String resourcesName;
        private String topicName;
        private String queueName;
        private String dlQueueName;
        private String topicARN;
        private String filterPolicy;

        public MessagingResources(String resourcesName, String topicName, String queueName, String dlQueueName) {
            this.resourcesName = resourcesName;
            this.topicName = topicName;
            this.queueName = queueName;
            this.dlQueueName = dlQueueName;
        }

        public MessagingResources(String resourcesName, String topicName, String queueName, String dlQueueName,
                                  String filterPolicy) {
            this(resourcesName, topicName, queueName, dlQueueName);
            this.filterPolicy = filterPolicy;
        }
    }


}
