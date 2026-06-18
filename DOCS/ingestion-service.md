# Ingestion Service

## What it is

The ingestion service is the entry point into rootLens. Every observability signal (log, trace, metric, custom event) emitted by any simulation service first lands here before entering the platform.

```
checkout-service  ──┐
                    ├──▶  ingestion-service  ──▶  Kafka
payment-service   ──┘
```

---

## Request Flow

```
POST /logs  (from simulation service)
     │
     ▼
LogController          ← validates the request (@Valid)
     │
     ▼
IngestionService       ← enriches the payload, picks the topic
     │
     ▼
KafkaTemplate          ← publishes async (fire-and-forget)
     │
     ▼
logs-topic             ← incident engine will consume from here later
```

The controller returns `202 Accepted` immediately — it does not wait for Kafka to confirm.
The HTTP caller (simulation service) is never blocked by Kafka latency or downtime.

---

## Endpoints

| Endpoint | Topic | Active |
|---|---|---|
| `POST /logs` | `logs-topic` | Yes — called by `LogForwarder` in both simulation services |
| `POST /traces` | `traces-topic` | Reserved for future trace span forwarding |
| `POST /metrics` | `metrics-topic` | Reserved for future metrics |
| `POST /events` | `events-topic` | Generic catch-all for custom platform events |

---

## Event Enrichment

Simulation services send a minimal payload:

```json
{
  "service": "payment-service",
  "severity": "ERROR",
  "traceId": "abc123...",
  "message": "Database timeout",
  "timestamp": 1717000000
}
```

The ingestion service wraps this into an `EnrichedEvent` that adds:

| Field | Value | Purpose |
|---|---|---|
| `ingestionTimestamp` | epoch ms at arrival | Measure ingestion lag vs original event time |
| `host` | container hostname | Identify which replica processed the event when scaled horizontally |
| `ingestionSource` | `"http-api"` | Future-proofs for other input methods (Kafka mirror, agent push, etc.) |
| `topic` | e.g. `"logs-topic"` | Self-describing — useful for debugging downstream consumers |

---

## Kafka Topic Auto-Creation

`KafkaTopicConfig` declares `NewTopic` Spring beans. On startup, Spring's built-in `KafkaAdmin`
calls `AdminClient.createTopics()` — creates the 4 topics if they don't exist, silently ignores
them if they already do. No init container or shell scripts needed.

| Topic | Partitions | Replicas |
|---|---|---|
| `logs-topic` | 3 | 1 |
| `traces-topic` | 3 | 1 |
| `metrics-topic` | 3 | 1 |
| `events-topic` | 3 | 1 |

3 partitions prepares for future horizontal scaling of consumers (incident engine).
1 replica matches the single-broker KRaft setup.

---

## Design Decisions

**Partition key = service name**
All events from `payment-service` land on the same partition. When the incident engine
consumes, it receives events from each service in order.

**`fail-fast: false` on Kafka admin**
Topic creation failure at startup does not crash the service. It retries. This makes
the startup sequence resilient to Kafka not being fully ready.

**`spring.json.add.type.headers: false`**
The `JsonSerializer` normally stamps a `__TypeId__` header with the producer's Java class name.
Disabled so the incident engine is not coupled to the ingestion service's internal type system.

**Validation at the boundary**
`@NotBlank` / `@NotNull` annotations on DTOs reject malformed payloads with a `400 Bad Request`
before any Kafka interaction.

**202 Accepted vs 200 OK**
Returns `202` because the event is accepted for async processing, not guaranteed to be persisted.
This correctly sets caller expectations.

---

## Package Structure

```
services/ingestion-service/
└── src/main/java/com/rootlens/ingestion/
    ├── IngestionServiceApplication.java
    ├── config/
    │   └── KafkaTopicConfig.java       ← NewTopic beans, auto-creates topics on startup
    ├── controller/
    │   ├── LogController.java          ← POST /logs
    │   ├── TraceController.java        ← POST /traces
    │   ├── MetricController.java       ← POST /metrics
    │   └── EventController.java        ← POST /events
    ├── dto/
    │   ├── LogEventRequest.java        ← validated inbound schema for logs
    │   ├── TraceEventRequest.java
    │   ├── MetricEventRequest.java
    │   └── GenericEventRequest.java    ← catch-all for /events
    ├── model/
    │   └── EnrichedEvent.java          ← enriched outbound schema published to Kafka
    ├── service/
    │   └── IngestionService.java       ← enrichment + Kafka publish logic
    └── exception/
        └── GlobalExceptionHandler.java ← 400 for validation errors, 500 for unexpected
```

---

## Configuration

```yaml
server:
  port: 8085

spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      acks: 1           # leader acknowledges before replica sync — balance of durability vs latency
      retries: 3
      properties:
        retry.backoff.ms: 500
        linger.ms: 5    # micro-batching at low throughput
    admin:
      fail-fast: false
```

---

## Port

`8085` — leaves 8083/8084 open for the incident engine and AI analysis service.
