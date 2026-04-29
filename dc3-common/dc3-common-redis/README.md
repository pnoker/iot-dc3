# DC3 Common Redis

## Overview

`dc3-common-redis` is the shared Redis configuration module of the IoT DC3 platform. It provides `RedisTemplate` and Spring Cache configuration, a Redis-backed point value entity,
and initialization logic for Redis-dependent services.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-redis
- **Version**: 2026.4.29

## Key Components

| Component             | Purpose                                                                              |
|-----------------------|--------------------------------------------------------------------------------------|
| `RedisTemplateConfig` | Configures `RedisTemplate<String, Object>` with Jackson serializer                   |
| `RedisCacheConfig`    | Configures `RedisCacheManager` for Spring `@Cacheable` support with configurable TTL |
| `RedisPointValueDO`   | Redis-specific entity for caching latest point values                                |
| `RedisInitRunner`     | Startup initialization for Redis-dependent checks                                    |

## Configuration Properties

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:dc3-redis}
      port: ${REDIS_PORT:36379}
      password: ${REDIS_PASSWORD:dc3dc3dc3}
      database: 2    # database index varies per service

  cache:
    redis:
      time-to-live: ${CACHE_REDIS_TIME_TO_LIVE:5S}
```

## Usage

Use Spring's standard caching annotations once this module is on the classpath:

```java
@Cacheable(cacheNames = "driver", key = "#id")
public DriverBO selectById(Long id) { ... }

@CacheEvict(cacheNames = "driver", key = "#entityBO.id")
public void update(DriverBO entityBO) { ... }
```

## Build Instructions

```bash
mvn -s ../../.mvn/settings.xml clean package
```

## Related Modules

- `dc3-common-manager`, `dc3-common-auth`, `dc3-common-data` — Include this module for Redis caching

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)

