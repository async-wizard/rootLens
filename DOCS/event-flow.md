### 1. Purpose 
This document defines
* how events move through RootLens
* service interactions
* Kafka topic flow 
* incident lifecycle propagation
* retry behaviour 
* trace propagation
* eventual consistency behaviour 

Step 1 - Service Emits Error Logs
The stimulated payment-service emits: 
{
    "service" : "payment-service" 
    severity : "ERROR" 
    "traceId" : "trace abc 123" 
    "message" : "Databse connection timeout" 
    "timestamp" : 1712345678
}

Step 2: Event Sent to Ingestion Service
The service calls: 
POST /logs

The ingestion service: 
* validates payload
* enriches metadata
* assignes ingestion timestamp
* publishes to kafka

Step 3: Kafka Receives LOG-EVENT
published to: 
logs-topic
Event type: 
LOG_EVENT

Step 4 - Incident Engine Consumes Event
The incident Engine consumes: 
logs-topic
Processing includes: 
* correlation checks 
* deduplication
* severity evaluation
* incident matching 

Step 5 - Correlation Engine Groups Related Failures
Additional Failures arrive: 
* checkout service retries failures 
* gateway service latency spike 

Step 6 - Incident Created
The incident Engine publishes: 
INCIDENT_CREATED
to: 
incidnets-topic
{
  "incidentId": "INCIDENT-142",
  "severity": "HIGH",
  "servicesImpacted": [
    "payment-service",
    "checkout-service"
  ],
  "status": "OPEN",
  "traceIds": [
    "trace-abc-123"
  ]
}

Step 7 - AI Analysis Triggered
The AI Service consumes: 
incidents topic

Workflow: 
1. gather related logs
2. fetch traces
3. aggregate context
4. generate prompt
5. invoke LLM
6. persist results

Step 8 -- AI Analysis Published
AI_ANALYSIS_COMPLETED
to: 
ai-analysis-topic

Example: 
{
  "incidentId": "INCIDENT-142",
  "severity": "HIGH",
  "servicesImpacted": [
    "payment-service",
    "checkout-service"
  ],
  "status": "OPEN",
  "traceIds": [
    "trace-abc-123"
  ]
}

Step 9 -- Dashboard Updated
Dashboard backend
* queries PostgreSQL
* fetches AI analysis
* refreshes dashboard cache
* updates UI


4. Event Types
4.1 LOG_EVENT
Represents service log emissions
Example fields: 
{
  "eventType": "LOG_EVENT",
  "service": "payment-service",
  "severity": "ERROR",
  "traceId": "trace-123",
  "message": "Database timeout",
  "timestamp": 1712345678
}

4.2 Trace_Event
Represents distributed tracing spans: 
Example Fields: 
{
  "eventType": "LOG_EVENT",
  "service": "payment-service",
  "severity": "ERROR",
  "traceId": "trace-123",
  "message": "Database timeout",
  "timestamp": 1712345678
}
4.3 Incident_Created 
4.4 Incident_Updated 
Represnets lifecycle trasistions: 
Examples: 
serverity escalation
status changes
impacted service expansion

4.5 AI_ANALYSIS_COMPLETED

5. Kafka Topic Flow
Topic Ownership
| Topic             | Producer            | Consumers           |
| ----------------- | ------------------- | ------------------- |
| logs-topic        | ingestion-service   | incident-engine     |
| traces-topic      | ingestion-service   | incident-engine     |
| metrics-topic     | ingestion-service   | incident-engine     |
| incidents-topic   | incident-engine     | ai-analysis-service |
| ai-analysis-topic | ai-analysis-service | dashboard-backend   |

6. Correlation Logic 
