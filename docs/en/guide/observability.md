---
title: Observability
---

<script setup>
import ObservabilityDiagram from '../../.vitepress/theme/components/ObservabilityDiagram.vue'
</script>


# Observability

Log aggregation and metrics monitoring in IoT DC3 are an **optional** stack: a single command, `make up-optional`,
starts EMQX, ELK (Elasticsearch + Logstash + Kibana), Prometheus, and Grafana. This page covers what each component
does, the port it exposes, how to feed service logs into Kibana and service metrics into Grafana, and the environment
variables that tune heap memory and toggle APM.

> You are here: you can already [deploy and start the platform](./usage), and now want to add log search and metrics
> dashboards to a running environment.

::: info The observability stack is optional
This stack is not part of the default startup set. Once `make up-db` (PostgreSQL + RabbitMQ) and the application stack
are up, the platform runs. The observability components need a separate `make up-optional` to start, add memory
overhead, and are enabled on demand.
:::

## Why it's a separate stack

Observability is split out of the core stack so that "getting the platform running" and "observing it" stay independent.
Someone evaluating the golden path shouldn't pay hundreds of megabytes of heap for Elasticsearch they won't look at.
When operators need to troubleshoot or watch trends, they layer this stack on top. It lives in
`dc3/docker-compose-optional.yml`, alongside the three core stacks (db/dev/app), and shares the same `dc3net` network,
so services can reach each other by container alias (for example `dc3-elasticsearch` and `dc3-prometheus`).

The stack handles two jobs:

- **Logs** — Logstash collects and normalizes the JSON file logs emitted by services, writes them into Elasticsearch,
  and Kibana lets you search and correlate them by field.
- **Metrics** — Prometheus scrapes metrics from services and exporters on a schedule, and Grafana renders them on
  dashboards.

EMQX is bundled in as well, acting as the MQTT broker for MQTT-style drivers and direct device connections.

## Components and ports

`make up-optional` starts the containers below at once. The ports are the host-published ports, bound to `127.0.0.1` by
default and controlled by `DC3_BIND_HOST`. The table is a quick reference; each component's role is covered after it.

| Container               | Role                                        | Host port (default)                                        | Control variable                                 |
|-------------------------|---------------------------------------------|------------------------------------------------------------|--------------------------------------------------|
| `dc3-emqx`              | MQTT broker + Dashboard                     | MQTT `31883`, Dashboard `18083`, WS `38083`, MQTTS `38883` | `DC3_EMQX_MQTT_PORT` / `DC3_EMQX_DASHBOARD_PORT` |
| `dc3-elasticsearch`     | Log storage and search engine               | internal `9200` (not published)                            | `DC3_ES_JAVA_OPTS`                               |
| `dc3-logstash`          | Log collection and normalization pipeline   | internal (not published)                                   | `DC3_LS_JAVA_OPTS`                               |
| `dc3-kibana`            | Log search and visualization UI             | `5601`                                                     | `DC3_KIBANA_PORT`                                |
| `dc3-apm`               | APM (application performance) data receiver | internal (not published)                                   | `APM_AGENT_ENABLE` (application-side toggle)     |
| `dc3-prometheus`        | Metrics scraping and time-series storage    | internal `9090` (not published), 7d retention              | —                                                |
| `dc3-postgres-exporter` | PostgreSQL metrics exporter                 | internal (not published)                                   | —                                                |
| `dc3-nginx-exporter`    | Frontend nginx metrics exporter             | internal (not published)                                   | —                                                |
| `dc3-grafana`           | Metrics dashboard UI                        | `3000`                                                     | `DC3_GRAFANA_PORT` / `GF_SERVER_ROOT_URL`        |

::: tip Only want a few of them?
You don't have to start the whole stack. Filter with `SERVICES` — for example, to start only monitoring:

```bash
make up STACK=optional SERVICES="prometheus grafana"
```

:::

EMQX's ports also show up in [Deployment modes and image registries](./usage): MQTT drivers connect to `31883`, and
operators sign in to the Dashboard on `18083` to inspect connections. Kibana (`5601`) and Grafana (`3000`) are the two
human-facing entry points — Kibana for searching logs, Grafana for viewing metrics. Elasticsearch, Logstash, APM,
Prometheus, and the two exporters **publish no ports to the host** and talk only within `dc3net`. They're backend
pipelines, not something you open directly.

## How logs are ingested (ELK)

Services write logs as JSON files (see [Logging conventions](./logging) for the message style). The output directory is
shared through a Docker volume named `logs`: the core stack writes into this volume, and Logstash mounts it at
`/usr/share/logstash/dc3/logs` to read from it. Logstash parses and tags the logs, writes them to Elasticsearch, and you
search them in Kibana.

<ObservabilityDiagram lang="en" />

To run the chain:

1. Make sure the core stack (dev or app) is running and already writing into the `logs` volume.
2. Run `make up-optional` to start ELK. Logstash reads from the `logs` volume automatically.
3. Open `http://localhost:5601` in a browser to reach Kibana, and search by fields like service name, `tenantId`, or
   event name.

::: warning Elasticsearch is memory-hungry — tune the heap first
The JVM heaps for Elasticsearch and Logstash default to small values so they start on a dev machine: `DC3_ES_JAVA_OPTS`
defaults to `-Xms512m -Xmx512m`, and `DC3_LS_JAVA_OPTS` to `-Xms256m -Xmx256m`. In production or under heavy log volume,
raise them to fit the machine, for example:

```bash
DC3_ES_JAVA_OPTS="-Xms2g -Xmx2g" make up-optional
```

:::

### APM is off by default

The `dc3-apm` container starts with ELK, but **whether the application reports APM data depends on `APM_AGENT_ENABLE`,
which defaults to `false`**. That variable lives in the core stack (`docker-compose.yml` / `docker-compose-dev.yml`) and
controls whether services attach the Java APM Agent to report performance data to `dc3-apm`. To turn it on, set it
explicitly when starting the core stack:

```bash
APM_AGENT_ENABLE=true make up STACK=app
```

::: info Starting the apm container ≠ enabling APM
Running `make up-optional` alone does not collect APM data. `dc3-apm` is only the receiving end. Unless the core stack's
`APM_AGENT_ENABLE` is `true`, the application never attaches the Agent, so there's nothing to report.
:::

## How metrics are ingested (Prometheus / Grafana)

Each service exposes a Prometheus-format metrics endpoint through Micrometer. Prometheus scrapes those endpoints on a
schedule, along with the two exporters — `postgres-exporter` (database metrics) and `nginx-exporter` (frontend nginx
metrics). Prometheus keeps 7 days of time-series data locally (`--storage.tsdb.retention.time=7d`), and Grafana queries
it as a data source to draw dashboards.

To run it:

1. Run `make up-optional` to start Prometheus, the two exporters, and Grafana.
2. Open `http://localhost:3000` in a browser to reach the Grafana dashboards.
3. Grafana's external URL is set by `GF_SERVER_ROOT_URL` (defaults to `http://localhost:3000`). If you reach it through
   a reverse proxy or from another host, update this variable so the generated links don't point at `localhost`.

```bash
# Fix the Grafana root URL for reverse-proxy/remote access (example value)
GF_SERVER_ROOT_URL="https://ops.example.com/grafana" make up-optional
```

Prometheus publishes no port to the host and is normally queried through Grafana. To look at it directly, temporarily
add a port mapping in the compose file, or use `podman exec` to drop into the container.

## Constraints and boundaries

- **Not started by default**: the core path doesn't depend on this stack. If it goes down, device access, command
  dispatch, and data persistence keep working — you just lose log search and metrics dashboards.
- **Memory floor**: Elasticsearch is the heaviest component here. On an 8 GB dev machine, run it at the
  `DC3_ES_JAVA_OPTS` default or lower, and prefer filtering with `SERVICES` to start only what you need.
- **Data retention**: Prometheus keeps a fixed 7 days; longer retention means changing `--storage.tsdb.retention.time`
  or attaching long-term storage. Elasticsearch and Logstash data lives in their named volumes (`elasticsearch`,
  `logstash`, `logs`), which `make reset` deletes along with everything else — use with care.
- **Dual APM toggle**: the `dc3-apm` container sits in the optional stack, and the `APM_AGENT_ENABLE` switch sits in the
  core stack. Both have to be on for APM data (see above).
- **Port binding**: only `127.0.0.1` is bound by default. To reach Kibana/Grafana/EMQX Dashboard from another machine,
  set `DC3_BIND_HOST` to `0.0.0.0` (or a specific NIC IP), and weigh the exposure yourself.

## Further reading

- [Deployment modes and image registries](./usage) — the full picture of the four compose stacks (db/dev/app/optional)
  and the `make` startup methods
- [Logging conventions](./logging) — the message style and field conventions of service logs, which decide what you can
  search by in Kibana
- [Troubleshooting](./troubleshooting) — the diagnostic path when things won't start, won't connect, or hit port
  conflicts
