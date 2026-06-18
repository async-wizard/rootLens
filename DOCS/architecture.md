1. Architecture Goals
The Rootlens architecture is designed around the following principles: 
* asynchronous communication
* scalable event ingestion
* decoupled services
* replayable event pipelines
* fault isolation
* observabilty first design

### 2. HIgh Level Architecture
┌────────────────────┐
│ Simulated Services │
│ payment/auth/etc   │
└─────────┬──────────┘
          │
          ▼
┌────────────────────┐
│ Ingestion Service  │
└─────────┬──────────┘
          │
          ▼
┌────────────────────┐
│ Kafka Event Bus    │
└─────────┬──────────┘
          │
  ┌───────┼────────┐
  ▼       ▼        ▼
┌──────────────┐
│ Incident     │
│ Engine       │
└──────┬───────┘
       │
       ▼
┌────────────────────┐
│ AI Analysis Service│
└─────────┬──────────┘
          │
          ▼
┌────────────────────┐
│ Dashboard Backend  │
└─────────┬──────────┘
          │
          ▼
┌────────────────────┐
│ React Dashboard UI │
└────────────────────┘

3. Architectural Principles 
3.1 Async first principles 
Services communicate asynchronously using kafka whenever possible. 
Benefits: 
* loose coupling 
* replayability
* retry support 
* independent scaling 
* fault isolaton

Syncchronous REST communication is minimized to dashboard facing APIs only 

3.2 Event Driven Processing
All observability signals are modeled as events. 
Examples: 
* log events
* trace evnets
* incident events 
* AI analysis completion events

This enables 
* scalable pipelines
* event replay
* stream processiong 
* observability workflows 


3.3 Independent Scalability 
Each service scales independently based on workload charateristics. 
Example: 
* Ingestion service scales for the high volume throughput
* AI analysis service scales for compute heavy interface 
* dashbaord backend scales for read heavy workloads

3.4 Fault Isolation
Failures in one service should not cascade acroos the platform. 
Examples: 
* AI service outages should not block ingestion
* dashboard failtures should not impact incident processing 
* dashboard backend scales for read-heavy workloads 

4. Core Services 
4.1 Ingestion Service 
Responsibilities
The ingestion service acts as the entry point into RootLens
Responsibilities: 
* receive logs/traces/events
* validate payloads
* enrich metadata
* assign timestamps
* publish events to Kafka

APIs
POST /logs
POST /traces
POST /metrics
POST /events

Output Topics
log topic
traces topic
metrices topic 

Scaling characteristics 
This service is 
* write heavy
* burst sensitive
* horizontally scalable

Scaling Strategy
* stateless instances 
* kafka produces batching
* load-balanced replicas 

4.2 Incident Engine 
Responsibilities
The incident Engine is the core intelligence layer. 
Responsibilities: 
* event correlation
* deduplication
* incident lifecycle management
* severity scoring 
* anomaly grouping 

Consumed T    opics
logs-topic
trace-topic
metrics-topic

Produced Topics 
incidents topic
alerts topic

Internal Components
Correlation Engine
Groups related events into incidents 

Deduplication Engine
Prevents repeated alerts for identical failures

Incident Manager 
Maintains: 
* OPEN
* INVESTIGATING
* IDENTIFIED
* MITIGATED
* RESOLVED 

Storage 
PostgreSQL stores: 
* incident metadata
* state transitions
* timestamps 
* severity levels

Redis Caches
* active incidents
* deduplication windows 

Scaling Characteristics: 
This service is: 
* cPU heavy
* stateful
* stream-processing caching

scaling strategy: 
* Kafka Consumer Groups 
* partition-based parallelism
* Redis assisted caching 

4.3 Ai Analysis Service
Responibilities
The AI Analysis Service performs 
* incident summarization
* probable root cause generation 
* remediation recommendation generation 

Consumed Topics 
* incidents-topic
Produced Topics
* ai-analysis-topic

AI Workflow
1. Incident event received 
2. Context aggregation 
3. Prompt generation
4. LLM interface 
5. Analysis persistence
6. Result publication

Failure Handling 
AI failures should: 
* not block incident creation
* retry asynchronously
* support dead letter handling 

Scaling Characteristics
This service is::
* compute inetensive
* latency sensitive
* stateless

Scaling Strategy: 
* async workers 
* queue based processing 
* horizontal scaling 

4.4 Graph service 
Responsibilities
Maintains dependency relationships between: 
* services
* APIs 
* Kafka Topics 
* downstream systems

Capabilities
Supports: 
* impact analysis
* dependency visulization
* service lineage
* propagation tracing 

Example: 
Which services depend on payment service? 
What downstream systems were impacted? 

Storage Strategy
MVP: 
* PostgreSQL adjacency tables 
Future
* graph database (Neo4)

4.5 Dashboard Backend
Responsibilities: 
Provides read-optimised APIs for frontend dashboards
* aggregate service data
* expose incident APIs
* expose trace APIs 
* expose dependency APIs 

APIs
GET /incidents
GET /incidents/{id}
GET /services/dependencies
GET /traces/{traceId}

Scaling Characteristics 
This service is: 
* read-heavy
* aggregation-oriented 

Scaling Strategy: 
* Stateless replicas
* Redis Caching
* Pagination

5. kafka Architecture
kakfa acts as the central event backbone. 
Intial Topics: 
logs-topic
traces-topic
metrics-topic
incidents-topic
alerts-topic
ai-analysis topic

Partitioning Strategy
Intial partitioning key: 
* serviceId
* traceId 

Benefits: 
* ordered processing
* event locality
* parallel consumption

Consumer Groups
Each major service uses independent consumer groups. 
Example: 
incident-engine-group
ai-analysis-group
dashboard-sync-group

This allows: 
* independent scaling
* replayability
* fault recovery

6. Storage Architecture

#### PostgreSQL
Stores:
* incident
* traces metadata
* AI analysis results
* dependency mappings

Chosen Because: 
* Strong Consistency
* relational querying 
* simplicity for MVP 

Redis
Used for: 
* active incident cache
* deduplication windows
* rate limiting 
* dashbaord acceleration

Chosen because: 
* low latency
* lighweight Caching
* ephemeral state support

7. Distributed Tracing 
Rootlens integrates distributed tracing using OpenTelemetry. 
Trace Flow
Service A
   ↓
Service B
   ↓
Service C

Shared Trace IDs allow: 
requested correlation
failure propagation analysis
end-to-end observability

8. Failure Handling Strategy
Transient Failures use: 
* Kafka Retries
* exponential backoff
* retry topics 

Dead Letter Queues (DLQ)
Unrecoverable events move to: 
* dead Letter queues

Examples: 
* Malformed payloads
* failed Ai processing 
* deserialization failures

Idempotency
Consuemrs should process events idempotency where possible
Examples:

duplicate incident updates
replayed Kafka messages
9. Scaling Considerations
Ingestion Scaling

Scale horizontally using:

stateless replicas
Kafka producer batching
Incident Processing Scaling

Scale using:

Kafka partitions
consumer groups
distributed processing
AI Scaling

Scale independently because:

inference workloads are expensive
latency differs from ingestion workloads
10. MVP Constraints

The MVP intentionally limits:

multi-region deployment
Kubernetes orchestration
advanced anomaly detection
distributed databases
multi-tenancy

Focus remains on:

architecture clarity
event-driven systems
observability workflows
scalable design patterns
11. Future Architecture Enhancements

Potential future additions:

Flink/Kafka Streams
vector databases
semantic incident search
deployment correlation
Kubernetes integrations
automated remediation
anomaly detection models
IMPORTANT

After this document, the next HIGH leverage doc is:

event-flow.md

That’s where we define:

exact event lifecycle
event schemas
message contracts
correlation examples
trace propagation
retry flows
AI workflow lifecycle     



