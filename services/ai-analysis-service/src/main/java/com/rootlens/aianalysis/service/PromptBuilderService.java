package com.rootlens.aianalysis.service;

import com.rootlens.aianalysis.entity.IncidentEventRecord;
import com.rootlens.aianalysis.model.IncidentEvent;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PromptBuilderService {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    public static final String SYSTEM_PROMPT =
            "You are an expert SRE analyzing a production incident. " +
            "Respond only with valid JSON — no markdown, no explanation outside the JSON object.";

    public String buildUserMessage(IncidentEvent incident, List<IncidentEventRecord> eventRecords) {
        StringBuilder sb = new StringBuilder();
        sb.append("Analyze this incident and provide root cause analysis.\n\n");

        sb.append("INCIDENT:\n");
        sb.append("- ID: ").append(incident.getIncidentId()).append("\n");
        sb.append("- Severity: ").append(incident.getSeverity()).append("\n");
        sb.append("- Status: ").append(incident.getStatus()).append("\n");
        sb.append("- Services impacted: ")
                .append(String.join(", ", incident.getServicesImpacted())).append("\n");

        if (incident.getCreatedAt() != null) {
            String detectedAt = FORMATTER.format(Instant.ofEpochMilli(incident.getCreatedAt()));
            sb.append("- Detected at: ").append(detectedAt).append(" UTC\n");
        }

        sb.append("\nERROR EVENTS (").append(eventRecords.size()).append(" total):\n");
        for (int i = 0; i < eventRecords.size(); i++) {
            IncidentEventRecord rec = eventRecords.get(i);
            String ts = rec.getReceivedAt() != null
                    ? FORMATTER.format(Instant.ofEpochMilli(rec.getReceivedAt()))
                    : "unknown";
            sb.append(i + 1).append(". [")
                    .append(rec.getService()).append(" | ").append(ts).append("] ")
                    .append("\"").append(rec.getMessage()).append("\"\n");
        }

        sb.append("\nRespond with exactly this JSON structure:\n");
        sb.append("{\n");
        sb.append("  \"summary\": \"<one-paragraph description of what happened>\",\n");
        sb.append("  \"probableCause\": \"<the most likely root cause>\",\n");
        sb.append("  \"remediation\": \"<specific actionable steps to resolve this>\"\n");
        sb.append("}");

        return sb.toString();
    }
}
