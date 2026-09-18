# DC3 Common Quartz

## Overview

`dc3-common-quartz` is the shared Quartz scheduler module of the IoT DC3 platform. It provides a reusable
`QuartzService` for programmatically registering scheduled jobs across services, primarily used by Data, Manager, MQTT,
and Driver modules for periodic tasks.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-quartz

## Key Components

| Component                   | Purpose                                                                      |
|-----------------------------|------------------------------------------------------------------------------|
| `QuartzConfig`              | Auto-configures the shared `QuartzService` bean                              |
| `QuartzService`             | Registers cron / fixed-interval Quartz jobs and controls scheduler lifecycle |
| `ActiveQuartzProfileConfig` | Activates the `quartz` profile unless `dc3.quartz.auto-profile=false` is set |

## Usage

```java
// Register a cron job. Re-registering the same group/name replaces the existing job.
quartzService.createJobWithCron("groupName", "jobName", "0 0 * * * ?", MyJob.class);

// Register a fixed-interval job.
quartzService.createJobWithInterval("groupName", "jobName", 5, DateBuilder.IntervalUnit.SECOND, MyJob.class);

quartzService.startScheduler();
```

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-quartz -am package
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-quartz -am test
```

## Related Modules

- `dc3-common-manager` — Uses `QuartzService` for its hourly maintenance job
- `dc3-common-data` / `dc3-common-mqtt` / `dc3-common-driver` — periodic persistence, message, and scheduling tasks
