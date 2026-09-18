# DC3 DB Postgres Data

Maven module: `dc3-db-postgres-data`.

R2DBC store adapters for the data domain: point value latest stores and the
ingest outbox, entity state leases and alarms, alerts and analytics, notify
config and history, rules and rule state, command history and the TSDB
handoff. Implements the reactive persistence ports declared by
`dc3-common-data`.

Auto-configuration registers the stores when a pooled `ConnectionFactory`,
reactive transaction boundary and page transaction are present.
