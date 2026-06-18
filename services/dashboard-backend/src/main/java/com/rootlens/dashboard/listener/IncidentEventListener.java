package com.rootlens.dashboard.listener;

import com.rootlens.dashboard.sse.SseEmitterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import org.springframework.cache.Cache;

import java.util.Optional;

@Component
public class IncidentEventListener {

    private static final Logger log = LoggerFactory.getLogger(IncidentEventListener.class);

    private final SseEmitterRegistry sseEmitterRegistry;
    private final CacheManager cacheManager;

    public IncidentEventListener(SseEmitterRegistry sseEmitterRegistry, CacheManager cacheManager) {
        this.sseEmitterRegistry = sseEmitterRegistry;
        this.cacheManager = cacheManager;
    }

    @KafkaListener(
        topics = {"incidents-topic", "ai-analysis-topic"},
        groupId = "dashboard-sse-group",
        containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void onEvent(String rawJson) {
        log.debug("SSE broadcast triggered by Kafka event");
        sseEmitterRegistry.broadcast("incident-update", rawJson);

        // Evict caches so the next REST poll returns fresh data
        evict("incidents-list");
        evict("incident-detail");
        evict("services-health");
    }

    private void evict(String cacheName) {
        Optional.ofNullable(cacheManager.getCache(cacheName))
                .ifPresent(Cache::clear);
    }
}
