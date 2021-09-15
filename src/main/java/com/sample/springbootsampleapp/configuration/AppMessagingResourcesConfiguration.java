package com.sample.springbootsampleapp.configuration;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Configures SNS topics and SQS queues
 */
@Configuration
public class AppMessagingResourcesConfiguration extends MessagingResourcesConfiguration {

    public static final String SHIP_CONFIRM_RESOURCES = "ShipConfirmResources";
    public static final String SHIP_STATUS_RESOURCES = "ShipStatusResources";


    @Value("${sns.fmg.shipconfirm.topic}")
    private String shipConfirmTopicName;

    @Value("${sns.fmg.shipstatusupdates.topic}")
    private String shipStatusTopicName;



    @Value("${sqs.afssapshipconfirm.queue.name}")
    private String afsSapShipConfirmQueue;

    @Value("${sqs.afssapshipconfirm.dlq.queue.name}")
    private String afsSapShipConfirmEventDlqQueue;

    @Value("${sqs.afssapshipconfirm.cancel.queue.name}")
    private String cancelEventQueue;

    @Value("${sqs.afssapshipconfirm.cancel.dlq.name}")
    private String cancelEventDlq;

    @Value("${sqs.afssapadpter.queue.filterpolicy}")
    private String filterPolicy;


    @Bean(name = MESSAGING_RESOURCES)
    @Override
    public Map<String, MessagingResources> getMessagingResources() {
        return Stream
                .of(getShipConfirmMessagingResources(), getShipStatusMessagingResources())
                .collect(Collectors.toMap(MessagingResources::getResourcesName, messagingResources -> messagingResources));
    }

    private MessagingResources getShipConfirmMessagingResources() {
        return new MessagingResources(SHIP_CONFIRM_RESOURCES, shipConfirmTopicName,
                afsSapShipConfirmQueue, afsSapShipConfirmEventDlqQueue, filterPolicy);
    }

    private MessagingResources getShipStatusMessagingResources() {
        return new MessagingResources(SHIP_STATUS_RESOURCES, shipStatusTopicName,
                cancelEventQueue, cancelEventDlq, filterPolicy);
    }



}
