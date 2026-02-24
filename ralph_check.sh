#!/bin/bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
export DOCKER_HOST=unix:///var/run/docker.sock
export DOCKER_API_VERSION=1.44
export TESTCONTAINERS_RYUK_DISABLED=true

# Gatekeeper script for RALPH flow

if [ -f .env ]; then
  set -a
  source .env
  set +a
fi

echo "Running RALPH Gatekeeper Checks..."

# 2. Compile & Unit Tests
./mvnw clean test || { echo "Tests failed! Commit rejected."; exit 1; }

# 3. Integration Tests (Verify Context starts with Docker)
# Assuming docker is running. If strict, can spin up testcontainers here.
./mvnw verify -DskipTests=false || { echo "Verification failed"; exit 1; }

echo "✅ All checks passed. Ready to commit."
exit 0