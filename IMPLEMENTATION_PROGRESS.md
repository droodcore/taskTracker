# Implementation Progress

## Rules

- Work only by stages.
- After each stage: run relevant checks, update this file, then prepare a commit.
- Do not start the next stage until the current one is verified.
- Commit messages should describe the actual change and should not include stage labels.

## Current Status

- Date: 2026-03-18
- Repository mode: migration from monolith to multi-module
- In progress: stage 5 Kafka resilience

## Stages

### Stage 0. Planning and tracking

Goal:
- Split the work into explicit stages and keep progress in one markdown file.

Tasks:
- [x] Inspect current project structure and requirements.
- [x] Define target module layout.
- [x] Create this tracking file.
- [x] Record the exact scope of each stage.
- [x] Confirm the repository state before stage 1 implementation.

Verification:
- Tracking file exists in the repository root.
- Stage list is explicit and ordered.

Verification:
- Tracking file exists in the repository root.
- Stage list is explicit and ordered.
- Repository state checked after the first structural move.

Commit:
- Pending after stage 1, because stage 0 only documents process.

### Stage 1. Multi-module Maven structure

Goal:
- Convert the repository into a parent multi-module Maven project.

Tasks:
- [x] Create parent `pom.xml`.
- [x] Create `task-service` module from the current application.
- [x] Create `notification-service` module.
- [x] Create shared contracts module for Kafka events.
- [x] Ensure module POMs are consistent.

Verification:
- [x] Root Maven reactor sees all modules.
- [x] Base project structure is valid.
- [x] `mvn -q -DskipTests compile`

Commit:
- Done: `7e99841` - `Stage 1: split project into Maven modules`

### Stage 2. Task service requirements

Goal:
- Complete mentor requirements in the main task service.

Tasks:
- [x] Add `@Transactional` to service operations.
- [x] Add pagination and sorting to task listing.
- [x] Add filtering by status and date.
- [x] Add a repository `@Query`.
- [x] Add Kafka producer for task events.
- [x] Use `@ConditionalOnProperty` for Redis cache switching.

Verification:
- [x] Unit tests for service/controller/repository pass.
- [x] Main task endpoint supports filters and paging.
- [x] `mvn -q -pl task-service -am test`

Commit:
- Done: `25f39ff` - `Add task search and Kafka event publishing`

### Stage 3. Notification service

Goal:
- Extract notifications into a separate Spring Boot service.

Tasks:
- [x] Move notification logic into `notification-service`.
- [x] Implement Strategy pattern with multiple senders.
- [x] Add Kafka consumer for task events.
- [x] Configure the service as an independent application.

Verification:
- [x] Notification service starts separately.
- [x] Consumer processes incoming task events.
- [x] `mvn -q -pl notification-service -am test`

Commit:
- Done: `ea81f7e` - `Extract notification service and Kafka consumer`

### Stage 4. Containers and docker compose

Goal:
- Run services independently in containers with infrastructure.

Tasks:
- [x] Add Dockerfile for `task-service`.
- [x] Add Dockerfile for `notification-service`.
- [x] Update `docker-compose.yml` with PostgreSQL, Redis, Kafka, task-service, notification-service.
- [x] Configure container-friendly environment values.

Verification:
- [x] `docker compose config` is valid.
- [x] Services are wired through container hostnames.
- [x] `docker compose config`

Commit:
- Pending: ready to commit after reviewing the final diff for this stage.

### Stage 5. Kafka resilience

Goal:
- Make inter-service Kafka processing more resilient.

Tasks:
- [ ] Add retry handling for message processing.
- [ ] Add exponential backoff for retries.
- [ ] Add circuit breaker around notification delivery.
- [ ] Configure dead letter topic handling.
- [ ] Add dedicated logging for dead letter events.

Verification:
- Retry/DLT behavior is covered by targeted tests where practical.
- Failure flow is documented and observable in logs.

Commit:
- Pending

### Stage 6. Tests and final verification

Goal:
- Strengthen automated coverage and verify end-to-end state.

Tasks:
- [ ] Update existing unit tests.
- [ ] Add tests for new repository filtering and paging.
- [ ] Add tests for Kafka producer/consumer related logic where practical.
- [ ] Run targeted Maven test suites.
- [ ] Record final verification results here.

Verification:
- Relevant test commands succeed.
- Remaining risks are documented.

Commit:
- Pending

## Notes

- `src/main/java/com/example/tasktracker/service/NotificationService.java` had user changes and should be treated carefully during extraction.
- Multi-module restructuring was started before this file was introduced and must now be completed as stage 1 work.
- Current repository state before stage 1 implementation is based on an in-progress move:
  - root `pom.xml` already replaced with parent POM
  - current monolith `src/` was moved to `task-service/src/`
  - new module POMs were created for `task-service`, `notification-service`, `task-contracts`
  - Git still shows deletions from old paths until stage 1 is completed
