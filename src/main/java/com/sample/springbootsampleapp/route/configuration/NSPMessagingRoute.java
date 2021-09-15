package com.sample.springbootsampleapp.route.configuration;

import com.sample.springbootsampleapp.util.ApplicationConstants;
import com.sample.springbootsampleapp.util.DistributedTraceProcessor;
import com.sample.springbootsampleapp.util.ExceptionLoggingProcessor;
import com.sample.routeconfigs.exception.ExceptionHandlerRouteBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.LoggingLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import static com.sample.routeconfigs.common.RouteConstants.UTF_8;

@Slf4j
@Configuration
public class NSPMessagingRoute extends ExceptionHandlerRouteBuilder {

    @Value("${sqs.afssapshipconfirm.nsp.queue.name}")
    private String nspQueue;

    @Value("${camel.maxRedeliveryCount:5}")
    private int maxRedeliveryCount;

    @Value("${camel.redeliveryDelayMs:2000}")
    private long redeliveryDelayMs;

    @Value("${sqs.no.consumers}")
    private int numberOfConsumers;

    @Value("${sqs.max.no.messages}")
    private int maxNumberOfMessage;

    @Override
    public void configure() throws Exception {

        onException(Exception.class).maximumRedeliveries(maxRedeliveryCount).redeliveryDelay(redeliveryDelayMs)
                .redeliveryDelay(redeliveryDelayMs).retryAttemptedLogLevel(LoggingLevel.INFO)
                .log(LoggingLevel.ERROR,
                        "ErrorType=GeneralException ErrorMsg=Exception occurred in springbootsampleapp while processing the request for id = ${property."
                                + ApplicationConstants.MESSAGE_ID + "}, moving message to DLQ")
                .handled(false)
                .useOriginalMessage()
                .bean(ExceptionLoggingProcessor.class);

        String fromUri = "wingtips.aws-sqs://" + nspQueue + "?amazonSQSClient=#amazonSQSClient"
                + "&concurrentConsumers=" + numberOfConsumers + "&maxMessagesPerPoll=" + maxNumberOfMessage
                + "&messageAttributeNames=All" + "&deleteAfterRead=true";

        from(fromUri).routeId(ApplicationConstants.NSP_ROUTE_ID)
                .routeDescription(ApplicationConstants.NSP_ROUTE_DESCRIPTION)
                .bean(DistributedTraceProcessor.class)
                .log(LoggingLevel.INFO, "Starting the Event=" + ApplicationConstants.NSP_ROUTE_ID + "  for springbootsampleapp repo")
                .log(LoggingLevel.INFO, "Event=NspRoute Status=Started Message=SQS Message received = ${body}")
                .setHeader(ApplicationConstants.MESSAGE_ID, xpath("/shipment/messageID", String.class))
                .convertBodyTo(String.class, UTF_8)


                .log(LoggingLevel.INFO, "Event=NspRoute Status=Completed id = ${property." + ApplicationConstants.MESSAGE_ID + "}")
                .log(LoggingLevel.INFO, "Complete the Event=" + ApplicationConstants.NSP_ROUTE_ID + "  for springbootsampleapp repo")

                .end();
    }
}
