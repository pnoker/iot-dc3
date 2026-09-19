# DC3 DB R2DBC Postgres

Maven module: `dc3-db-r2dbc-postgres`.

The only supported relational adapter. It registers the PostgreSQL dialect
(identifier quoting, JSONB conventions) backed by the `r2dbc-postgresql`
driver, unconditionally. Adding another engine requires a deliberate
architecture change, not a new runtime switch.
