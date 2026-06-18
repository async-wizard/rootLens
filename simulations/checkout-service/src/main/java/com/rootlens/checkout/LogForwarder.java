package com.rootlens.checkout;

import io.opentelemetry.api.trace.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class LogForwarder {

    private static final Logger log = LoggerFactory.getLogger(LogForwarder.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${rootlens.ingestion.url:}")
    private String ingestionUrl;

    public void sendError(String service, String message) {
        log.error("[{}] {}", service, message);
        send(service, "ERROR", message);
    }

    public void sendWarn(String service, String message) {
        log.warn("[{}] {}", service, message);
        send(service, "WARN", message);
    }

    public void sendInfo(String service, String message) {
        log.info("[{}] {}", service, message);
        send(service, "INFO", message);
    }

    private void send(String service, String severity, String message) {
        if (ingestionUrl.isBlank()) return;

        Span currentSpan = Span.current();
        String traceId = currentSpan.getSpanContext().isValid()
                ? currentSpan.getSpanContext().getTraceId()
                : "unknown";

        String json = "{\"eventType\":\"LOG_EVENT\""
                + ",\"service\":\"" + esc(service) + "\""
                + ",\"severity\":\"" + severity + "\""
                + ",\"traceId\":\"" + esc(traceId) + "\""
                + ",\"message\":\"" + esc(message) + "\""
                + ",\"timestamp\":" + (System.currentTimeMillis() / 1000)
                + "}";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForObject(ingestionUrl + "/logs", new HttpEntity<>(json, headers), String.class);
        } catch (Exception e) {
            log.warn("Ingestion service unreachable: {}", e.getMessage());
        }
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
