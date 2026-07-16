---
title: Driver Development
---

<script setup>
import DriverAuthoringStateDiagram from '../../.vitepress/theme/components/DriverAuthoringStateDiagram.vue'
import DriverAuthoringFlow1Diagram from '../../.vitepress/theme/components/DriverAuthoringFlow1Diagram.vue'
import DriverAuthoringFlow2Diagram from '../../.vitepress/theme/components/DriverAuthoringFlow2Diagram.vue'
import DriverAuthoringSeqDiagram from '../../.vitepress/theme/components/DriverAuthoringSeqDiagram.vue'
import DriverAuthoringFlow3Diagram from '../../.vitepress/theme/components/DriverAuthoringFlow3Diagram.vue'
</script>


# Driver Development

Drivers are the southbound I/O layer of IoT DC3. They bring heterogeneous protocol devices — Modbus, OPC UA, MQTT, S7,
BACnet, and more — into the platform's data and command planes through a single interface. This page walks through
deriving a new protocol driver from the `dc3-driver-virtual` template, then covers the driver lifecycle, read/write
scheduling, and the one routing identifier you must not change after going to production. By the end you'll have a
driver that registers, collects data, and accepts commands.

> You are here: you want to onboard devices speaking a protocol that no existing driver supports. If you only want to
> use an existing driver, start with the [Operation Manual](../operation/) and [Quick Start](../quickstart/). Next, see
> the [Command Plane](../architecture/command-plane) to understand how read/write commands flow back to devices.

Unless otherwise noted, run commands from the `iot-dc3` repository root.

## What a driver is: a Spring Boot service that aggregates 7 SPIs

A driver is a standalone Spring Boot service (`dc3-driver-<protocol>`). It does not talk directly to the management
center or the data center. Instead it inherits the `dc3-common-driver` SDK, which handles registration, scheduling,
RabbitMQ messaging, gRPC calls, and tenant context — so **you only implement the protocol logic**.

That logic is exposed through a single entry interface: `DriverCustomService`. It declares no methods of its own; it
aggregates 7 single-responsibility SPI sub-interfaces. Implementing this one interface puts all 7 concerns in your
hands:

| SPI sub-interface        | The question you must answer                                                                              |
|--------------------------|-----------------------------------------------------------------------------------------------------------|
| `DriverLifecycle`        | What to initialize when the process starts (`initial()`)? What to do on each custom cycle (`schedule()`)? |
| `DriverProtocol`         | How to read a point from the device (`read(...)`)? How to write a point (`write(...)`)?                   |
| `DriverCommand`          | How to run the custom commands defined in the template (`execute(...)`)?                                  |
| `DriverMetadataListener` | How to refresh local caches when device/point metadata changes (`event(...)`)?                            |
| `DriverHealth`           | Is the driver as a whole ONLINE / OFFLINE / FAULT / MAINTAIN?                                             |
| `DeviceHealth`           | How to determine the online state of a single device?                                                     |
| `DriverValidator`        | Is the driver/point configuration valid (`validate*`)? Can it generate simulated values?                  |

Source entry point: `dc3-common/dc3-common-driver/.../service/DriverCustomService.java` (a single `extends` line
stitches the 7 interfaces together). The `dc3-driver-virtual` template ships runnable example implementations for all 7,
which makes it the best starting point for a new driver.

::: tip Terminology alignment
**Attribute** comes from `dc3.driver.*-attribute` in the driver's `application.yml` and defines "which configuration
items this driver has". **Config** is the concrete value a given device fills in for those attributes, stored in the
management center. The driver registers attributes at startup and retrieves a device's config values at runtime through
`Map<String, AttributeBO>`.
:::

## Lifecycle: register (with retry) → initial → schedule

After the driver process starts, `DriverInitRunner` (an `ApplicationRunner`) runs a fixed bootstrap sequence: it first
registers itself and all attribute definitions with the management center, then calls your `initial()` for one-time
setup once registration succeeds, and finally lets the SDK wire up scheduled tasks (read scheduling, custom scheduling,
device health checks).

Registration goes over gRPC, and the management center may not be ready when the driver starts (rolling restarts, pod
rescheduling). So registration is not one-shot: `DriverInitRunner.registerWithRetry()` retries with **capped exponential
backoff** — starting at 2 seconds, doubling each time, capped at 30 seconds, up to 30 attempts. Only if all 30 fail does
it throw and exit. Without this retry, a brief blip in the management center would drag the driver into
CrashLoopBackOff.

<DriverAuthoringStateDiagram lang="en" />

Source: `dc3-common/dc3-common-driver/.../init/DriverInitRunner.java` (`REGISTER_MAX_ATTEMPTS=30`,
`REGISTER_INITIAL_BACKOFF=2s`, `REGISTER_MAX_BACKOFF=30s`). `initial()` runs only once at startup — use it to build
connection pools and subscriptions. `schedule()` fires on the cron defined in `dc3.driver.schedule.custom`.

## From template to new driver: four steps

The work for a new driver concentrates in four places: copy the template, edit `pom.xml`, edit `application.yml`, and
implement `DriverCustomService`. The diagram shows the overall path, expanded step by step afterward.

<DriverAuthoringFlow1Diagram lang="en" />

### Step 1: Copy the template and rename

Name the driver module `dc3-driver-<protocol>`, with the protocol name in kebab-case:

```bash
cp -r dc3-driver/dc3-driver-virtual dc3-driver/dc3-driver-knx
```

Then rename the Java package, the application class, and the custom service implementation class. The two key classes in
the template are:

| Class                            | Description                                                   |
|----------------------------------|---------------------------------------------------------------|
| `VirtualDriverApplication`       | Spring Boot application class                                 |
| `VirtualDriverCustomServiceImpl` | Protocol logic entry point (`implements DriverCustomService`) |

A new driver should use protocol-specific names — `KnxDriverApplication`, `KnxDriverCustomServiceImpl` — to avoid
duplicate class names across drivers. Put the application class and the implementation class under the same parent
package so component scanning picks up the `@Service`-annotated `DriverCustomService` implementation:

```java
@SpringBootApplication
public class KnxDriverApplication {
    public static void main(String[] args) {
        SpringApplication.run(KnxDriverApplication.class, args);
    }
}
```

### Step 2: Wire into the parent POM

Register the new module in the `<modules>` of `dc3-driver/pom.xml`:

```xml
<modules>
  <module>dc3-driver-knx</module>
  <!-- existing modules -->
</modules>
```

The new module's own `pom.xml` usually just inherits the driver parent module and adds the protocol library:

```xml
<parent>
  <groupId>io.github.pnoker</groupId>
  <artifactId>dc3-driver</artifactId>
  <version>2026.5.22</version>
</parent>

<artifactId>dc3-driver-knx</artifactId>
<packaging>jar</packaging>

<dependencies>
  <!-- Protocol library, e.g. calimero-core (KNX). Heavy protocol dependencies belong here only, not in dc3-common-driver -->
</dependencies>
```

The `dc3-driver` parent module already pulls in `dc3-common-driver` (the SDK) and the Spring Boot Maven Plugin, so you
don't need to declare them again.

### Step 3: Configure `application.yml`

`dc3.driver` is the driver's most important user-visible configuration. The SDK reads it at startup and registers it
with the management center, which uses it to render the device and point configuration forms. The example below follows
the real structure of `dc3-driver-virtual`, with KNX semantics in place of virtual's values:

```yaml
dc3:
  driver:
    tenant: default
    name: KNX Driver
    code: KnxDriver               # stable routing identifier, see constraints below
    type: DRIVER_CLIENT
    remark: @project.description@

    schedule:
      read:                       # read scheduling: periodically collect point values
        enabled: true
        cron: '0/30 * * * * ?'    # one round every 30 seconds
      custom:                     # custom scheduling: driver schedule() callback
        enabled: true
        cron: '0/5 * * * * ?'
    health:
      device:                     # device health reporting
        enabled: true
        cron: '0/15 * * * * ?'
        timeout: 45               # device status lease TTL (seconds)
        timeout-unit: SECONDS

    driver-attribute:             # driver-level attributes: filled once per device instance
      - attribute-name: Host
        attribute-code: host
        attribute-type-flag: STRING
        default-value: localhost
        remark: KNX/IP gateway host
      - attribute-name: Port
        attribute-code: port
        attribute-type-flag: INT
        default-value: 3671
        remark: KNX/IP gateway port

    point-attribute:              # point-level attributes: filled once per point
      - attribute-name: Group Address
        attribute-code: groupAddress
        attribute-type-flag: STRING
        default-value: 1/0/1
        remark: KNX group address

spring:
  application:
    name: @project.artifactId@
  profiles:
    active:
      - ${NODE_ENV:dev}

logging:
  file:
    name: dc3/logs/driver/knx/${spring.application.name}.log
```

What the attribute fields mean (the prose above builds the mental model; this table is a quick reference):

| Field                 | Description                                                                                                                     |
|-----------------------|---------------------------------------------------------------------------------------------------------------------------------|
| `attribute-name`      | UI display name; driver metadata is conventionally in English                                                                   |
| `attribute-code`      | The stable key the protocol implementation reads, e.g. `host`, `port`, `objectType`                                             |
| `attribute-type-flag` | Attribute type; `AttributeTypeEnum` has 8 values: `STRING` / `BYTE` / `SHORT` / `INT` / `LONG` / `FLOAT` / `DOUBLE` / `BOOLEAN` |
| `default-value`       | Default value                                                                                                                   |
| `remark`              | Description text; English recommended                                                                                           |

::: warning The toggle field is named enabled
The scheduling toggle field is named `enabled`. `DriverScheduleServiceImpl` reads `getRead().getEnabled()` /
`getCustom().getEnabled()` / `device.getEnabled()`, bound to the `private Boolean enabled` inside `DriverProperties`.
Spring's relaxed binding will not map `enable` to `enabled` — they are different property names. The
`dc3-driver-virtual` template writes `enable`, which actually has no effect. For a new driver, use `enabled`. Device
health's `enabled` defaults to `false` and must be explicitly set to `true` to turn it on.
:::

The attribute registration chain: `dc3.driver` in `application.yml` → SDK parses it into a `RegisterBO` → submitted to
the management center over gRPC. The diagram shows the entity relationships of this flow:

<DriverAuthoringFlow2Diagram lang="en" />

### Step 4: Implement `DriverCustomService`

The core protocol logic lives in the `DriverCustomService` implementation. `read(...)` returns a single
`ReadPointValue`, and `write(...)` returns a `Boolean`. That is the entire protocol contract's outward commitment (
source `DriverProtocol.java`):

```java
@Slf4j
@Service
public class KnxDriverCustomServiceImpl implements DriverCustomService {

    @Resource
    private DriverMetadata driverMetadata;

    @Resource
    private DriverSenderService driverSenderService;

    @Override
    public void initial() {
        // One-time initialization: set up the protocol stack, connection pool, subscriptions
    }

    @Override
    public void schedule() {
        // Custom periodic task, e.g. periodically report device status (with TTL)
        driverMetadata.getDeviceIds().forEach(deviceId ->
                driverSenderService.deviceStatusSender(
                        deviceId, EntityStatusEnum.ONLINE, 45, TimeUnit.SECONDS));
    }

    @Override
    public void event(MetadataEventDTO metadataEvent) {
        // React to device/point metadata changes (ADD/UPDATE/DELETE), refreshing local cache or subscriptions
    }

    @Override
    public ReadPointValue read(Map<String, AttributeBO> driverConfig,
                               Map<String, AttributeBO> pointConfig,
                               DeviceBO device,
                               PointBO point) {
        String host = driverConfig.get("host").getValue(String.class);
        Integer port = driverConfig.get("port").getValue(Integer.class);
        String groupAddress = pointConfig.get("groupAddress").getValue(String.class);

        // Perform the protocol read, returning the raw string value (example value "0")
        return new ReadPointValue(device, point, "0");
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig,
                         Map<String, AttributeBO> pointConfig,
                         DeviceBO device,
                         PointBO point,
                         WritePointValue writePointValue) {
        // Perform the protocol write; return true only when the device confirms the write succeeded
        return true;
    }
}
```

::: danger Do not swallow exceptions on read/write failure
Throwing an exception from `read()` / `write()` is the SDK's agreed failure signal — the SDK logs it and acks or nacks
the command on RabbitMQ. When a write command fails, the result does not echo back the written value (
`responseValue=null`), to avoid a "false success". And a single point's read failure should not bring down the whole
collection round.
:::

## Read/write scheduling: how data goes out, how commands come in

A driver has two data flows running in opposite directions. The SDK orchestrates both; you only fill in the protocol
implementation.

**Read (outbound)**: Quartz's `DriverReadScheduleJob` fires on the cron in `dc3.driver.schedule.read`, iterates this
driver's devices from the `DriverMetadata` cache, submits a read task per device (thread pool), calls your `read()` to
get a `ReadPointValue`, and the SDK then sends it to the data center over RabbitMQ. You do **not** write the RabbitMQ or
gRPC plumbing yourself.

**Write (inbound)**: the data center dispatches read/write commands to this driver's command queue over RabbitMQ.
`PointCommandReceiver` deduplicates them, locks per device, then calls your `read()` or `write()` in turn and sends the
result back to the data center.

<DriverAuthoringSeqDiagram lang="en" />

Inbound write handling on the driver side is not a bare `write()` call — it is a pipeline with validation,
deduplication, and locking. The diagram below expands `PointCommandReceiver`'s pipeline, including error paths:

<DriverAuthoringFlow3Diagram lang="en" />

The send side goes through `DriverSenderService` (source `DriverSenderService.java`). Common methods:

| Method                                                                | Purpose                                      |
|-----------------------------------------------------------------------|----------------------------------------------|
| `pointValueSender(PointValue)` / `pointValueSender(List<PointValue>)` | Send a single or batched set of point values |
| `deviceStatusSender(deviceId, status)`                                | Report device status (default TTL)           |
| `deviceStatusSender(deviceId, status, timeout, unit)`                 | Report device status with TTL                |
| `driverAlarmSender(String)`                                           | Report a driver-level alarm                  |
| `deviceAlarmSender(deviceId, String)`                                 | Report a device-level alarm                  |
| `eventReportSender(EventReportDTO)`                                   | Report a device event                        |
| `pointCommandResultSender(...)` / `commandResultSender(...)`          | Acknowledge command results                  |

The `status` values are defined in `EntityStatusEnum`: `ONLINE(0)` / `OFFLINE(1)` / `MAINTAIN(2)` / `FAULT(3)`.

::: warning The device status TTL must be greater than the read cycle
Device status is reported as a "lease": if it is not renewed before expiry, the device is judged offline. The TTL must
be **greater than** the status-report or read cycle, otherwise the device will be judged offline between two heartbeats
and flap repeatedly. For example, with a read cron of `0/30 * * * * ?` (every 30 seconds), the TTL should be ≥ 25
seconds. The template's default device health `timeout: 45` seconds leaves plenty of margin.
:::

## Naming and routing: the identifier you must not change

Driver routing involves three identifiers. Telling them apart avoids a trap you cannot undo after going to production:

| Identifier                | Source                                | Purpose                                                                                 |
|---------------------------|---------------------------------------|-----------------------------------------------------------------------------------------|
| `dc3.driver.code`         | `application.yml`                     | Unique driver-type code; the management center uses it to identify the driver type      |
| `dc3.driver.service`      | Auto-derived or explicitly overridden | Driver instance routing identifier, used for the RabbitMQ command queue and routing key |
| `spring.application.name` | Maven artifactId                      | Log file name, Actuator metadata, etc.                                                  |

::: danger dc3.driver.code is a stable identifier; changing it requires migration
Once in production, `dc3.driver.code` must not be changed casually. It is registered with the management center as the
driverCode and binds all metadata of that driver type — changing it amounts to swapping in a new driver type, all
onboarded devices will be lost, and a data migration plan is mandatory. (The RabbitMQ command queue and routing key are
built from `dc3.driver.service`, not `code` — see the table above.)
:::

## Build, run, and smoke test

To run locally, first load the environment variables, so the local Java process points at the dependency ports Compose
publishes to localhost:

```bash
source dc3/env/dev.env.sh
```

Build the new driver and its dependencies, then run:

::: code-group

```bash [Build]
mvn -s .mvn/settings.xml clean package -pl dc3-driver/dc3-driver-knx -am
```

```bash [Run]
java -jar dc3-driver/dc3-driver-knx/target/dc3-driver-knx.jar
```

:::

In development the driver auto-registers with the management center. Check the driver logs and confirm an event like
`Driver register succeeded` appears — that means registration worked. (During retries it prints
`Driver register failed on attempt n/30, retrying...`.)

Run an end-to-end smoke test along the golden path (HTTP paths and fields come from the gateway contract; example values
are marked as examples):

1. On the management side, create the driver, profile, point, and device, and fill in config values for the driver
   attributes `host`/`port` and the point attribute `groupAddress`.
2. Wait for one read cycle (30 seconds by default).
3. Fetch the latest point value to confirm that `read()`'s collection has been persisted:

```bash
# Example: deviceId/pointId are example values
curl -X POST http://localhost:8000/api/v3/data/point_value/latest \
  -H 'X-Auth-Tenant: default' \
  -H 'X-Auth-Login: dc3' \
  -H 'X-Auth-Token: <token>' \
  -H 'Content-Type: application/json' \
  -d '{"deviceId": 1, "pointId": 1, "page": {"current": 1, "size": 10}}'
```

4. Dispatch a write command to a writable point to confirm `write()` is invoked and acknowledged:

```bash
curl -X POST http://localhost:8000/api/v3/data/point_command/write \
  -H 'X-Auth-Tenant: default' \
  -H 'X-Auth-Login: dc3' \
  -H 'X-Auth-Token: <token>' \
  -H 'Content-Type: application/json' \
  -d '{"deviceId": 1, "pointId": 1, "value": "42"}'
```

The endpoint returns a `commandId`. Use it to query the command history and check the execution status (
`PointCommandHistoryVO`'s `status` takes `SUCCESS`/`FAILED` etc.; on a successful write, `responseValue` echoes the
written value):

```bash
curl -X GET 'http://localhost:8000/api/v3/data/point_command_history/get_by_command_id?commandId=<commandId>' \
  -H 'X-Auth-Tenant: default' \
  -H 'X-Auth-Login: dc3' \
  -H 'X-Auth-Token: <token>'
```

For the full command lifecycle and acknowledgment semantics, see the [Command Plane](../architecture/command-plane).

::: info Where the auth headers come from
All protected endpoints require `X-Auth-Tenant` / `X-Auth-Login` / `X-Auth-Token`. Get the token by fetching the salt
via `POST /api/v3/auth/token/salt` and exchanging it via `POST /api/v3/auth/token/generate` (valid for 12 hours). See
the [API Documentation](./api-documentation) for details.
:::

## FAQ

| Problem                                     | Root cause and handling                                                                                                                                                    |
|---------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Driver code conflict                        | Duplicate `dc3.driver.code` — keep it globally unique and stable; do not change the code of an already-deployed driver                                                     |
| `DriverCustomService` not loaded            | The implementation class lacks `@Service`, or sits outside the application class's component-scan scope                                                                    |
| Registration keeps retrying without success | Management center not ready or gRPC unreachable — check the `Driver register failed on attempt n/30` logs, verify `CENTER_MANAGER_HOST` and the management center's health |
| `read` returns empty or throws              | Do not swallow exceptions; let the logs surface the protocol error. A single-point failure should not bring down the whole round                                           |
| Devices frequently go offline (flap)        | The status TTL is smaller than the read/report cycle — increase the TTL or shorten the schedule cycle                                                                      |
| Reads happen but the data page has no value | Check RabbitMQ connectivity, data center logs, and tenant context                                                                                                          |
| Metadata changes have no effect             | Update the local protocol client, subscriptions, or cache inside `event(...)`                                                                                              |
| Heavy protocol dependency                   | Put it only in the specific driver module's `pom.xml`, not in `dc3-common-driver`                                                                                          |

## Further reading

- [Command Plane](../architecture/command-plane) — how read/write commands are dispatched, deduplicated, locked, and
  acknowledged, and how they connect to this page's `read()`/`write()`
- [Module Map](../architecture/modules) — the full picture of the 28 driver modules and where the `dc3-common-driver`
  SDK sits in the dependency tree
- [Domain Model](../architecture/domain-model) — the fields and boundaries of Profile / Point / Device and the three
  layers Param/Attribute/Config
- [API Documentation](./api-documentation) — the auth flow, gateway contract, and OpenAPI
- [Troubleshooting](../guide/troubleshooting) — issues with startup dependencies, ports, and environment variables
