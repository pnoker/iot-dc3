# DC3 Common Quartz

## Overview

`dc3-common-quartz` is the shared Quartz scheduler module of the IoT DC3 platform. It provides a reusable
`QuartzService` for programmatically registering scheduled jobs across services, primarily used by Data, Manager,
MQTT, and Driver modules for periodic tasks.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-quartz
- **Version**: 2026.5.22

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
mvn -s ../../.mvn/settings.xml clean package
```

## Related Modules

- `dc3-common-manager` — Uses `QuartzService` for hourly data-volume statistics jobs

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)
