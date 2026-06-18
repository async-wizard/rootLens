package com.rootlens.incidentengine.service;

import com.rootlens.incidentengine.entity.Incident;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WebhookNotificationService {

    private static final Logger log = LoggerFactory.getLogger(WebhookNotificationService.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${rootlens.alert.webhook-url:}")
    private String webhookUrl;

    public void notify(Incident incident) {
        if (webhookUrl.isBlank()) return;

        String color    = "HIGH".equals(incident.getSeverity()) ? "danger" : "warning";
        String services = String.join(", ", incident.getServicesImpacted());

        // Slack-compatible incoming webhook format — also works as generic JSON webhook
        String payload = String.format(
                "{\"attachments\":[{\"color\":\"%s\","
                + "\"title\":\"\\uD83D\\uDEA8 Incident %s\","
                + "\"text\":\"Severity: *%s* | Services: %s | Status: %s\","
                + "\"footer\":\"rootLens\",\"ts\":%d}]}",
                color,
                esc(incident.getId()),
                esc(incident.getSeverity()),
                esc(services),
                esc(incident.getStatus()),
                System.currentTimeMillis() / 1000);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForObject(webhookUrl, new HttpEntity<>(payload, headers), String.class);
            log.info("Webhook notified for incident {} (severity={})",
                    incident.getId(), incident.getSeverity());
        } catch (Exception e) {
            log.warn("Webhook delivery failed for {}: {}", incident.getId(), e.getMessage());
        }
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
