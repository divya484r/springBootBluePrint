package com.sample.springbootsampleapp.configuration;



import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.ReadMode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@Slf4j
@EnableCaching
public class ElastiCacheConfiguration {

    @Value("${redis.endpoint}")
    private String redisEndpoint;

    private RedissonClient getElastiCacheClient() {
        log.info("Connecting to  the  Redis: {}", redisEndpoint);
        Config config = new Config();
        config.useReplicatedServers()
                .setScanInterval(2000)
                .addNodeAddress(redisEndpoint)
                .setReadMode(ReadMode.SLAVE);
       return Redisson.create(config);
    }

    @Bean
    @Qualifier("productEnrichmentCache")
    @Primary
    public RMapCache<String, String> productEnrichmentCache() {
        return getElastiCacheClient().getMapCache("productEnrichmentCache");
    }

}

