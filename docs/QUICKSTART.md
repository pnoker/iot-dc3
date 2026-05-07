# Local Development Quick Start

One-stop workflow for running IoT DC3 locally from source.

## Prerequisites

- JDK 21
- Maven 3.9+
- Podman or Docker
- Make (optional, but recommended)

## 1. Start base infrastructure

```bash
# Global registry
make dev-db

# or mainland China registry
make dev-db REGISTRY=domestic
```

This starts PostgreSQL and RabbitMQ.

## 2. (Optional) Start optional dependencies

```bash
make dev-optional
# or
make dev-optional REGISTRY=domestic
```

Typically used for MQTT broker dependencies such as EMQX.

## 3. Load environment variables (optional for source run)

```bash
source dc3/env/dev.env.sh
```

Review the file first; it uses `source KEY=value` style lines in some environments.

## 4. Build from source

```bash
# Fast build with parallel threads
mvn -s .mvn/settings.xml clean package

# Deploy API/Common modules (if needed)
make deploy
```

The repository is configured with:

- Parallel builds (-T 1C)
- Test skip for packaging (-Dmaven.test.skip=true)
- Enforced JDK 21 and Maven 3.9+
- Spring Java Format validation

## 5. Run services (recommended order)

```bash
java -jar dc3-gateway/target/dc3-gateway.jar
java -jar dc3-center/dc3-center-auth/target/dc3-center-auth.jar
java -jar dc3-center/dc3-center-data/target/dc3-center-data.jar
java -jar dc3-center/dc3-center-manager/target/dc3-center-manager.jar
java -jar dc3-driver/dc3-driver-virtual/target/dc3-driver-virtual.jar
```

## 6. Run via Docker Compose (alternative)

```bash
# Local dev stack
make dev
make dev REGISTRY=domestic

# Full local environment (db + optional + dev)
make dev-all
make dev-all REGISTRY=domestic

# Packaged application stack
make app
make app REGISTRY=domestic
make app-all REGISTRY=aliyun
```

## 7. Useful maintenance commands

```bash
# Print resolved compose file
make compose-file STACK=dev

# Tail logs
make compose-logs STACK=dev

# Container status
make compose-ps STACK=dev

# Restart
make compose-restart STACK=dev
```

## 8. Service default endpoints

| Service | HTTP | gRPC |
|---------|------|------|
| Gateway | 8000 | - |
| Auth Center | 8300 | 9300 |
| Manager Center | 8400 | 9400 |
| Data Center | 8500 | 9500 |

## 9. Recommended local workflow

1. `make dev-db`
2. `mvn -s .mvn/settings.xml clean package`
3. Run Gateway -> Auth -> Data -> Manager -> Driver
4. Test APIs via Gateway at http://localhost:8000/api/v3/...
5. If using observability stack, start Grafana/ELK after the core platform is stable

## 10. Common pitfalls

- If Nacos is required in pre/pro profile, make sure the register service is available.
- If ports are occupied, override them through .env or environment variables.
- If you want to run everything in one JVM for debugging, use dc3-center-single.

```bash
java -jar dc3-center/dc3-center-single/target/dc3-center-single.jar
```
