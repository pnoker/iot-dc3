# DC3 DB Postgres Agentic

Maven module: `dc3-db-postgres-agentic`.

R2DBC store adapters for the agentic domain: sessions, messages, actions,
attachments, model configs and model providers. Implements the reactive
persistence ports declared by `dc3-common-agentic` against the PostgreSQL
dialect and JSONB conventions.

Auto-configuration registers the stores when a pooled `ConnectionFactory`,
reactive transaction boundary and page transaction are present.
