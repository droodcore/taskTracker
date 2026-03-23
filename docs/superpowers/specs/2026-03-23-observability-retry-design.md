# Observability & Kafka Retry — Design Spec

## Overview

Three additions to the taskTracker project:
1. **Prometheus + Grafana** — metrics collection and visualization
2. **ELK Stack** — centralized logging
3. **Kafka Producer Retry** — Resilience4j retry + circuit breaker on event publishing

Target: production-like setup for learning purposes.

---

## 1. Prometheus + Grafana

### Dependencies (both services)

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-actuator` | Expose metrics/health endpoints |
| `micrometer-registry-prometheus` | Prometheus metric format |

### Docker Compose

- **Prometheus** — scrape interval 15s, targets: `task-service:8080/actuator/prometheus`, `notification-service:8081/actuator/prometheus`
- **Grafana** — port 3000, provisioned datasource (Prometheus), two dashboards from JSON files

### Actuator Configuration

Both services expose:
- `/actuator/prometheus` — metrics in Prometheus format
- `/actuator/health` — health checks

notification-service moves to port **8081** to avoid conflict with task-service.

### Custom Metrics

**task-service (business):**
- `tasks_created_total` (Counter) — total tasks created
- `tasks_by_status` (Gauge, tag: status) — current tasks per status
- `tasks_by_type` (Gauge, tag: type) — current tasks per type
- `cache_hits_total` / `cache_misses_total` (Counter) — Redis cache effectiveness

**task-service (infrastructure):**
- `kafka_events_published_total` (Counter, tag: event_type) — events sent to Kafka
- `kafka_publish_failures_total` (Counter) — failed publish attempts
- `kafka_retry_attempts_total` (Counter) — retry invocations
- Resilience4j metrics (auto-exported via micrometer)

**notification-service:**
- `kafka_events_consumed_total` (Counter, tag: event_type) — events consumed
- `notifications_sent_total` (Counter, tag: channel) — notifications by channel
- `dlt_messages_total` (Counter) — dead letter topic messages
- Resilience4j circuit breaker metrics (auto-exported)

### Grafana Dashboards

**Infrastructure Dashboard:**
- JVM Memory (heap/non-heap), GC pauses, thread count
- HTTP request rate, latency percentiles (p50/p95/p99), error rate
- Kafka producer/consumer metrics, retry/failure rates
- Circuit breaker state timeline

**Business Dashboard:**
- Task creation rate over time
- Tasks by status (pie chart)
- Tasks by type (bar chart)
- Notification delivery rate by channel
- DLT message rate (alert indicator)

---

## 2. ELK Stack

### Docker Compose

| Service | Port | Purpose |
|---|---|---|
| Elasticsearch 8.x | 9200 | Log storage & search (single-node, security disabled) |
| Logstash | 5000 (TCP), 5044 (Beats) | Log ingestion & transformation |
| Kibana | 5601 | Log visualization |
| Filebeat | — | Docker container log collector |

### Dependencies (both services)

| Dependency | Purpose |
|---|---|
| `logstash-logback-encoder` | Structured JSON logging over TCP to Logstash |

### Logback Configuration (both services)

Two appenders:
- **Console appender** — human-readable format, unchanged (for `docker logs`)
- **LogstashTcpSocketAppender** — JSON logs → Logstash:5000

MDC context fields:
- `service_name` — identifies the source service
- `trace_id` — request correlation across services

Structured JSON fields: timestamp, level, logger, thread, message, stack_trace.

### Logstash Pipeline

```
input {
  tcp {
    port => 5000
    codec => json_lines
    tags => ["app"]
  }
  beats {
    port => 5044
    tags => ["infra"]
  }
}

filter {
  if "app" in [tags] {
    mutate { add_field => { "log_source" => "application" } }
  }
  if "infra" in [tags] {
    mutate { add_field => { "log_source" => "infrastructure" } }
  }
}

output {
  if "app" in [tags] {
    elasticsearch {
      hosts => ["elasticsearch:9200"]
      index => "%{[service_name]}-%{+YYYY.MM.dd}"
    }
  }
  if "infra" in [tags] {
    elasticsearch {
      hosts => ["elasticsearch:9200"]
      index => "infra-%{[container][name]}-%{+YYYY.MM.dd}"
    }
  }
}
```

### Filebeat

- Docker autodiscovery filtering by container name (kafka*, postgres, redis)
- Multiline pattern for Java stack traces
- Output → Logstash:5044

### Kibana

- Index patterns: `task-service-*`, `notification-service-*`, `infra-*`
- Saved searches: errors only, Kafka events, slow requests
- Dashboard: log volume over time, error rate by service, top error messages

### Design Decisions

- **Per-day indices** — enables ILM rotation policy
- **Separate app/infra indices** — different retention, different search patterns
- **JSON only to Logstash** — console remains human-readable for local dev

---

## 3. Kafka Producer Retry (Resilience4j)

### Dependencies (task-service)

| Dependency | Purpose |
|---|---|
| `resilience4j-spring-boot3` | Retry + Circuit Breaker |
| `resilience4j-micrometer` | Export R4J metrics to Prometheus |

### Architecture

Two layers on `KafkaEventPublisher`:

```
Request → CircuitBreaker → Retry → KafkaTemplate.send()
```

**Layer 1: Retry** — transient failures (broker temporarily unavailable, network blip)
- Max attempts: 3
- Wait: 500ms → 1s → 2s (exponential backoff, multiplier 2.0)
- Retry on: `KafkaException`, `TimeoutException`
- Ignore: `SerializationException` (retry is pointless)

**Layer 2: Circuit Breaker** — prolonged outages (Kafka cluster down)
- Sliding window: 10 calls
- Failure rate threshold: 50%
- Wait in open state: 30s
- Permitted calls in half-open: 3
- Fallback: log warning + increment `kafka_publish_fallback_total` metric

CB wraps Retry — when retry is exhausted, it counts as a CB failure. When CB is open, retry doesn't even start (fail-fast).

### Configuration (application.yaml)

```yaml
resilience4j:
  retry:
    instances:
      kafkaPublisher:
        max-attempts: 3
        wait-duration: 500ms
        enable-exponential-backoff: true
        exponential-backoff-multiplier: 2.0
        retry-exceptions:
          - org.apache.kafka.common.KafkaException
          - java.util.concurrent.TimeoutException
        ignore-exceptions:
          - org.apache.kafka.common.errors.SerializationException
  circuitbreaker:
    instances:
      kafkaPublisher:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        permitted-number-of-calls-in-half-open-state: 3
```

### Metrics (auto-exported via resilience4j-micrometer)

- `resilience4j_retry_calls_total` (tag: kind=successful/failed/retry)
- `resilience4j_circuitbreaker_state`
- `resilience4j_circuitbreaker_calls_total`
- Custom: `kafka_publish_fallback_total`

### Integration with Existing Code

Current flow: `TaskService` publishes events via `TransactionSynchronizationManager.registerSynchronization()` (after commit). The Retry/CB decorators wrap `KafkaTemplate.send()` inside the publisher. Transaction synchronization logic remains unchanged.

---

## File Changes Summary

### New files
- `prometheus/prometheus.yml` — scrape config
- `grafana/provisioning/datasources/prometheus.yml` — datasource
- `grafana/provisioning/dashboards/dashboard.yml` — dashboard provider
- `grafana/dashboards/infrastructure.json` — infrastructure dashboard
- `grafana/dashboards/business.json` — business dashboard
- `logstash/pipeline/logstash.conf` — Logstash pipeline
- `filebeat/filebeat.yml` — Filebeat config

### Modified files
- `pom.xml` — add actuator, micrometer-prometheus, logstash-logback-encoder, resilience4j dependencies
- `docker-compose.yml` — add Prometheus, Grafana, Elasticsearch, Logstash, Kibana, Filebeat containers
- `task-service/src/main/resources/application.yaml` — actuator, resilience4j config
- `notification-service/src/main/resources/application.yaml` — actuator config, port 8081
- `task-service/src/main/resources/logback-spring.xml` — add Logstash appender
- `notification-service/src/main/resources/logback-spring.xml` — add Logstash appender
- `TaskService.java` or `KafkaEventPublisher.java` — add custom metrics, R4J annotations
- `NotificationService` / `TaskEventConsumer` — add custom metrics