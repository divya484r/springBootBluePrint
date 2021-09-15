package com.sample.springbootsampleapp.route.configuration;

import com.sample.springbootsampleapp.model.FulfillmentStatus;
import com.sample.springbootsampleapp.util.ApplicationConstants;
import com.sample.springbootsampleapp.util.ExceptionLoggingProcessor;
import com.sample.springbootsampleapp.util.DistributedTraceProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.xml.bind.JAXBContext;
import static com.sample.routeconfigs.common.RouteConstants.UTF_8;

@Slf4j
@Component
public class ShipStatusRoute extends RouteBuilder {

    @Value("${camel.maxRedeliveryCount:5}")
    private int maxRedeliveryCount;

    @Value("${camel.redeliveryDelayMs:2000}")
    private long redeliveryDelayMs;

    @Override
    public void configure() throws Exception {
        // XML Data Format
        JaxbDataFormat xmlDataFormat = new JaxbDataFormat();
        JAXBContext con = JAXBContext.newInstance(FulfillmentStatus.class);
        xmlDataFormat.setContext(con);

        onException(Exception.class).maximumRedeliveries(maxRedeliveryCount).redeliveryDelay(redeliveryDelayMs)
                .redeliveryDelay(redeliveryDelayMs).retryAttemptedLogLevel(LoggingLevel.INFO)
                .log(LoggingLevel.ERROR,
                        "ErrorType=GeneralException ErrorMsg=Exception occurred in springbootsampleapp while processing the request for id = ${property."
                                + ApplicationConstants.MESSAGE_ID + "}, moving message to DLQ")
                .handled(false)
                .useOriginalMessage()
                .bean(ExceptionLoggingProcessor.class);



        from(ApplicationConstants.SHIP_CANCEL_ROUTER)
                .routeId(ApplicationConstants.SHIP_CANCEL_ROUTE_ID)
                .routeDescription(ApplicationConstants.SHIP_CANCEL_ROUTE_DESCRIPTION)
                .bean(DistributedTraceProcessor.class)
                .log(LoggingLevel.INFO, "Starting the Event=" + ApplicationConstants.SHIP_CANCEL_ROUTE_ID + "  for springbootsampleapp repo")
                .log(LoggingLevel.INFO, "Event=ShipStatus Status=Started Message=SQS Message received for ShipStatus Events = ${body}")
                .setProperty(ApplicationConstants.MESSAGE_ID, xpath("/fulfillmentStatus/messageID", String.class))
                .convertBodyTo(String.class, UTF_8)
                .unmarshal(xmlDataFormat)

                .log(LoggingLevel.INFO, "Event=ShipStatus Status=Completed id = ${property." + ApplicationConstants.MESSAGE_ID + "}")
                .log(LoggingLevel.INFO, "Complete the Event=" + ApplicationConstants.SHIP_CANCEL_ROUTE_ID + "  for springbootsampleapp repo")

                .end();
    }
}
