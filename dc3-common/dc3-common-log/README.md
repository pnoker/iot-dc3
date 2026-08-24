# DC3 Common Log

## Overview

`dc3-common-log` provides the shared Logback configuration used by IoT DC3 applications. It contributes console and
rolling JSON file appenders plus the Logstash Logback encoder dependency.

The former annotation/aspect logging implementation was removed. This module does not currently register Spring
auto-configuration or provide `@Logs`, `LogsType`, or `LogsAspect` APIs.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-log

## Resources

| Resource | Purpose |
|---|---|
| `logback.xml` | Runtime console logging and size/time-based rolling JSON files |
| `logback-test.xml` | Test logging defaults |
| `AutoConfiguration.imports` | Intentionally empty; documents that no logging aspect is registered |

Spring Boot discovers the shared `logback.xml` from the dependency classpath. Applications normally set
`logging.file.name` in their own `application.yml`; important overrides include `DC3_LOG_LEVEL`, `LOG_FILE`,
`FILE_LOG_THRESHOLD`, and the standard `LOGBACK_ROLLINGPOLICY_*` properties.

Keep log messages in English, use parameterized placeholders, and never log credentials, tokens, passwords, or raw
private payloads.

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-log -am package
```

## Testing

This module has no production Java implementation classes. Its tests verify the shared Logback configuration and
logging policy (`LogbackConfigurationTest`, `LoggingPolicyTest`):

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-log -am test
```

## Related Modules

Center, gateway, and driver applications consume the shared Logback resources through their module dependencies.
