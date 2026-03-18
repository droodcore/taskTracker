# Implementation Progress

## Rules

- Work only by stages.
- After each stage: run relevant checks, update this file, then prepare a commit.
- Do not start the next stage until the current one is verified.

## Current Status

- Date: 2026-03-18
- Repository mode: migration from monolith to multi-module
- In progress: stage 0 planning and tracking

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
- Pending: create commit after reviewing git state for stage-only changes.

### Stage 2. Task service requirements

Goal:
- Complete mentor requirements in the main task service.

Tasks:
- [ ] Add `@Transactional` to service operations.
- [ ] Add pagination and sorting to task listing.
- [ ] Add filtering by status and date.
- [ ] Add a repository `@Query`.
- [ ] Add Kafka producer for task events.
- [ ] Use `@ConditionalOnProperty` for Redis cache switching.

Verification:
- Unit tests for service/controller/repository pass.
- Main task endpoint supports filters and paging.

Commit:
- Pending

### Stage 3. Notification service

Goal:
- Extract notifications into a separate Spring Boot service.

Tasks:
- [ ] Move notification logic into `notification-service`.
- [ ] Implement Strategy pattern with multiple senders.
- [ ] Add Kafka consumer for task events.
- [ ] Configure the service as an independent application.

Verification:
- Notification service starts separately.
- Consumer processes incoming task events.

Commit:
- Pending

### Stage 4. Containers and docker compose

Goal:
- Run services independently in containers with infrastructure.

Tasks:
- [ ] Add Dockerfile for `task-service`.
- [ ] Add Dockerfile for `notification-service`.
- [ ] Update `docker-compose.yml` with PostgreSQL, Redis, Kafka, task-service, notification-service.
- [ ] Configure container-friendly environment values.

Verification:
- `docker compose config` is valid.
- Services are wired through container hostnames.

Commit:
- Pending

### Stage 5. Tests and final verification

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
