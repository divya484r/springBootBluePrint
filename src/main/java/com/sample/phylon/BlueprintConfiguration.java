package com.sample.phylon;

import com.netflix.appinfo.AmazonInfo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


/**
 * Spring configuration class that provides custom configuration of common
 * beans.
 */
@Configuration
@Component
public class BlueprintConfiguration {

    @Value("${min.thread.count}")
    private int minThreadCount;

    @Value("${max.thread.count}")
    private int maxThreadCount;

    @Value("${idle.time.seconds}")
    private int timeToLive;

    @Bean
    public EurekaInstanceConfigBean eurekaInstanceConfigBean() {
        EurekaInstanceConfigBean bean = new EurekaInstanceConfigBean(new InetUtils(new InetUtilsProperties()));
        AmazonInfo dataCenter = AmazonInfo.Builder.newBuilder().autoBuild("eureka");
        bean.setDataCenterInfo(dataCenter);
        return bean;
    }

    @Value("${camel.route.Rest.in.uri:direct:RestPost}")
    private String endpointInUri;


    public String getEndpointInUri() {
        return endpointInUri;
    }

    public void setEndpointInUri(String endpointInUri) {
        this.endpointInUri = endpointInUri;
    }

    @Value("${info.app.name:sampleApp}")
    private String appName;


    @LoadBalanced
    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
