# Troubleshooting FAQ

Common local development and runtime issues in IoT DC3 and how to resolve them.

## 1. Maven build is slow

**Cause**: Parallelism not enabled / JVM heap too small.

**Resolution**: Already configured in this repo:
- `.mvn/maven.config` contains `-T 1C`
- `.mvn/jvm.config` contains `-Xms512m -Xmx1024m`

If still slow, increase heap or reduce background CPU load.

## 2. Unsupported class file major version / Java version errors

**Cause**: Project requires JDK 21.

**Resolution**:
```bash
java -version
mvn -version
```
The build now enforces JDK 21 via `maven-enforcer-plugin`.

## 3. spring-javaformat:validate fails

**Cause**: Code formatting does not match Spring Java Format rules.

**Resolution**:
```bash
mvn -s .mvn/settings.xml io.spring.javaformat:spring-javaformat-maven-plugin:apply
```
Then rebuild.

## 4. Ports already in use

**Symptom**: Application fails to start due to occupied ports (8000/8300/8400/8500/9300/9400/9500).

**Resolution**: Override ports using environment variables or `.env` values.

Common overrides:
- `SERVER_PORT`
- `GRPC_SERVER_PORT`
- `DC3_GATEWAY_PORT`
- `DC3_AUTH_PORT`
- `DC3_MANAGER_PORT`
- `DC3_DATA_PORT`

## 5. Database connection failures in dev

**Cause**: Postgres container not started / custom .env changed published ports.

**Resolution**:
```bash
make compose-ps STACK=db
make compose-file STACK=db
```
Confirm the published port matches the application config.

## 6. RabbitMQ connection failures

**Cause**: Container not ready / wrong virtual host or credentials.

**Resolution**: Wait until healthcheck passes, then restart dependent services.
```bash
make compose-logs STACK=db
```

## 7. Nacos discovery errors in pre/pro profile

**Cause**: Those profiles expect service discovery via Nacos.

**Resolution**: For local source debugging, prefer `dev` profile unless you intentionally test registry behavior.

## 8. Gateway returns 401/403

**Cause**: Request routed to authenticated endpoints without token.

**Resolution**: Call token APIs first (`/api/v3/auth/token/...`), then pass the token in subsequent requests.

## 9. Driver cannot register

**Cause**: Manager not running / gRPC target address misconfigured.

**Resolution**: Start services in recommended order: Gateway -> Auth -> Data -> Manager -> Driver.

## 10. Docker image build fails in Makefile

**Cause**: `mvn package` failed inside image build.

**Resolution**:
```bash
make package
make build
```

## 11. Aliyun registry variant used unexpectedly

**Cause**: `REGISTRY` was set to `domestic` / `aliyun` / `cn`.

**Resolution**: Use `REGISTRY=global` for Docker Hub images.
```bash
make dev-db REGISTRY=global
```

## 12. Want faster debugging in one JVM

Use `dc3-center-single` to combine Auth/Manager/Data into one process.
```bash
java -jar dc3-center/dc3-center-single/target/dc3-center-single.jar
```
