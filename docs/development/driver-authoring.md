# Authoring a New Driver

This guide walks you through creating a new device driver from scratch using
[`dc3-driver-virtual`](https://github.com/pnoker/iot-dc3/tree/release/dc3-driver/dc3-driver-virtual) as a template.
Drivers connect the
DC3 platform to physical devices via a specific protocol (Modbus, OPC, MQTT, S7, …) and are
the southbound I/O layer of the system.

If you only want to *use* an existing driver, see [快速开始](../quickstart/index.md) instead.

Unless stated otherwise, run commands from the repository root.

## What a driver does

A driver is a Spring Boot service that:

1. **Registers** itself with `dc3-center-manager` on startup (driver name, code, attribute
   schema, point attribute schema). Manager uses this to render the device-config UI.
2. **Reads** point values from physical devices on a configurable cron and pushes them to
   `dc3-center-data` via RabbitMQ.
3. **Writes** values to devices when commands arrive from `dc3-center-data` over RabbitMQ.
4. **Reports device status** (online / offline / fault / maintain) on a configurable cron.
5. **Reacts** to metadata change events (device / point added, removed, updated) so it can
   adjust its in-memory state without restarting.

All five behaviours are exposed through one SPI: `DriverCustomService`. The SDK
(`dc3-common-driver`) takes care of registration, RabbitMQ wiring, scheduling, and gRPC
plumbing — your driver only implements the protocol-specific parts.

## Prerequisites

- Java 21 + Maven 3.9+
- A running `dc3-center-manager`, `dc3-center-data`, `dc3-center-auth`, RabbitMQ, and
  Postgres. Easiest: `make dev-all`. See [快速开始](../quickstart/index.md).
- Familiarity with whichever device protocol your driver implements.

## Step 1 — Create the module

Pick a short, descriptive name in kebab-case: `dc3-driver-<protocol>` (e.g. `dc3-driver-bacnet`).

```bash
cd /path/to/iot-dc3
cp -r dc3-driver/dc3-driver-virtual dc3-driver/dc3-driver-bacnet
cd dc3-driver/dc3-driver-bacnet
```

Then rename the Java package and main class to match. The `dc3-driver-virtual` template has
a single `service/impl/VirtualDriverCustomServiceImpl.java` and a `VirtualDriverApplication.java`.
Rename both to your driver, and keep the custom service implementation name protocol-specific
(for example, `BacnetDriverCustomServiceImpl`) so static analysis tools can scan every driver
module without filtering duplicate fully qualified class names.

## Step 2 — Wire into the parent POM

Add your module to [`dc3-driver/pom.xml`](https://github.com/pnoker/iot-dc3/blob/release/dc3-driver/pom.xml)
`<modules>`:

```xml
<modules>
  <module>dc3-driver-bacnet</module>
  <!-- existing entries -->
</modules>
```

Your module's own `pom.xml` only needs:

```xml
<parent>
  <groupId>io.github.pnoker</groupId>
  <artifactId>dc3-driver</artifactId>
  <version>2026.5.22</version>
</parent>
<artifactId>dc3-driver-bacnet</artifactId>
<packaging>jar</packaging>

<dependencies>
  <!-- protocol-specific libraries go here, e.g. bacnet4j -->
</dependencies>
```

The parent already pulls in `dc3-common-driver` and the Spring Boot Maven plugin.

## Step 3 — Application class

Drop a single `@SpringBootApplication`-annotated entry point. Same package as your
protocol-specific `DriverCustomService` implementation so default component scanning picks both up:

```java
@SpringBootApplication
public class BacnetDriverApplication {
    public static void main(String[] args) {
        SpringApplication.run(BacnetDriverApplication.class, args);
    }
}
```

## Step 4 — `application.yml`

This is the most important file: the keys under `dc3.driver:` are read by the SDK at startup
and pushed to Manager during registration. They define the **schema** of attributes that
operators will fill in when configuring a device or point in the UI.

```yaml
dc3:
  driver:
    tenant: default
    name: BACnet驱动            # human-readable name
    code: BacnetDriver          # unique routing key — see "Naming" below
    type: DRIVER_CLIENT
    remark: @project.description@

    schedule:
      read:                     # periodic read of every point
        enable: true
        cron: '0/30 * * * * ?'  # every 30s
      custom:                   # your DriverCustomService#schedule() callback
        enable: true
        cron: '0/5 * * * * ?'

    driver-attribute:           # per-device config (one row per device)
      - attribute-name: 主机
        attribute-code: host
        attribute-type-flag: STRING
        default-value: localhost
        remark: BACnet host
      - attribute-name: 端口
        attribute-code: port
        attribute-type-flag: INT
        default-value: 47808
        remark: BACnet port

    point-attribute:            # per-point config (one row per point on the device)
      - attribute-name: 对象类型
        attribute-code: objectType
        attribute-type-flag: STRING
        default-value: analog-input
        remark: BACnet object type
      - attribute-name: 实例号
        attribute-code: instance
        attribute-type-flag: INT
        default-value: 0
        remark: Object instance number

spring:
  application:
    name: @project.artifactId@
  profiles:
    active:
      - ${NODE_ENV:dev}

logging:
  file:
    name: dc3/logs/driver/bacnet/${spring.application.name}.log
```

You'll also typically copy `application-dev.yml` / `-pre.yml` / `-pro.yml` from
`dc3-driver-virtual` and adjust the schedule cron. **Never hardcode `localhost`** —
everything goes through `${ENV:default}` placeholders. See
[`dc3/env/dev.env.sh`](https://github.com/pnoker/iot-dc3/blob/release/dc3/env/dev.env.sh) for the env-var contract.

### Attribute types

`attribute-type-flag` is one of: `STRING`, `INT`, `LONG`, `DOUBLE`, `FLOAT`, `BOOLEAN`,
`BYTE`, `SHORT`. Pick what makes sense; the value reaches your `read` / `write` methods as
an `AttributeBO` whose typed accessor (`.getValue(Type)`) handles parsing.

## Step 5 — Implement `DriverCustomService`

Five methods. Drop your protocol logic in:

```java
@Slf4j
@Service
public class BacnetDriverCustomServiceImpl implements DriverCustomService {

    @Resource private DriverMetadata driverMetadata;
    @Resource private DriverSenderService driverSenderService;

    /** Called once at startup. Open connection pools, init protocol stacks, etc. */
    @Override
    public void initial() { /* ... */ }

    /**
     * Called on the cron defined by `dc3.driver.schedule.custom.cron`. Use it for anything
     * that doesn't fit `read` (e.g. heartbeat, status polling, reconnect retries).
     * Typically you push status updates here:
     */
    @Override
    public void schedule() {
        driverMetadata.getDeviceIds().forEach(id ->
            driverSenderService.deviceStatusSender(id, DeviceStatusEnum.ONLINE, 25, TimeUnit.SECONDS));
    }

    /**
     * Manager fires this whenever a device or point is added/updated/deleted. Use it to
     * drop cached protocol clients, rebuild subscriptions, etc.
     */
    @Override
    public void event(MetadataEventDTO metadataEvent) { /* ... */ }

    /**
     * Read a single point value. Called once per (device, point) on the
     * `dc3.driver.schedule.read.cron`. driverConfig holds the device-level attributes the
     * operator filled in; pointConfig holds the per-point attributes.
     *
     * Return ReadPointValue with the raw string; the framework will run any configured
     * scaling / unit conversion before persisting.
     */
    @Override
    public ReadPointValue read(Map<String, AttributeBO> driverConfig,
                       Map<String, AttributeBO> pointConfig,
                       DeviceBO device, PointBO point) {
        String host = driverConfig.get("host").getValue(String.class);
        int port    = driverConfig.get("port").getValue(Integer.class);
        // ... protocol-specific read, return new ReadPointValue(device, point, "<raw value>")
    }

    /**
     * Write a value to a point. Called when an external command arrives over RabbitMQ.
     * Return false (or throw) if the protocol does not support writes for this point.
     */
    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig,
                         Map<String, AttributeBO> pointConfig,
                         DeviceBO device, PointBO point, WritePointValue writePointValue) {
        // ... protocol-specific write
        return true;
    }
}
```

## Step 6 — Push values & status from the SDK

You don't need to write any RabbitMQ or gRPC plumbing. Use the injected
`DriverSenderService`:

| Method                                                                    | Purpose                                                                                                                                                                                                         |
|---------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `pointValueSender(PointValue)` / `pointValueSender(List<PointValue>)`     | Push a single (or batch) of values to `dc3-center-data`. Use this for *push-style* drivers (MQTT, OPC subscriptions); polling drivers usually just return `ReadPointValue` from `read` and let the SDK send it. |
| `deviceStatusSender(deviceId, status)` / `(deviceId, status, ttl, unit)`  | Report a device as ONLINE / OFFLINE / FAULT / MAINTAIN. The TTL drives auto-OFFLINE on silence (default ~25 s if you use the no-TTL overload).                                                                  |
| `driverEventSender(DriverEventDTO)` / `deviceEventSender(DeviceEventDTO)` | Emit structured driver- or device-scoped events that show up in the dashboard alert feed.                                                                                                                       |
| `driverAlarmSender(String)` / `deviceAlarmSender(deviceId, String)`       | Quick path for alarm-style events with just a human-readable reason (e.g. `"OPC UA session dropped"`).                                                                                                          |

Status TTLs: pick a number larger than your `schedule` cron interval. Otherwise the device
flips to OFFLINE between two heartbeats. For example, with `cron: '0/5 * * * * ?'` (every
5 s), `25 SECONDS` gives a 5× safety margin.

## Step 7 — Driver naming and routing

Three identifiers participate in driver routing:

- **`dc3.driver.code`** (from `application.yml`) — the unique driver-type key Manager uses to
  reject duplicate registrations. Validated by a strict regex: `^[A-Za-z0-9][...]{1,31}$`.
- **`dc3.driver.service`** (auto-derived; can be overridden) — the per-instance routing
  identifier used as the suffix on RabbitMQ command queues (`dc3.q.command.driver.<service>`)
  and routing keys (`dc3.r.command.driver.<service>`). See
  [
  `RabbitConstant`](https://github.com/pnoker/iot-dc3/blob/release/dc3-common/dc3-common-constant/src/main/java/io/github/pnoker/common/constant/driver/RabbitConstant.java).
- **`spring.application.name`** (`@project.artifactId@`) — controls log filenames and
  Actuator metadata. Has no routing implication.

**Pick `dc3.driver.code` once and never change it.** Renaming it after the driver has any
devices configured against it requires a migration of `dc3_driver`,
`dc3_driver_attribute`, and the RabbitMQ bindings.

## Step 8 — Build and run

```bash
# Build everything (runs protobuf-maven-plugin → builds the SDK → builds your driver)
source dc3/env/dev.env.sh
mvn -s .mvn/settings.xml clean package -pl dc3-driver/dc3-driver-bacnet -am

# Or via the project Makefile
make package
```

Run it standalone (with center services already up via `make dev-db dev-optional`):

```bash
java -jar dc3-driver/dc3-driver-bacnet/target/dc3-driver-bacnet.jar
```

In dev mode the service registers itself with `dc3-center-manager` automatically — watch
the manager logs for `Driver registered: BacnetDriver`. Once registered, it shows up under
the **Driver** menu in the UI; you can then create a **Profile**, attach it to **Devices**,
and bind **Points**.

## Step 9 — Smoke-test the loop

1. Create a device via the UI → fill in `host` / `port` (your `driver-attribute` schema).
2. Bind a point → fill in `objectType` / `instance` (your `point-attribute` schema).
3. Wait one `dc3.driver.schedule.read.cron` cycle.
4. Inspect a value either in the **Live values** dashboard, in the database
   (`select * from dc3_point_value order by create_time desc limit 5;`), or with `curl`
   against `dc3-gateway` (`/api/v3/data/point_value/...`).
5. Trigger a write via the UI's **Send command** button — your `write` method should fire.

## Common gotchas

- **`dc3.driver.code` collides with another driver.** Manager rejects the registration. Pick a
  unique code; convention is PascalCase ending in `Driver`.
- **`DriverCustomService` implementation not picked up.** The class needs `@Service` *and* must live in
  a package scanned from your `@SpringBootApplication` class — i.e. same package or below.
- **`read` returns null / throws.** The framework logs and skips the point for that cycle;
  it does *not* fail the whole driver. Don't catch and return null silently — let the
  exception surface so you see it in logs.
- **Status flips OFFLINE every cycle.** Your TTL is shorter than the schedule interval.
  Bump the TTL or shorten the schedule.
- **Points read but never appear in the dashboard.** The driver is sending values to
  RabbitMQ, but `dc3-center-data` isn't running, or RabbitMQ credentials are wrong. Check
  `RABBITMQ_HOST` env vars and the data-center logs.
- **Hot-reload of metadata.** Don't cache `DeviceBO` / `PointBO` references across calls
  without listening on `event(...)`. Manager mutates these on the fly.
- **Heavy protocol library.** If your driver pulls in a fat protocol stack
  (e.g. `bacnet4j`, `j2mod`), prefer adding it to your driver's `pom.xml`, not to the
  shared `dc3-common-driver`. Drivers are runnable JARs and should not bloat each other.

## Reference

- SDK base interface: [
  `DriverCustomService.java`](https://github.com/pnoker/iot-dc3/blob/release/dc3-common/dc3-common-driver/src/main/java/io/github/pnoker/common/driver/service/DriverCustomService.java)
- Sender service: [
  `DriverSenderService.java`](https://github.com/pnoker/iot-dc3/blob/release/dc3-common/dc3-common-driver/src/main/java/io/github/pnoker/common/driver/service/DriverSenderService.java)
- Working examples: `dc3-driver-virtual` (simplest), `dc3-driver-modbus-tcp` (TCP polling),
  `dc3-driver-mqtt` (push-style subscriber), `dc3-driver-listening-virtual` (push-style
  template), `dc3-driver-opc-ua` (subscription model).
- Architecture overview: [模块与依赖](../architecture/modules.md)
- Troubleshooting: [故障排查](../guide/troubleshooting.md)
