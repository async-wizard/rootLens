1. Executive Summary
### Product Name
RootLens

### Overview
RootLens is an Ai powered observabilty and root cause analysic platform designed for the distributed systems. 

The platform ingests logs, traces, metrics, and service events from distributed microservices, correlates related failures into incidents and generates AI assisted root cause insights for engineers. 

RootLens aims to reduce incident investigation time by providing: 
* real time event ingestion
* incident correlation
* distributed tracing
* dependency visualization
* AI generated summaries and remediation suggestions

The system is built using an event-driven architecture kafka, Redis, PostgreSQL and Spring Boot services. 

2. Problem Statement
Modern distributed systems are difficult to debug because: 
* logs are fragmented across services
* failures cascade across dependencies
* engineers experience alert fatigue
* root cause analysis is manual and slow
* service dependencies are often unclear
* incident context is scattered acroos tooling


Existing observability systems provide monitoring and alerting, but still spend significant time correlating events and indentifying probable root cause. 

Rootllens aims to centralize signals and automate incident understanding using AI assisted analysis and event corr

3. Goals 
### Primary Goals
G1 - Centralized Observabilty
Provide a unified platform for ingestion
* logs
* traces
* metrices
* system events



G2: Incident Correlation
Automatically group related failures into logical incidents. 
Example: 
* Payment service checkout
* checkout retries failing
* DB latency spk

g3: AI assisted Root Cause Analysis
Generate: 
* incident summaries
* probable cause
* remediation suggestions

G4: Service Dependency Visibility
Provide visibilty into
* upstream/downstream dependencies
* impacted services 
* event propagation paths

G5: Event Driven Scalabilty
Use asynchronous kafka based communication to support
* decoupled services 
* replayabilty
* retryabilty
* scalable ingestion pipelines

4. Non-Goals (VERY IMPORTANT)

RootLens MVP will NOT:

support Kubernetes-native deployments
support production-scale ingestion volumes
implement advanced ML anomaly detection
replace enterprise monitoring platforms
provide multi-tenant support
support real-time auto-remediation
implement distributed storage systems
support complex RBAC/auth systems

This project focuses on:

architecture
observability concepts
event-driven systems
incident intelligence

rather than production-hardening.

6. Core Features
6.1 Event Ingestion

The system must support ingestion of:

logs
traces
metrics
service events

via REST APIs.

6.2 Incident Correlation Engine

The system must:

group related events
deduplicate repetitive alerts
maintain incident lifecycle states
assign severity levels
6.3 AI Analysis Service

The system must generate:

incident summaries
probable root causes
remediation suggestions

based on correlated event context.

6.4 Dependency Graph Visualization

The system should visualize:

service dependencies
event propagation
downstream impact relationships
6.5 Observability Dashboard

The dashboard should provide:

active incidents
service health
trace visualization
incident timelines
AI analysis results
7. System Architecture Overview

RootLens consists of the following core services:

| Service             | Responsibility                     |
| ------------------- | ---------------------------------- |
| Ingestion Service   | Receives logs/events/traces        |
| Incident Engine     | Correlates and manages incidents   |
| AI Analysis Service | Generates AI insights              |
| Graph Service       | Maintains service dependency graph |
| Dashboard Backend   | Aggregates APIs for frontend       |


All services communicate asynchronously through Kafka topics wherever possible.

8. Event Lifecycle
Ingestion Flow
Microservices
    ↓
Ingestion Service
    ↓
Kafka Topics
    ↓
Incident Engine
    ↓
AI Analysis Service
    ↓
Dashboard APIs
9. Incident Lifecycle

Incidents progress through the following states:

OPEN
  ↓
INVESTIGATING
  ↓
IDENTIFIED
  ↓
MITIGATED
  ↓
RESOLVED

Optional future state:

REOPENED
10. Event Types
Supported Event Categories
LOG_EVENT
TRACE_EVENT
METRIC_EVENT
INCIDENT_CREATED
INCIDENT_UPDATED
ALERT_TRIGGERED
AI_ANALYSIS_COMPLETED

These event types will be transported through Kafka topics.

11. Kafka Topics

Initial Kafka topics:

logs-topic
traces-topic
metrics-topic
incidents-topic
alerts-topic
ai-analysis-topic
12. Technology Stack
Component	Technology
Backend Services	Java + Spring Boot
Messaging	Kafka
Cache	Redis
Database	PostgreSQL
Frontend	React
Tracing	OpenTelemetry
Containerization	Docker Compose
13. Non-Functional Requirements
Scalability

Services should scale independently.

Fault Tolerance

Kafka replayability and retry mechanisms should support recovery from transient failures.

Low Latency

Incident correlation should occur near real-time.

Reliability

Services should avoid synchronous coupling where possible.

14. MVP Scope

The MVP will include:

simulated microservices
ingestion APIs
Kafka event pipeline
incident correlation engine
AI summarization
observability dashboard
dependency graph visualization
15. Future Enhancements

Potential future capabilities:

anomaly detection
semantic search
vector embeddings
deployment correlation
Slack integration
Kubernetes integrations
automated remediation
historical analytics
ML-based severity prediction
16. Success Metrics

The MVP will be considered successful if it can:

ingest simulated distributed system events
correlate related failures into incidents
generate AI-assisted root cause insights
visualize service relationships
provide meaningful observability workflows end-to-end
IMPORTANT

After this PRD:
DO NOT immediately jump into coding.

Next document should be:

architecture.md

That’s where we define:

exact service interactions
Kafka flow
storage boundaries
sync vs async communication
retry strategy
scaling bottlenecks
event schemas

That document is where the project starts looking genuinely senior-level.