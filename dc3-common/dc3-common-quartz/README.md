# DC3 Common Quartz

## Overview

`dc3-common-quartz` is the shared Quartz scheduler module of the IoT DC3 platform. It provides a reusable `QuartzService` for programmatically creating, updating, pausing, and
triggering scheduled jobs across services, primarily used in the Manager Center for platform-level periodic tasks.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-quartz
- **Version**: 2026.5.5

## Key Components

| Component                   | Purpose                                                                           |
|-----------------------------|-----------------------------------------------------------------------------------|
| `QuartzService`             | Lifecycle management for Quartz jobs: add, update, delete, pause, resume, trigger |
| `ActiveQuartzProfileConfig` | Conditionally activates Quartz config based on active profile                     |

## Usage

```java
// Schedule a job
quartzService.createJob(MyJob.class, "jobName", "groupName", "0 0 * * * ?");

// Update cron expression
quartzService.updateJob("jobName", "groupName", "0 0/30 * * * ?");
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

