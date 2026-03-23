# Observability & Kafka Retry Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Prometheus+Grafana monitoring, ELK centralized logging, and Resilience4j retry/circuit-breaker on Kafka producer.

**Architecture:** Three cross-cutting concerns added to existing microservices. Prometheus scrapes actuator endpoints; Logstash receives structured JSON from apps and Filebeat from Docker containers; Resilience4j wraps synchronous Kafka send in TaskEventProducer. All new infra runs under Docker Compose `monitoring` and `logging` profiles.

**Tech Stack:** Spring Boot Actuator, Micrometer, Prometheus, Grafana, Elasticsearch 8, Logstash, Kibana, Filebeat, logstash-logback-encoder, Resilience4j 2.2.0

---

## File Structure

### New files
| File | Responsibility |
|------|---------------|
| `prometheus/prometheus.yml` | Prometheus scrape config |
| `grafana/provisioning/datasources/prometheus.yml` | Grafana auto-provision Prometheus datasource |
| `grafana/provisioning/dashboards/dashboard.yml` | Grafana dashboard provider config |
| `grafana/dashboards/infrastructure.json` | Infrastructure Grafana dashboard |
| `grafana/dashboards/business.json` | Business Grafana dashboard |
| `logstash/pipeline/logstash.conf` | Logstash ingestion pipeline |
| `filebeat/filebeat.yml` | Filebeat Docker log collector config |
| `task-service/src/main/resources/logback-spring.xml` | Logback config with Logstash TCP appender |
| `task-service/.../metrics/TaskMetricsService.java` | Scheduled gauge metrics from DB |
| `task-service/.../filter/TraceIdFilter.java` | Servlet filter generating trace_id in MDC |
| `task-service/.../messaging/KafkaHeaderTraceIdInterceptor.java` | Adds trace_id to Kafka producer headers |
| `notification-service/.../interceptor/TraceIdConsumerInterceptor.java` | Extracts trace_id from Kafka headers |

### Modified files
| File | Changes |
|------|---------|
| `task-service/pom.xml` | +actuator, +micrometer-prometheus, +logstash-logback-encoder, +resilience4j-spring-boot3, +resilience4j-micrometer |
| `notification-service/pom.xml` | +spring-boot-starter-web, +actuator, +micrometer-prometheus, +logstash-logback-encoder, +resilience4j-micrometer |
| `task-service/src/main/resources/application.yaml` | +actuator config, +resilience4j config, reduce Kafka native retries |
| `notification-service/src/main/resources/application.yaml` | +server.port 8081, +actuator config |
| `notification-service/src/main/resources/logback-spring.xml` | +Logstash TCP appender |
| `docker-compose.yml` | +prometheus, +grafana, +elasticsearch, +logstash, +kibana, +filebeat containers |
| `task-service/.../messaging/TaskEventProducer.java` | Synchronous send + Resilience4j @Retry + @CircuitBreaker + metrics |
| `task-service/.../config/KafkaProducerConfig.java` | +ProducerInterceptor for trace_id |

---

### Task 1: Add Dependencies to task-service pom.xml

**Files:**
- Modify: `task-service/pom.xml:17-87` (dependencies block)

- [ ] **Step 1: Add actuator + micrometer + logback-encoder + resilience4j dependencies**

Add inside `<dependencies>` block, after the `spring-kafka` dependency (line 46):

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
        </dependency>
        <dependency>
            <groupId>net.logstash.logback</groupId>
            <artifactId>logstash-logback-encoder</artifactId>
            <version>7.4</version>
        </dependency>
        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-spring-boot3</artifactId>
            <version>2.2.0</version>
        </dependency>
        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-micrometer</artifactId>
            <version>2.2.0</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>
```

- [ ] **Step 2: Verify build compiles**

Run: `cd /home/mk/Documents/study/taskTracker && mvn -pl task-service -am compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add task-service/pom.xml
git commit -m "feat(task-service): add actuator, prometheus, logstash-encoder, resilience4j deps"
```

---

### Task 2: Add Dependencies to notification-service pom.xml

**Files:**
- Modify: `notification-service/pom.xml:21-63` (dependencies block)

- [ ] **Step 1: Add web + actuator + micrometer + logback-encoder + resilience4j-micrometer**

Add inside `<dependencies>` block, after `spring-boot-starter` (line 30):

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
        </dependency>
        <dependency>
            <groupId>net.logstash.logback</groupId>
            <artifactId>logstash-logback-encoder</artifactId>
            <version>7.4</version>
        </dependency>
        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-micrometer</artifactId>
            <version>2.2.0</version>
        </dependency>
```

- [ ] **Step 2: Verify build compiles**

Run: `cd /home/mk/Documents/study/taskTracker && mvn -pl notification-service -am compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add notification-service/pom.xml
git commit -m "feat(notification-service): add web, actuator, prometheus, logstash-encoder, r4j-micrometer deps"
```

---

### Task 3: Configure Actuator + Prometheus Endpoint (task-service)

**Files:**
- Modify: `task-service/src/main/resources/application.yaml`

- [ ] **Step 1: Add actuator and management config**

Append to the end of `application.yaml`:

```yaml

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: always
  metrics:
    tags:
      application: task-service
```

- [ ] **Step 2: Commit**

```bash
git add task-service/src/main/resources/application.yaml
git commit -m "feat(task-service): configure actuator with prometheus endpoint"
```

---

### Task 4: Configure Actuator + Prometheus Endpoint (notification-service)

**Files:**
- Modify: `notification-service/src/main/resources/application.yaml`

- [ ] **Step 1: Add server port + actuator config**

Add at the top of the file (before `spring:`):

```yaml
server:
  port: ${SERVER_PORT:8081}

```

Append to end of file:

```yaml

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: always
  metrics:
    tags:
      application: notification-service
```

- [ ] **Step 2: Commit**

```bash
git add notification-service/src/main/resources/application.yaml
git commit -m "feat(notification-service): add web server port 8081, configure actuator with prometheus"
```

---

### Task 5: Add Prometheus + Grafana to Docker Compose

**Files:**
- Create: `prometheus/prometheus.yml`
- Create: `grafana/provisioning/datasources/prometheus.yml`
- Create: `grafana/provisioning/dashboards/dashboard.yml`
- Modify: `docker-compose.yml`

- [ ] **Step 1: Create Prometheus scrape config**

Create `prometheus/prometheus.yml`:

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'task-service'
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ['task-service:8080']

  - job_name: 'notification-service'
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ['notification-service:8081']
```

- [ ] **Step 2: Create Grafana datasource provisioning**

Create `grafana/provisioning/datasources/prometheus.yml`:

```yaml
apiVersion: 1
datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true
```

- [ ] **Step 3: Create Grafana dashboard provider**

Create `grafana/provisioning/dashboards/dashboard.yml`:

```yaml
apiVersion: 1
providers:
  - name: 'default'
    orgId: 1
    folder: 'Task Tracker'
    type: file
    disableDeletion: false
    editable: true
    options:
      path: /var/lib/grafana/dashboards
      foldersFromFilesStructure: false
```

- [ ] **Step 4: Add Prometheus + Grafana containers to docker-compose.yml**

Add before the `volumes:` section at the end of docker-compose.yml:

```yaml
  prometheus:
    image: prom/prometheus:v2.51.0
    container_name: task_tracker_prometheus
    profiles: ["monitoring"]
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
    depends_on:
      task-service:
        condition: service_started
      notification-service:
        condition: service_started
    restart: always

  grafana:
    image: grafana/grafana:10.4.0
    container_name: task_tracker_grafana
    profiles: ["monitoring"]
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_USER: admin
      GF_SECURITY_ADMIN_PASSWORD: admin
    volumes:
      - ./grafana/provisioning:/etc/grafana/provisioning:ro
      - ./grafana/dashboards:/var/lib/grafana/dashboards:ro
      - grafana_data:/var/lib/grafana
    depends_on:
      - prometheus
    restart: always
```

Add `grafana_data:` to the `volumes:` section:

```yaml
volumes:
  task_tracker_db_data:
  grafana_data:
```

- [ ] **Step 5: Commit**

```bash
git add prometheus/ grafana/ docker-compose.yml
git commit -m "feat: add Prometheus + Grafana to docker-compose with provisioning"
```

---

### Task 6: Add Custom Business Metrics to task-service

**Files:**
- Create: `task-service/src/main/java/com/example/tasktracker/metrics/TaskMetricsService.java`
- Modify: `task-service/src/main/java/com/example/tasktracker/service/TaskService.java`

- [ ] **Step 1: Create TaskMetricsService — scheduled gauge populator from DB**

Create `task-service/src/main/java/com/example/tasktracker/metrics/TaskMetricsService.java`:

```java
package com.example.tasktracker.metrics;

import com.example.tasktracker.model.TaskType;
import com.example.tasktracker.repository.TaskRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
public class TaskMetricsService {

    private final TaskRepository taskRepository;
    private final MeterRegistry meterRegistry;

    private final Map<String, AtomicLong> statusGauges = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> typeGauges = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        for (TaskType type : TaskType.values()) {
            AtomicLong value = new AtomicLong(0);
            typeGauges.put(type.name(), value);
            Gauge.builder("tasks_by_type", value, AtomicLong::doubleValue)
                    .tag("type", type.name())
                    .description("Current number of tasks by type")
                    .register(meterRegistry);
        }
    }

    @Scheduled(fixedRate = 30000)
    public void updateMetrics() {
        // Update type gauges
        for (TaskType type : TaskType.values()) {
            long count = taskRepository.findByType(type).size();
            typeGauges.get(type.name()).set(count);
        }

        // Update status gauges dynamically
        taskRepository.findAll().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        task -> task.getStatus() != null ? task.getStatus() : "UNKNOWN",
                        java.util.stream.Collectors.counting()))
                .forEach((status, count) -> {
                    statusGauges.computeIfAbsent(status, s -> {
                        AtomicLong value = new AtomicLong(0);
                        Gauge.builder("tasks_by_status", value, AtomicLong::doubleValue)
                                .tag("status", s)
                                .description("Current number of tasks by status")
                                .register(meterRegistry);
                        return value;
                    }).set(count);
                });
    }
}
```

- [ ] **Step 2: Add @EnableScheduling to TaskTrackerApplication**

In `task-service/src/main/java/com/example/tasktracker/TaskTrackerApplication.java`, add `@EnableScheduling` annotation:

```java
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class TaskTrackerApplication {
```

- [ ] **Step 3: Add tasks_created_total counter to TaskService**

In `task-service/src/main/java/com/example/tasktracker/service/TaskService.java`, add MeterRegistry field and counter increment in createTask:

Add field:
```java
    private final MeterRegistry meterRegistry;
```

Add import:
```java
import io.micrometer.core.instrument.MeterRegistry;
```

In `createTask()` method, after `Task savedTask = taskRepository.save(task);` add:
```java
        meterRegistry.counter("tasks_created_total").increment();
```

- [ ] **Step 4: Verify build compiles**

Run: `cd /home/mk/Documents/study/taskTracker && mvn -pl task-service -am compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add task-service/src/main/java/com/example/tasktracker/metrics/TaskMetricsService.java \
        task-service/src/main/java/com/example/tasktracker/TaskTrackerApplication.java \
        task-service/src/main/java/com/example/tasktracker/service/TaskService.java
git commit -m "feat(task-service): add custom business metrics - task counters and gauges"
```

---

### Task 7: Add Kafka Publish Metrics to TaskEventProducer

**Files:**
- Modify: `task-service/src/main/java/com/example/tasktracker/messaging/TaskEventProducer.java`

- [ ] **Step 1: Add MeterRegistry and counters for Kafka publishing**

Replace the current `TaskEventProducer` content with:

```java
package com.example.tasktracker.messaging;

import com.example.tasktracker.model.Task;
import com.example.taskcontracts.event.NotificationChannel;
import com.example.taskcontracts.event.TaskEventType;
import com.example.taskcontracts.event.TaskNotificationEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

@Slf4j
@Component
public class TaskEventProducer {

    private final KafkaTemplate<String, TaskNotificationEvent> kafkaTemplate;
    private final Counter publishedCounter;
    private final Counter failureCounter;

    @Value("${app.kafka.topic.task-notifications}")
    private String taskNotificationsTopic;

    public TaskEventProducer(
            KafkaTemplate<String, TaskNotificationEvent> kafkaTemplate,
            MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.publishedCounter = Counter.builder("kafka_events_published_total")
                .description("Total Kafka events published successfully")
                .register(meterRegistry);
        this.failureCounter = Counter.builder("kafka_publish_failures_total")
                .description("Total Kafka publish failures")
                .register(meterRegistry);
    }

    public void sendTaskEvent(TaskEventType eventType, Task task) {
        TaskNotificationEvent event = new TaskNotificationEvent(
                eventType,
                task.getId(),
                task.getTitle(),
                task.getStatus(),
                task.getUser().getEmail(),
                NotificationChannel.EMAIL,
                LocalDateTime.now());

        Runnable publishAction = () -> kafkaTemplate.send(taskNotificationsTopic, String.valueOf(task.getId()), event)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        failureCounter.increment();
                        log.error("Failed to publish task event type={} taskId={}", eventType, task.getId(), throwable);
                        return;
                    }

                    publishedCounter.increment();
                    log.info("Published task event type={} taskId={} topic={}",
                            eventType, task.getId(), taskNotificationsTopic);
                });

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishAction.run();
                }
            });
            return;
        }

        publishAction.run();
    }
}
```

- [ ] **Step 2: Verify build compiles**

Run: `cd /home/mk/Documents/study/taskTracker && mvn -pl task-service -am compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add task-service/src/main/java/com/example/tasktracker/messaging/TaskEventProducer.java
git commit -m "feat(task-service): add Kafka publish success/failure counters"
```

---

### Task 8: Add Custom Metrics to notification-service

**Files:**
- Modify: `notification-service/src/main/java/com/example/notificationservice/consumer/TaskEventConsumer.java`
- Modify: `notification-service/src/main/java/com/example/notificationservice/service/NotificationService.java`

- [ ] **Step 1: Add event consumed + DLT counters to TaskEventConsumer**

Add fields and constructor to `TaskEventConsumer`:

```java
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Slf4j
@Component
public class TaskEventConsumer {

    private static final Logger dltLogger = LoggerFactory.getLogger("notification-dlt");

    private final NotificationService notificationService;
    private final Counter consumedCounter;
    private final Counter dltCounter;

    public TaskEventConsumer(NotificationService notificationService, MeterRegistry meterRegistry) {
        this.notificationService = notificationService;
        this.consumedCounter = Counter.builder("kafka_events_consumed_total")
                .description("Total Kafka events consumed successfully")
                .register(meterRegistry);
        this.dltCounter = Counter.builder("dlt_messages_total")
                .description("Total messages sent to DLT")
                .register(meterRegistry);
    }
```

At the end of `consume()` method (after the log.info line), add:
```java
        consumedCounter.increment();
```

At the end of `handleDlt()` method (after dltLogger.error), add:
```java
        dltCounter.increment();
```

- [ ] **Step 2: Add notifications_sent_total counter to NotificationService**

Add `MeterRegistry` to `NotificationService` constructor and counter in `sendNotification()`:

```java
import io.micrometer.core.instrument.MeterRegistry;

@Slf4j
@Service
public class NotificationService {

    private final Map<NotificationChannel, NotificationSender> senders;
    private final MeterRegistry meterRegistry;

    public NotificationService(List<NotificationSender> senders, MeterRegistry meterRegistry) {
        this.senders = senders.stream()
                .collect(Collectors.toUnmodifiableMap(NotificationSender::channel, Function.identity()));
        this.meterRegistry = meterRegistry;
    }
```

After `notificationSender.send(message, recipient);` add:
```java
        meterRegistry.counter("notifications_sent_total", "channel", notificationChannel.name()).increment();
```

- [ ] **Step 3: Verify build compiles**

Run: `cd /home/mk/Documents/study/taskTracker && mvn -pl notification-service -am compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add notification-service/src/main/java/com/example/notificationservice/consumer/TaskEventConsumer.java \
        notification-service/src/main/java/com/example/notificationservice/service/NotificationService.java
git commit -m "feat(notification-service): add consumed, DLT, and notification sent metrics"
```

---

### Task 9: Create Grafana Dashboards

**Files:**
- Create: `grafana/dashboards/infrastructure.json`
- Create: `grafana/dashboards/business.json`

- [ ] **Step 1: Create Infrastructure dashboard JSON**

Create `grafana/dashboards/infrastructure.json` — a Grafana dashboard with panels:
1. **JVM Heap Memory** — `jvm_memory_used_bytes{area="heap"}` by service (timeseries)
2. **JVM Non-Heap Memory** — `jvm_memory_used_bytes{area="nonheap"}` (timeseries)
3. **GC Pause Duration** — `rate(jvm_gc_pause_seconds_sum[5m])` (timeseries)
4. **HTTP Request Rate** — `rate(http_server_requests_seconds_count[5m])` by uri, method (timeseries)
5. **HTTP Latency p95** — `histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))` (timeseries)
6. **Kafka Events Published** — `rate(kafka_events_published_total[5m])` (stat + timeseries)
7. **Kafka Publish Failures** — `kafka_publish_failures_total` (stat)
8. **Kafka Events Consumed** — `rate(kafka_events_consumed_total[5m])` (timeseries)
9. **DLT Messages** — `dlt_messages_total` (stat, red threshold > 0)
10. **Circuit Breaker State** — `resilience4j_circuitbreaker_state` (state timeline)
11. **Retry Calls** — `resilience4j_retry_calls_total` by kind (timeseries)

The JSON should be a full valid Grafana dashboard model (see Grafana docs for structure). Use `uid: "infra-dashboard"`, `title: "Task Tracker - Infrastructure"`.

- [ ] **Step 2: Create Business dashboard JSON**

Create `grafana/dashboards/business.json` — a Grafana dashboard with panels:
1. **Task Creation Rate** — `rate(tasks_created_total[5m])` (timeseries)
2. **Tasks Created Total** — `tasks_created_total` (stat)
3. **Tasks by Status** — `tasks_by_status` by status tag (pie chart)
4. **Tasks by Type** — `tasks_by_type` by type tag (bar chart)
5. **Notifications Sent by Channel** — `rate(notifications_sent_total[5m])` by channel (timeseries)
6. **Notifications Sent Total** — `notifications_sent_total` by channel (stat)
7. **Cache Performance** — `cache_gets_total{result="hit"}` vs `cache_gets_total{result="miss"}` (timeseries)

Use `uid: "business-dashboard"`, `title: "Task Tracker - Business"`.

- [ ] **Step 3: Commit**

```bash
git add grafana/dashboards/
git commit -m "feat: add Grafana infrastructure and business dashboards"
```

---

### Task 10: Add ELK Stack to Docker Compose

**Files:**
- Create: `logstash/pipeline/logstash.conf`
- Create: `filebeat/filebeat.yml`
- Modify: `docker-compose.yml`

- [ ] **Step 1: Create Logstash pipeline config**

Create `logstash/pipeline/logstash.conf`:

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
    if ![service_name] {
      mutate { add_field => { "service_name" => "unknown-service" } }
    }
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
      index => "infra-%{+YYYY.MM.dd}"
    }
  }
}
```

- [ ] **Step 2: Create Filebeat config**

Create `filebeat/filebeat.yml`:

```yaml
filebeat.autodiscover:
  providers:
    - type: docker
      hints.enabled: true
      templates:
        - condition:
            or:
              - contains:
                  docker.container.name: kafka
              - contains:
                  docker.container.name: postgres
              - contains:
                  docker.container.name: redis
          config:
            - type: container
              paths:
                - /var/lib/docker/containers/${data.docker.container.id}/*.log
              multiline.pattern: '^[[:space:]]+(at|\.{3}|Caused by)'
              multiline.negate: false
              multiline.match: after

output.logstash:
  hosts: ["logstash:5044"]

logging.level: warning
```

- [ ] **Step 3: Add ELK + Filebeat containers to docker-compose.yml**

Add before the `volumes:` section:

```yaml
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.13.0
    container_name: task_tracker_elasticsearch
    profiles: ["logging"]
    environment:
      discovery.type: single-node
      xpack.security.enabled: "false"
      ES_JAVA_OPTS: "-Xms512m -Xmx512m"
    ports:
      - "9200:9200"
    volumes:
      - elasticsearch_data:/usr/share/elasticsearch/data
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:9200/_cluster/health || exit 1"]
      interval: 10s
      timeout: 10s
      retries: 15
    restart: always

  logstash:
    image: docker.elastic.co/logstash/logstash:8.13.0
    container_name: task_tracker_logstash
    profiles: ["logging"]
    environment:
      LS_JAVA_OPTS: "-Xms256m -Xmx256m"
    ports:
      - "5000:5000"
      - "5044:5044"
    volumes:
      - ./logstash/pipeline:/usr/share/logstash/pipeline:ro
    depends_on:
      elasticsearch:
        condition: service_healthy
    restart: always

  kibana:
    image: docker.elastic.co/kibana/kibana:8.13.0
    container_name: task_tracker_kibana
    profiles: ["logging"]
    ports:
      - "5601:5601"
    environment:
      ELASTICSEARCH_HOSTS: '["http://elasticsearch:9200"]'
    depends_on:
      elasticsearch:
        condition: service_healthy
    restart: always

  filebeat:
    image: docker.elastic.co/beats/filebeat:8.13.0
    container_name: task_tracker_filebeat
    profiles: ["logging"]
    user: root
    volumes:
      - ./filebeat/filebeat.yml:/usr/share/filebeat/filebeat.yml:ro
      - /var/lib/docker/containers:/var/lib/docker/containers:ro
      - /var/run/docker.sock:/var/run/docker.sock:ro
    depends_on:
      logstash:
        condition: service_started
    restart: always
```

Add volumes:

```yaml
volumes:
  task_tracker_db_data:
  grafana_data:
  elasticsearch_data:
```

- [ ] **Step 4: Commit**

```bash
git add logstash/ filebeat/ docker-compose.yml
git commit -m "feat: add ELK stack + Filebeat to docker-compose with logging profile"
```

---

### Task 11: Add Logstash Appender to task-service Logback

**Files:**
- Create: `task-service/src/main/resources/logback-spring.xml`

- [ ] **Step 1: Create logback-spring.xml with Console + Logstash appenders**

Create `task-service/src/main/resources/logback-spring.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <property name="SERVICE_NAME" value="task-service"/>
    <property name="CONSOLE_PATTERN"
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n"/>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${CONSOLE_PATTERN}</pattern>
        </encoder>
    </appender>

    <appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
        <destination>${LOGSTASH_HOST:-localhost}:${LOGSTASH_PORT:-5000}</destination>
        <reconnectionDelay>5000</reconnectionDelay>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"service_name":"${SERVICE_NAME}"}</customFields>
            <includeMdcKeyName>trace_id</includeMdcKeyName>
        </encoder>
    </appender>

    <springProfile name="!test">
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
            <appender-ref ref="LOGSTASH"/>
        </root>
    </springProfile>

    <springProfile name="test">
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>

    <logger name="com.example.tasktracker" level="DEBUG"/>
</configuration>
```

- [ ] **Step 2: Commit**

```bash
git add task-service/src/main/resources/logback-spring.xml
git commit -m "feat(task-service): add logback config with Logstash TCP appender"
```

---

### Task 12: Add Logstash Appender to notification-service Logback

**Files:**
- Modify: `notification-service/src/main/resources/logback-spring.xml`

- [ ] **Step 1: Add Logstash appender to existing logback-spring.xml**

Replace the entire file content with (preserving existing DLT appender):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <property name="SERVICE_NAME" value="notification-service"/>
    <property name="CONSOLE_PATTERN"
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n"/>
    <property name="LOG_DIR" value="${NOTIFICATION_LOG_DIR:-/tmp/notification-service}"/>
    <property name="DLT_LOG_FILE" value="${LOG_DIR}/notification-dlt.log"/>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${CONSOLE_PATTERN}</pattern>
        </encoder>
    </appender>

    <appender name="DLT_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${DLT_LOG_FILE}</file>
        <encoder>
            <pattern>${CONSOLE_PATTERN}</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${LOG_DIR}/notification-dlt.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <maxFileSize>10MB</maxFileSize>
            <maxHistory>14</maxHistory>
            <totalSizeCap>100MB</totalSizeCap>
        </rollingPolicy>
    </appender>

    <appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
        <destination>${LOGSTASH_HOST:-localhost}:${LOGSTASH_PORT:-5000}</destination>
        <reconnectionDelay>5000</reconnectionDelay>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"service_name":"${SERVICE_NAME}"}</customFields>
            <includeMdcKeyName>trace_id</includeMdcKeyName>
        </encoder>
    </appender>

    <logger name="notification-dlt" level="ERROR" additivity="false">
        <appender-ref ref="DLT_FILE"/>
    </logger>

    <springProfile name="!test">
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
            <appender-ref ref="LOGSTASH"/>
        </root>
    </springProfile>

    <springProfile name="test">
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>
</configuration>
```

- [ ] **Step 2: Commit**

```bash
git add notification-service/src/main/resources/logback-spring.xml
git commit -m "feat(notification-service): add Logstash TCP appender to logback config"
```

---

### Task 13: Add Trace ID Filter to task-service

**Files:**
- Create: `task-service/src/main/java/com/example/tasktracker/filter/TraceIdFilter.java`

- [ ] **Step 1: Create servlet filter that generates trace_id and puts it in MDC**

Create `task-service/src/main/java/com/example/tasktracker/filter/TraceIdFilter.java`:

```java
package com.example.tasktracker.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_KEY = "trace_id";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        MDC.put(TRACE_ID_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID_KEY);
        }
    }
}
```

- [ ] **Step 2: Verify build compiles**

Run: `cd /home/mk/Documents/study/taskTracker && mvn -pl task-service -am compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add task-service/src/main/java/com/example/tasktracker/filter/TraceIdFilter.java
git commit -m "feat(task-service): add TraceIdFilter for MDC trace_id generation"
```

---

### Task 14: Propagate Trace ID Through Kafka Headers

**Files:**
- Modify: `task-service/src/main/java/com/example/tasktracker/messaging/TaskEventProducer.java`
- Create: `notification-service/src/main/java/com/example/notificationservice/interceptor/TraceIdConsumerInterceptor.java`
- Modify: `notification-service/src/main/resources/application.yaml`

- [ ] **Step 1: Add trace_id Kafka header in TaskEventProducer**

In `TaskEventProducer.java`, modify the `publishAction` to include MDC trace_id as Kafka header. Replace the `Runnable publishAction` block:

```java
        String traceId = org.slf4j.MDC.get("trace_id");

        Runnable publishAction = () -> {
            org.springframework.messaging.Message<TaskNotificationEvent> message =
                    org.springframework.messaging.support.MessageBuilder
                            .withPayload(event)
                            .setHeader(org.springframework.kafka.support.KafkaHeaders.TOPIC, taskNotificationsTopic)
                            .setHeader(org.springframework.kafka.support.KafkaHeaders.KEY, String.valueOf(task.getId()))
                            .setHeader("trace_id", traceId != null ? traceId : "")
                            .build();

            kafkaTemplate.send(message)
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            failureCounter.increment();
                            log.error("Failed to publish task event type={} taskId={}", eventType, task.getId(), throwable);
                            return;
                        }

                        publishedCounter.increment();
                        log.info("Published task event type={} taskId={} topic={}",
                                eventType, task.getId(), taskNotificationsTopic);
                    });
        };
```

- [ ] **Step 2: Create consumer interceptor for trace_id extraction**

Create `notification-service/src/main/java/com/example/notificationservice/interceptor/TraceIdConsumerInterceptor.java`:

```java
package com.example.notificationservice.interceptor;

import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class TraceIdConsumerInterceptor implements ConsumerInterceptor<String, Object> {

    @Override
    public ConsumerRecords<String, Object> onConsume(ConsumerRecords<String, Object> records) {
        records.forEach(record -> {
            Header traceIdHeader = record.headers().lastHeader("trace_id");
            if (traceIdHeader != null) {
                String traceId = new String(traceIdHeader.value(), StandardCharsets.UTF_8);
                MDC.put("trace_id", traceId);
            }
        });
        return records;
    }

    @Override
    public void onCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {}

    @Override
    public void close() {}

    @Override
    public void configure(Map<String, ?> configs) {}
}
```

- [ ] **Step 3: Register interceptor in notification-service application.yaml**

Add to the `spring.kafka.consumer.properties` section in `notification-service/src/main/resources/application.yaml`:

```yaml
        interceptor.classes: com.example.notificationservice.interceptor.TraceIdConsumerInterceptor
```

- [ ] **Step 4: Verify both services compile**

Run: `cd /home/mk/Documents/study/taskTracker && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add task-service/src/main/java/com/example/tasktracker/messaging/TaskEventProducer.java \
        notification-service/src/main/java/com/example/notificationservice/interceptor/TraceIdConsumerInterceptor.java \
        notification-service/src/main/resources/application.yaml
git commit -m "feat: propagate trace_id through Kafka headers for cross-service log correlation"
```

---

### Task 15: Reduce Kafka Native Retries + Add Resilience4j Config

**Files:**
- Modify: `task-service/src/main/resources/application.yaml`

- [ ] **Step 1: Reduce Kafka producer native retries and add Resilience4j config**

In `task-service/src/main/resources/application.yaml`, replace the Kafka producer section:

```yaml
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9094,localhost:9095,localhost:9096}
    producer:
      acks: all
      retries: 0
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      properties:
        enable.idempotence: false
        delivery.timeout.ms: 5000
        max.in.flight.requests.per.connection: 5
        request.timeout.ms: 3000
        spring.json.add.type.headers: false
```

Note: `enable.idempotence` set to `false` because it requires `retries > 0`. We're delegating retry to Resilience4j.

Append to end of file (after `logging:` block):

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
          - java.util.concurrent.ExecutionException
        ignore-exceptions:
          - org.apache.kafka.common.errors.SerializationException
  circuitbreaker:
    instances:
      kafkaPublisher:
        sliding-window-type: COUNT_BASED
        sliding-window-size: 10
        minimum-number-of-calls: 5
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        permitted-number-of-calls-in-half-open-state: 3
```

- [ ] **Step 2: Commit**

```bash
git add task-service/src/main/resources/application.yaml
git commit -m "feat(task-service): reduce Kafka native retries, add Resilience4j retry+CB config"
```

---

### Task 16: Add Resilience4j Retry + Circuit Breaker to TaskEventProducer

**Files:**
- Modify: `task-service/src/main/java/com/example/tasktracker/messaging/TaskEventProducer.java`

- [ ] **Step 1: Rewrite TaskEventProducer with synchronous send + R4J annotations**

Replace `TaskEventProducer.java` with:

```java
package com.example.tasktracker.messaging;

import com.example.tasktracker.model.Task;
import com.example.taskcontracts.event.NotificationChannel;
import com.example.taskcontracts.event.TaskEventType;
import com.example.taskcontracts.event.TaskNotificationEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class TaskEventProducer {

    private final KafkaTemplate<String, TaskNotificationEvent> kafkaTemplate;
    private final Counter publishedCounter;
    private final Counter failureCounter;
    private final Counter fallbackCounter;

    @Value("${app.kafka.topic.task-notifications}")
    private String taskNotificationsTopic;

    public TaskEventProducer(
            KafkaTemplate<String, TaskNotificationEvent> kafkaTemplate,
            MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.publishedCounter = Counter.builder("kafka_events_published_total")
                .description("Total Kafka events published successfully")
                .register(meterRegistry);
        this.failureCounter = Counter.builder("kafka_publish_failures_total")
                .description("Total Kafka publish failures")
                .register(meterRegistry);
        this.fallbackCounter = Counter.builder("kafka_publish_fallback_total")
                .description("Total Kafka publish circuit breaker fallbacks")
                .register(meterRegistry);
    }

    public void sendTaskEvent(TaskEventType eventType, Task task) {
        TaskNotificationEvent event = new TaskNotificationEvent(
                eventType,
                task.getId(),
                task.getTitle(),
                task.getStatus(),
                task.getUser().getEmail(),
                NotificationChannel.EMAIL,
                LocalDateTime.now());

        String traceId = MDC.get("trace_id");

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doSend(event, task.getId(), eventType, traceId);
                }
            });
            return;
        }

        doSend(event, task.getId(), eventType, traceId);
    }

    @CircuitBreaker(name = "kafkaPublisher", fallbackMethod = "fallbackSend")
    @Retry(name = "kafkaPublisher")
    protected void doSend(TaskNotificationEvent event, Long taskId, TaskEventType eventType, String traceId) {
        Message<TaskNotificationEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, taskNotificationsTopic)
                .setHeader(KafkaHeaders.KEY, String.valueOf(taskId))
                .setHeader("trace_id", traceId != null ? traceId : "")
                .build();

        try {
            kafkaTemplate.send(message).get(5, TimeUnit.SECONDS);
            publishedCounter.increment();
            log.info("Published task event type={} taskId={} topic={}", eventType, taskId, taskNotificationsTopic);
        } catch (ExecutionException e) {
            failureCounter.increment();
            log.error("Failed to publish task event type={} taskId={}", eventType, taskId, e.getCause());
            throw new org.apache.kafka.common.KafkaException("Kafka send failed", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            failureCounter.increment();
            throw new org.apache.kafka.common.KafkaException("Kafka send interrupted", e);
        } catch (TimeoutException e) {
            failureCounter.increment();
            log.error("Kafka send timed out type={} taskId={}", eventType, taskId);
            throw e;
        }
    }

    @SuppressWarnings("unused")
    protected void fallbackSend(TaskNotificationEvent event, Long taskId, TaskEventType eventType,
                                String traceId, Throwable throwable) {
        fallbackCounter.increment();
        log.warn("Circuit breaker fallback for task event type={} taskId={}: {}",
                eventType, taskId, throwable.getMessage());
    }
}
```

- [ ] **Step 2: Verify build compiles**

Run: `cd /home/mk/Documents/study/taskTracker && mvn -pl task-service -am compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add task-service/src/main/java/com/example/tasktracker/messaging/TaskEventProducer.java
git commit -m "feat(task-service): add Resilience4j retry + circuit breaker on Kafka producer"
```

---

### Task 17: Update Docker Compose for Full Integration

**Files:**
- Modify: `docker-compose.yml`

- [ ] **Step 1: Add LOGSTASH_HOST and SERVER_PORT env vars to services**

In the `task-service` environment section, add:
```yaml
      LOGSTASH_HOST: logstash
      LOGSTASH_PORT: 5000
```

In the `notification-service` environment section, add:
```yaml
      LOGSTASH_HOST: logstash
      LOGSTASH_PORT: 5000
      SERVER_PORT: 8081
```

Add port mapping to notification-service (needed for Prometheus scraping):
```yaml
    ports:
      - "8081:8081"
```

- [ ] **Step 2: Commit**

```bash
git add docker-compose.yml
git commit -m "feat: add Logstash host/port env vars and notification-service port mapping"
```

---

### Task 18: Smoke Test — Full Stack

- [ ] **Step 1: Build both services**

```bash
cd /home/mk/Documents/study/taskTracker && mvn clean package -DskipTests -q
```
Expected: BUILD SUCCESS

- [ ] **Step 2: Start core services**

```bash
docker compose up -d db redis kafka-1 kafka-2 kafka-3 task-service notification-service
```

Wait for health checks, then verify:
```bash
docker compose ps
```

- [ ] **Step 3: Verify actuator endpoints**

```bash
curl -s http://localhost:8080/actuator/prometheus | head -5
curl -s http://localhost:8081/actuator/prometheus | head -5
```
Expected: Prometheus-format metrics output

- [ ] **Step 4: Start monitoring stack**

```bash
docker compose --profile monitoring up -d
```

Verify:
```bash
curl -s http://localhost:9090/api/v1/targets | python3 -m json.tool | grep -A2 '"health"'
```
Expected: Both targets show `"health": "up"`

Open Grafana at `http://localhost:3000` (admin/admin), verify dashboards appear.

- [ ] **Step 5: Start logging stack**

```bash
docker compose --profile logging up -d
```

Verify Elasticsearch:
```bash
curl -s http://localhost:9200/_cluster/health | python3 -m json.tool | grep status
```
Expected: `"status": "green"` or `"yellow"`

Open Kibana at `http://localhost:5601`, create index patterns.

- [ ] **Step 6: Create a task to generate metrics + logs**

```bash
curl -X POST http://localhost:8080/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Test observability","description":"Smoke test","status":"NEW","type":"STUDY","userId":1,"categoryId":1}'
```

Verify in:
- Grafana: `tasks_created_total` increments
- Kibana: log entry appears for task-service with `trace_id`
- Prometheus: `curl http://localhost:9090/api/v1/query?query=tasks_created_total`

- [ ] **Step 7: Commit any final adjustments**

```bash
git add -A && git status
```

Only commit if there are meaningful changes.