package com.rootlens.incidentengine.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class DeduplicationService {

    private final StringRedisTemplate redisTemplate;

    @Value("${rootlens.incident.dedup-ttl-seconds}")
    private long dedupTtlSeconds;

    @Value("${rootlens.incident.trace-ttl-seconds}")
    private long traceTtlSeconds;

    public DeduplicationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Optional<String> getTraceIncidentId(String traceId) {
        String value = redisTemplate.opsForValue().get("trace:" + traceId);
        return Optional.ofNullable(value);
    }

    public Optional<String> getDedupIncidentId(String service, String fingerprint) {
        String value = redisTemplate.opsForValue().get("dedup:" + service + ":" + fingerprint);
        return Optional.ofNullable(value);
    }

    public void storeTrace(String traceId, String incidentId) {
        redisTemplate.opsForValue().set("trace:" + traceId, incidentId, traceTtlSeconds, TimeUnit.SECONDS);
    }

    public void storeDedup(String service, String fingerprint, String incidentId) {
        redisTemplate.opsForValue().set("dedup:" + service + ":" + fingerprint, incidentId, dedupTtlSeconds, TimeUnit.SECONDS);
    }
}
