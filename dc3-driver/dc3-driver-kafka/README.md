# DC3 Driver Kafka

## Overview

`dc3-driver-kafka` treats Apache Kafka as a streaming data source. Kafka is a publish-subscribe stream, so inbound
values are received asynchronously through a `@KafkaListener` and cached by message key; a point read returns the
latest cached value, and a point write produces a message to the configured topic.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-kafka
- **Driver Name**: Kafka Driver

## Driver Attributes (Device-level)

| Attribute | Code  | Type   | Default          | Description                         |
|-----------|-------|--------|------------------|-------------------------------------|
| Topic     | topic | STRING | dc3-driver-kafka | Default topic for produce/consume   |

## Point Attributes

| Attribute | Code  | Type   | Default | Description                                            |
|-----------|-------|--------|---------|--------------------------------------------------------|
| Topic     | topic | STRING |         | Override topic (defaults to driver topic)              |
| Key       | key   | STRING |         | Message key used for produce and cache lookup          |

## Command Attributes (write)

| Attribute | Code  | Type   | Default | Description                              |
|-----------|-------|--------|---------|------------------------------------------|
| Topic     | topic | STRING |         | Topic to produce the write message to    |

The module `application.yml` is authoritative for attribute codes, types, defaults, scheduling, health, and local
buffering. Keep this README aligned when those user-facing settings change.

## Prerequisites

A reachable Kafka cluster. Point the `spring.kafka.*` properties (or `KAFKA_BOOTSTRAP_SERVERS`) at the target broker.

## Connection

The broker connection is configured through Spring Boot `spring.kafka.*` properties in `application.yml`, driven by
the `KAFKA_BOOTSTRAP_SERVERS` environment variable.

## Running Locally

```bash
make up-db
make up-dev GROUP=core
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-kafka -am package
java -jar dc3-driver/dc3-driver-kafka/target/dc3-driver-kafka.jar
```

## Testing

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-kafka -am test
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration
