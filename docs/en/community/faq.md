---
title: FAQ
---

# FAQ (Q&A)

## Licensing

### What open-source license does IoT DC3 use?

IoT DC3 is released under the [AGPL-3.0](https://github.com/pnoker/iot-dc3/blob/release/LICENSE-AGPL.txt) license.

The core requirement of AGPL-3.0: if you modify the platform code and **provide it as a network service** (including
SaaS or internal systems), you must open-source the complete modified source code. Internal use without redistribution
or network service provision does not require open-sourcing.

### What does AGPL-3.0 mean for my company?

| Scenario                                                                | Must open-source?                               |
|-------------------------------------------------------------------------|-------------------------------------------------|
| Internal deployment, no code changes, self-use only                     | No                                              |
| Internal deployment, modified code, self-use only (no network service)  | No (but contributions welcome)                  |
| SaaS product built on DC3, sold externally                              | **Yes**, all modifications must be open-sourced |
| Secondary development based on DC3, distributed to customer deployments | **Yes**, all modifications must be open-sourced |
| Only calling DC3 APIs, no changes to DC3 itself                         | No                                              |

### Can I develop proprietary derivatives?

If you only call DC3 APIs without modifying DC3 source code, your caller-side code can remain closed-source. Once you
modify DC3 source code and provide it as a network service, AGPL-3.0 requires you to open-source your modifications.

### Is there a commercial license?

There is currently no standalone commercial license. If your use case is compatible with AGPL-3.0, you may use it
freely. For special requirements, contact the maintainers through community channels.

---

## Pricing & Business Model

### Does IoT DC3 itself cost money?

**No.** IoT DC3 is completely free and open-source. You may freely download, use, modify, and distribute it (subject to
AGPL-3.0 terms).

### How does the project sustain itself?

IoT DC3 is currently a personal open-source project maintained by the author, operating as a community-driven effort.
Possible future commercialization paths include: technical support services, enterprise custom development, SaaS
hosting, etc. The core platform will always remain open-source.

### Do I need to pay anyone to use IoT DC3?

No. No payment to anyone is required to use IoT DC3. However, you are responsible for your own infrastructure costs (
servers, databases, etc.).

---

## Technology Choices

### Why Java instead of Go / Node.js / Python?

IoT DC3 chose the Java + Spring ecosystem for these core reasons:

1. **Industrial IoT landscape**: A large body of existing industrial systems (SCADA, MES, ERP) are Java-based. Java has
   natural advantages in industrial integration.
2. **Spring ecosystem maturity**: Spring Boot / Cloud / Security / Data provide out-of-the-box capabilities for
   distributed systems, security, and data access.
3. **JVM stability**: Long-running device-access services demand reliable GC and memory management. The JVM has decades
   of production-proven stability.
4. **AI integration**: Spring AI enables the platform to interface with multiple LLM providers (OpenAI, Claude, local
   models, etc.) through a unified paradigm.
5. **Team expertise**: The maintainer has deep experience in the Java / Spring ecosystem.

### Why PostgreSQL instead of MySQL?

1. **TimescaleDB extension**: For IoT time-series data, the TimescaleDB extension on PostgreSQL provides native
   hypertable auto-partitioning, compression, and data retention policies.
2. **Apache AGE**: A graph database extension for device relationship and topology path queries.
3. **pgvector**: A vector extension providing infrastructure for AI semantic search.
4. **Richer data types**: JSONB, arrays, range types, etc.
5. **Stricter SQL standards**: More reliable in complex query and transaction scenarios.

IoT DC3 depends deeply on PostgreSQL. These three extensions (TimescaleDB + AGE + pgvector) are core to the platform's
data architecture.

### What device protocols are supported? How do I choose?

The platform includes **28 built-in driver modules**, covering:

- **Industrial Bus / PLC**: Modbus TCP/RTU, OPC UA/DA, S7 (Siemens), MELSEC, FINS (Omron), EtherNet/IP
- **SCADA / Power / Metering**: BACnet/IP, IEC 104, DLMS, SL651, SNMP
- **IoT / Wireless**: MQTT, CoAP, LwM2M, HTTP, BLE, Zigbee, CAN
- **Serial / General Network**: Serial, TCP/UDP
- **Database**: MySQL, PostgreSQL, Oracle, SQL Server

Selection tip: first identify the protocols your field devices support, then check
the [Driver Capability Matrix](../drivers/matrix) to confirm the required read / write / subscribe capabilities are met.

---

## Deployment & Operations

### Minimum hardware requirements?

**Development environment** (dependency stack only — PostgreSQL + RabbitMQ):

- CPU: 2 cores
- RAM: 4 GB
- Disk: 20 GB

**Production environment** (full stack — gateway + 4 centers + N drivers + dependencies):

- CPU: 8 cores or more
- RAM: 16 GB or more
- Disk: 100 GB SSD or more (time-series data grows continuously; plan for expansion)

### How to migrate from development to production?

1. **Security hardening**: Change default keys / passwords, enable TLS, configure firewall rules, disable debug
   endpoints.
2. **Data persistence**: Ensure PostgreSQL and RabbitMQ data volumes are correctly mounted and backed up.
3. **High availability**: Configure PostgreSQL replication and RabbitMQ clustering as needed.
4. **Monitoring & alerting**: Deploy Prometheus + Grafana (included in docker-compose-optional.yml).
5. **Log collection**: Integrate with ELK (included in docker-compose-optional.yml).
6. **Environment variables**: See [Environment Variables](../quickstart/environment) and replace development values with
   production values.

See [Security Policy](./security) for the production baseline checklist.

### How do I back up data?

PostgreSQL data backup:

```bash
# Full backup
podman exec dc3-postgres pg_dumpall -U dc3 > backup.sql

# Platform data only (exclude TimescaleDB time-series data)
podman exec dc3-postgres pg_dump -U dc3 \
  --schema=dc3_auth --schema=dc3_manager --schema=dc3_data > backup_platform.sql
```

For production, configure pgBackRest or scheduled pg_dump tasks with offsite storage.

---

## Driver Development

### How do I develop a new driver?

1. Read the [Driver Development Guide](../development/driver-authoring).
2. Copy the closest existing driver module under `dc3-driver/` as a template.
3. Implement the `read()`, `write()`, and (optionally) `subscribe()` methods required by the Driver SDK.
4. Add the driver service configuration to `dc3/docker-compose.yml`.
5. Write documentation (follow the format of existing driver doc pages).

### Does a driver have to be written in Java?

The Driver SDK itself is in Java, but you can also implement device access in any language via **MQTT bridging** or *
*HTTP proxy**. A non-Java program publishes data to an MQTT topic → the MQTT driver subscribes → data enters the
platform pipeline. However, this approach loses the SDK's built-in state management, automatic reconnection, and health
reporting capabilities.

---

## AI Capabilities

### What can AI do?

IoT DC3's Agentic Center (based on Spring AI) gives LLMs the following capabilities:

- **Device querying**: Natural language queries for device status, point values, and historical data.
- **Command issuance**: Let the AI write parameters to devices through conversation.
- **Alarm analysis**: AI analyzes alarm history and provides root-cause inference.
- **Data insights**: Trend analysis and anomaly detection on time-series data.

AI capabilities are exposed through the MCP (Model Context Protocol) and can be called directly by tools like Claude
Desktop, VS Code, and Cursor. See the [AI Overview](../ai/).

### Which LLM providers are supported?

Through Spring AI, all major model providers are theoretically supported: OpenAI, Anthropic Claude, Google Gemini,
Alibaba Tongyi Qianwen, Baidu ERNIE Bot, local Ollama models, and more. See the [Agentic Center](../ai/agentic) for
configuration details.

---

## Community & Contribution

### How do I get help?

1. Check the [Troubleshooting Guide](../guide/troubleshooting).
2. Search [GitHub Issues](https://github.com/pnoker/iot-dc3/issues) for similar problems.
3. Not found? Open a new issue with: version number, logs, reproduction steps, and environment info.

### How can I contribute?

See the [Contributing Guide](./contributing). All forms of contribution are welcome: bug reports, documentation
improvements, code contributions, and discussions.

### Is commercial support available?

The project currently operates as a community effort with no official commercial support. For enterprise-level support
needs, contact the maintainers through community channels.
