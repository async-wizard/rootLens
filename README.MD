# RootLens

AI-powered observability and root cause analysis platform for distributed systems.

RootLens ingests logs, traces, and metrics from microservices, correlates related events into incidents using distributed trace IDs, and generates AI root cause analysis via pluggable LLM providers (Groq, Claude, DeepSeek, OpenRouter). Results are served through a real-time React dashboard.

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [High Level Design](#high-level-design)
3. [Low Level Design](#low-level-design)
4. [Kafka Topics](#kafka-topics)
5. [Database Schema](#database-schema)
6. [REST API Reference](#rest-api-reference)
7. [LLM Provider Configuration](#llm-provider-configuration)
8. [Generating Incidents](#generating-incidents)

---

## Quick Start

### Prerequisites
- Docker + Docker Compose
- Java 17 (for local builds)
- Node.js 18+ (for the frontend)
- A free LLM API key — [Groq](https://console.groq.com) is easiest (no credit card)

### Run the stack

```bash
# 1. Export your LLM credentials (Groq example)
export GROQ_API_KEY=your-key-here
export AI_PROVIDER=groq

# 2. Start all backend services
cd infra
docker compose up -d

# 3. Start the frontend dev server
cd ../frontend
npm install
npm run dev
```

| Interface | URL |
|---|---|
| React Dashboard | http://localhost:5173 |
| Dashboard REST API | http://localhost:8086 |
| Kafka UI | http://localhost:8080 |
| Jaeger Traces | http://localhost:16686 |

### Generate incidents

```bash
# Trigger 10 checkout requests (some will fail and create incidents)
for i in {1..10}; do curl -s http://localhost:8081/checkout; done
```

The dashboard auto-polls every 30 seconds. New incidents appear with AI analysis within ~5 seconds of generation.

---

## High Level Design

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Simulation Layer                              │
│                                                                      │
│   ┌──────────────────┐        ┌──────────────────┐                  │
│   │  checkout-service│──────► │  payment-service  │                  │
│   │     :8081        │  HTTP  │      :8082        │                  │
│   └────────┬─────────┘        └────────┬──────────┘                 │
│            │  LogForwarder              │  LogForwarder              │
└────────────┼───────────────────────────┼────────────────────────────┘
             │ POST /logs                │ POST /logs
             ▼                           ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        Ingestion Layer                               │
│                                                                      │
│          ┌──────────────────────────────────┐                        │
│          │        ingestion-service          │                        │
│          │             :8085                 │                        │
│          │  /logs  /metrics  /traces /events │                        │
│          └──────────────────┬───────────────┘                        │
└─────────────────────────────┼───────────────────────────────────────┘
                              │ Publishes EnrichedEvent
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         Kafka (KRaft, :9092)                         │
│                                                                      │
│   logs-topic ──────────────────────────────────────────────────┐    │
│   metrics-topic ───────────────────────────────────────────────┤    │
│   traces-topic ────────────────────────────────────────────────┘    │
│                                                                      │
│   incidents-topic ──────────────────────────────────────────────┐   │
│   alerts-topic  (future alerting consumers)                      │   │
│   ai-analysis-topic (future downstream consumers)                │   │
└──────────────────────────────────────────────────────────────────┼──┘
          │ (logs/metrics/traces-topic)                            │
          ▼                                                         │
┌──────────────────────────────┐                                   │
│      incident-engine         │                                   │
│           :8083              │                                   │
│  • Correlation by traceId   │                                   │
│  • Deduplication via Redis  │──► incidents-topic                │
│  • Incident lifecycle mgmt  │                                   │
└──────────────────────────────┘                                   │
          │                                                         │ (incidents-topic)
          │                                                         ▼
          │                                          ┌──────────────────────────────┐
          │                                          │    ai-analysis-service       │
          │                                          │          :8084               │
          │                                          │  • Context aggregation       │
          │                                          │  • Prompt engineering        │
          │                                          │  • LLM provider strategy     │
          │                                          │  (Groq / Claude / DeepSeek)  │
          │                                          └──────────────┬───────────────┘
          │                                                         │
          ▼                                                         ▼
┌─────────────────────────────────────────────────────────────────────┐
│                          Storage Layer                               │
│                                                                      │
│          PostgreSQL :5432                Redis :6379                 │
│   ┌────────────────────┐        ┌───────────────────────┐           │
│   │ incidents          │        │ traceId → incidentId  │           │
│   │ incident_events    │        │ (correlation cache)   │           │
│   │ ai_analysis        │        └───────────────────────┘           │
│   └────────────────────┘                                            │
└─────────────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────────────┐
│                       Presentation Layer                             │
│                                                                      │
│   ┌──────────────────────┐      ┌─────────────────────────────┐     │
│   │   dashboard-backend  │      │       React Frontend         │     │
│   │        :8086         │◄─────│          :5173               │     │
│   │  REST API + caching  │      │  Incidents / Detail /        │     │
│   └──────────────────────┘      │  Services pages              │     │
│                                 └─────────────────────────────┘     │
└─────────────────────────────────────────────────────────────────────┘
```

### Data Flow

```
1. Simulation services call each other over HTTP with OpenTelemetry auto-instrumentation
   → same traceId propagated via W3C TraceContext headers

2. On each log/error event, LogForwarder POSTs a JSON payload to ingestion-service

3. ingestion-service enriches the event (adds host, ingestionSource, timestamp)
   → publishes EnrichedEvent to the appropriate Kafka topic

4. incident-engine consumes events, looks up traceId in Redis:
   - HIT  → event belongs to existing incident → append event record, update severity
   - MISS → new incident → generate INCIDENT-{traceIdPrefix} → write to Redis + DB

5. When an incident is created/updated, incident-engine publishes IncidentEvent
   to incidents-topic

6. ai-analysis-service consumes IncidentEvent:
   - Aggregates all incident_events from DB for context
   - Builds structured prompt with event timeline
   - Calls active LLM provider (Groq / Claude / DeepSeek / OpenRouter)
   - Parses JSON response: { summary, probableCause, remediation }
   - Persists AiAnalysis record, publishes to ai-analysis-topic

7. dashboard-backend serves REST API, reading incidents + analysis from PostgreSQL
   with Redis-backed response caching

8. React frontend polls every 30s, renders incident list, detail view, service health
```

### Technology Choices

| Concern | Choice | Reason |
|---|---|---|
| Event bus | Kafka (KRaft) | Durable, replayable, fan-out to multiple consumers |
| Correlation cache | Redis | Sub-millisecond traceId lookup, TTL-based expiry |
| Storage | PostgreSQL | ACID for incident state, queryable for dashboard |
| Tracing | OpenTelemetry + Jaeger | Standard, auto-instruments Spring Boot HTTP calls |
| AI | Pluggable strategy | Swap providers without code changes |
| Frontend | React + Tanstack Query | Real-time polling, typed API client |

---

## Low Level Design

### 1. Simulation Services

Both services are Spring Boot apps instrumented with the OpenTelemetry Java agent. They simulate realistic failure scenarios.

**checkout-service (:8081)**

```
CheckoutController
  └── POST /checkout
        ├── calls payment-service GET /payment via RestTemplate
        │     • success (70%) → logForwarder.sendInfo(...)
        │     • failure (30%) → logForwarder.sendError("Payment call failed: 500 ...")
        └── returns checkout result

LogForwarder
  └── send(service, severity, message)
        • reads current OTel Span → extracts traceId
        • builds JSON string manually (avoids RestTemplate Map serialization bug)
        • POST to ingestion-service /logs with Content-Type: application/json
```

**payment-service (:8082)**

```
PaymentController
  └── GET /payment
        ├── success (70%) → logForwarder.sendInfo("Payment processed")
        └── failure (30%) → logForwarder.sendError("Database timeout")
                          → throw 500 error (propagates to checkout-service)
```

Both services are containerized with the OTel agent mounted as a volume:
```
JAVA_TOOL_OPTIONS=-javaagent:/app/opentelemetry-javaagent.jar
OTEL_EXPORTER_OTLP_ENDPOINT=http://jaeger:4318
```

---

### 2. Ingestion Service (:8085)

Single responsibility: accept raw telemetry over HTTP, enrich it, and publish to Kafka.

```
Controllers (4)
  POST /logs    → LogController    ─┐
  POST /metrics → MetricController  ├─► EnrichmentService → KafkaTemplate → topic
  POST /traces  → TraceController   │
  POST /events  → EventController  ─┘

EnrichmentService
  └── enriches raw payload into EnrichedEvent:
        • eventType  (LOG_EVENT / METRIC_EVENT / TRACE_EVENT)
        • service
        • severity
        • traceId
        • message
        • originalTimestamp (from payload)
        • ingestionTimestamp (System.currentTimeMillis())
        • ingestionSource   (e.g. "http-log")
        • host              (from payload or "unknown")

KafkaProducerConfig
  └── explicit KafkaTemplate<String, EnrichedEvent> bean
        • key   = traceId (ensures ordering per trace)
        • value = EnrichedEvent (JSON, no type headers)
```

**EnrichedEvent schema (Kafka message value):**
```json
{
  "eventType": "LOG_EVENT",
  "service": "payment-service",
  "severity": "ERROR",
  "traceId": "340c4e0c0b292992f589d70a05b7e21c",
  "message": "Database timeout",
  "originalTimestamp": 1781421051,
  "ingestionTimestamp": 1781421052435,
  "ingestionSource": "http-log",
  "host": "unknown"
}
```

---

### 3. Incident Engine (:8083)

Stateful correlation service. Converts raw event streams into incidents.

```
IncidentEventListener
  @KafkaListener(topics = [logs-topic, metrics-topic, traces-topic])
  └── onEvent(EnrichedEvent)
        └── CorrelationService.correlate(event)

CorrelationService
  └── correlate(event):
        1. Validate traceId (reject "unknown" / blank)
        2. Redis GET "trace:{traceId}"
           HIT  → incidentId = cached value
                → load Incident from DB
                → update severity if escalating (LOW < MEDIUM < HIGH)
                → append IncidentEventRecord
                → save incident + event record
           MISS → generate incidentId = "INCIDENT-" + traceId.substring(0,6)
                → DeduplicationService checks DB (second guard)
                → create new Incident (status=OPEN)
                → Redis SET "trace:{traceId}" = incidentId (TTL: 5 min)
                → save Incident + IncidentEventRecord
                → publish IncidentEvent to incidents-topic
                → publish to alerts-topic

DeduplicationService
  └── Redis-backed idempotency guard (prevents duplicate incidents on replay)

IncidentEventRecord (saved per correlated event):
  incidentId, service, severity, traceId, message,
  originalTimestamp, receivedAt, ingestionSource, host
```

**Incident severity escalation:**
```
LOW → MEDIUM → HIGH   (never downgrades)
```

**Consumer group:** `incident-engine-group`  
**Error handling:** `DefaultErrorHandler` with `FixedBackOff(1000ms, 3 retries)` + DLT

---

### 4. AI Analysis Service (:8084)

Pluggable LLM orchestration service. Consumes incident events and produces structured root cause analysis.

```
IncidentEventListener
  @KafkaListener(topic = incidents-topic)
  └── onIncident(IncidentEvent)
        └── AiAnalysisOrchestrator.analyze(event)

AiAnalysisOrchestrator
  └── analyze(incident):
        1. Idempotency check (AiAnalysisRepository.findByIncidentId)
        2. Aggregate context: SELECT * FROM incident_events WHERE incident_id = ?
        3. PromptBuilderService.buildUserMessage(incident, events)
        4. LlmAnalysisProvider.analyze(systemPrompt, userMessage)  ← strategy pattern
        5. Parse JSON response: { summary, probableCause, remediation }
        6. Persist AiAnalysis entity
        7. AiAnalysisPublisherService.publish(analysis) → ai-analysis-topic

LlmAnalysisProvider  (interface)
  ├── ClaudeAnalysisService    → Anthropic Java SDK (claude-sonnet-4-6)
  └── OpenAiCompatibleProvider → RestClient to OpenAI-compatible APIs
        ├── Groq     (https://api.groq.com/openai/v1)     llama-3.3-70b-versatile
        ├── DeepSeek (https://api.deepseek.com/v1)        deepseek-chat
        └── OpenRouter (https://openrouter.ai/api/v1)     meta-llama/llama-3.1-8b-instruct:free

LlmProviderConfig
  └── reads AI_PROVIDER env var → instantiates correct strategy bean

PromptBuilderService
  └── SYSTEM_PROMPT: instructs model to respond with JSON only:
        { "summary": "...", "probableCause": "...", "remediation": "..." }
  └── userMessage: incident severity + services + timeline of events with timestamps
```

**LLM response contract (expected JSON):**
```json
{
  "summary": "One paragraph describing what happened",
  "probableCause": "Root cause explanation",
  "remediation": "Actionable steps to resolve and prevent recurrence"
}
```

**Consumer group:** `ai-analysis-group`  
**Error handling:** LLM failures caught in orchestrator, stored as `success=false` analysis (no DLT noise)

---

### 5. Dashboard Backend (:8086)

Read-only REST API with Redis caching. No business logic — aggregates data from PostgreSQL for the frontend.

```
REST Endpoints:
  GET  /incidents?page=0&size=20     → paginated IncidentSummaryDto list
  GET  /incidents/{id}               → IncidentDetailDto (events + analysis inline)
  GET  /incidents/{id}/analysis      → AnalysisDto only
  GET  /services                     → ServiceHealthDto list

IncidentDetailDto:
  incidentId, status, severity, servicesImpacted, traceIds,
  createdAt, updatedAt,
  events: [ { service, severity, traceId, message, originalTimestamp, receivedAt } ],
  analysis: { summary, probableCause, remediation, model, analysisTimestamp, success }

ServiceHealthDto:
  name, activeIncidents, highestSeverity, lastEventAt

Caching:
  @Cacheable("incidents")  → Redis, TTL 30s
  @Cacheable("services")   → Redis, TTL 30s
  CORS: allowedOrigins("*") for local dev
```

---

### 6. React Frontend (:5173)

Three-page SPA. Polls the dashboard backend every 30 seconds.

```
src/
├── api/client.ts          fetchIncidents(page, size)
│                          fetchIncidentDetail(id)
│                          fetchServices()
├── types/index.ts         IncidentSummary, IncidentDetail, Analysis, ServiceHealth
├── pages/
│   ├── IncidentsPage.tsx  paginated table, severity + status badges, AI indicator
│   ├── IncidentDetailPage.tsx  event timeline + AI analysis card
│   └── ServicesPage.tsx   service health card grid
└── components/
    ├── Navbar.tsx          brand + nav links + last-updated counter
    ├── SeverityBadge.tsx   HIGH=red, MEDIUM=orange, LOW=gray
    └── StatusBadge.tsx     OPEN=blue, INVESTIGATING=yellow, RESOLVED=green

Stack: Vite + React 19 + TypeScript + Tailwind CSS v4 + shadcn/ui
Query: @tanstack/react-query  refetchInterval: 30_000  staleTime: 0
Routing: react-router-dom v7
```

---

## Kafka Topics

| Topic | Producer | Consumer(s) | Message Type |
|---|---|---|---|
| `logs-topic` | ingestion-service | incident-engine | `EnrichedEvent` |
| `metrics-topic` | ingestion-service | incident-engine | `EnrichedEvent` |
| `traces-topic` | ingestion-service | incident-engine | `EnrichedEvent` |
| `incidents-topic` | incident-engine | ai-analysis-service | `IncidentEvent` |
| `alerts-topic` | incident-engine | *(future alerting)* | `IncidentEvent` |
| `ai-analysis-topic` | ai-analysis-service | *(future consumers)* | `AiAnalysisEvent` |
| `incidents-topic.DLT` | Spring Kafka error handler | *(manual review)* | raw bytes |

**Consumer groups:**
- `incident-engine-group` → subscribes to logs/metrics/traces topics
- `ai-analysis-group` → subscribes to incidents-topic

**Kafka config (KRaft single-broker):**
```yaml
KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
KAFKA_MIN_INSYNC_REPLICAS: 1
```

---

## Database Schema

```sql
-- Incidents created by incident-engine
CREATE TABLE incidents (
    id                VARCHAR(50) PRIMARY KEY,   -- "INCIDENT-{traceIdPrefix}"
    status            VARCHAR(30) NOT NULL,       -- OPEN | INVESTIGATING | RESOLVED
    severity          VARCHAR(20) NOT NULL,       -- LOW | MEDIUM | HIGH
    services_impacted TEXT,                       -- JSON array, stored as string
    trace_ids         TEXT,                       -- JSON array, stored as string
    created_at        BIGINT NOT NULL,            -- epoch ms
    updated_at        BIGINT NOT NULL
);

-- Individual events correlated to each incident
CREATE TABLE incident_events (
    id                 BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    incident_id        VARCHAR(50) NOT NULL,      -- FK → incidents.id
    service            VARCHAR(100),
    severity           VARCHAR(20),
    trace_id           VARCHAR(64),
    message            TEXT,
    original_timestamp BIGINT,                    -- epoch seconds (from source)
    received_at        BIGINT,                    -- epoch ms (ingestion time)
    ingestion_source   VARCHAR(50),
    host               VARCHAR(100)
);

-- AI analysis result per incident
CREATE TABLE ai_analysis (
    id                 VARCHAR(100) PRIMARY KEY,  -- "ANALYSIS-{incidentId}"
    incident_id        VARCHAR(50) NOT NULL UNIQUE,
    summary            TEXT,
    probable_cause     TEXT,
    remediation        TEXT,
    model              VARCHAR(100),              -- e.g. "Groq/llama-3.3-70b-versatile"
    analysis_timestamp BIGINT,                    -- epoch ms
    success            BOOLEAN,
    raw_response       TEXT                       -- full LLM output for debugging
);
```

---

## REST API Reference

### GET /incidents

Returns paginated list of incidents.

**Query params:** `page` (default 0), `size` (default 20)

```json
{
  "content": [
    {
      "incidentId": "INCIDENT-ecaff5",
      "status": "OPEN",
      "severity": "HIGH",
      "servicesImpacted": ["payment-service", "checkout-service"],
      "createdAt": 1781421052435,
      "updatedAt": 1781421052738,
      "hasAnalysis": true
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 2,
  "totalPages": 1
}
```

### GET /incidents/{id}

Returns full incident detail including correlated events and AI analysis.

```json
{
  "incidentId": "INCIDENT-ecaff5",
  "status": "OPEN",
  "severity": "HIGH",
  "servicesImpacted": ["payment-service", "checkout-service"],
  "traceIds": ["340c4e0c0b292992f589d70a05b7e21c"],
  "createdAt": 1781421052435,
  "updatedAt": 1781421052738,
  "events": [
    {
      "service": "payment-service",
      "severity": "ERROR",
      "traceId": "340c4e0c0b292992f589d70a05b7e21c",
      "message": "Database timeout",
      "originalTimestamp": 1781421051,
      "receivedAt": 1781421052572
    },
    {
      "service": "checkout-service",
      "severity": "ERROR",
      "traceId": "340c4e0c0b292992f589d70a05b7e21c",
      "message": "Payment call failed: 500",
      "originalTimestamp": 1781421051,
      "receivedAt": 1781421053340
    }
  ],
  "analysis": {
    "summary": "The payment-service experienced a database timeout which cascaded to checkout-service",
    "probableCause": "Database connection or query issue with payment-service",
    "remediation": "Investigate DB logs, increase connection pool size, add circuit breakers",
    "model": "Groq/llama-3.3-70b-versatile",
    "analysisTimestamp": 1781429029810,
    "success": true
  }
}
```

### GET /services

Returns per-service health aggregated from active incidents.

```json
{
  "services": [
    {
      "name": "payment-service",
      "activeIncidents": 2,
      "highestSeverity": "HIGH",
      "lastEventAt": 1781421053340
    }
  ]
}
```

---

## LLM Provider Configuration

Set environment variables before starting the stack:

```bash
# Groq (free tier — recommended for development)
export AI_PROVIDER=groq
export GROQ_API_KEY=gsk_...

# Anthropic Claude
export AI_PROVIDER=claude
export ANTHROPIC_API_KEY=sk-ant-...

# DeepSeek
export AI_PROVIDER=deepseek
export DEEPSEEK_API_KEY=...

# OpenRouter (has free models)
export AI_PROVIDER=openrouter
export OPENROUTER_API_KEY=sk-or-...
```

Default models per provider:

| Provider | Default Model |
|---|---|
| Groq | `llama-3.3-70b-versatile` |
| Claude | `claude-sonnet-4-6` |
| DeepSeek | `deepseek-chat` |
| OpenRouter | `meta-llama/llama-3.1-8b-instruct:free` |

---

## Generating Incidents

The checkout-service has a built-in 30% failure rate that triggers correlated errors across both services with the same OpenTelemetry trace ID.

```bash
# Generate a batch of requests (produces ~3 incidents per 10 requests)
for i in {1..10}; do curl -s http://localhost:8081/checkout; done

# Watch incidents appear in real time
watch -n 5 'curl -s http://localhost:8086/incidents | python3 -m json.tool | grep -E "incidentId|severity|hasAnalysis"'

# Reset consumer offset to re-analyze existing incidents with a different provider
docker compose stop ai-analysis-service
docker exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group ai-analysis-group \
  --reset-offsets --to-earliest \
  --topic incidents-topic --execute
export AI_PROVIDER=groq GROQ_API_KEY=...
docker compose up -d ai-analysis-service

# View distributed traces in Jaeger
open http://localhost:16686  # search for "checkout-service"
```
