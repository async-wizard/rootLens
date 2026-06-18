package com.rootlens.dashboard.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${rootlens.dashboard.cache.incidents-list-ttl-seconds}")
    private long listTtl;

    @Value("${rootlens.dashboard.cache.incident-detail-ttl-seconds}")
    private long detailTtl;

    @Value("${rootlens.dashboard.cache.services-ttl-seconds}")
    private long servicesTtl;

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        Map<String, RedisCacheConfiguration> configs = Map.of(
                "incidents-list",  base.entryTtl(Duration.ofSeconds(listTtl)),
                "incident-detail", base.entryTtl(Duration.ofSeconds(detailTtl)),
                "services-health", base.entryTtl(Duration.ofSeconds(servicesTtl))
        );

        return RedisCacheManager.builder(factory)
                .withInitialCacheConfigurations(configs)
                .build();
    }
}
