# DC3 DB R2DBC Postgres

Maven module: `dc3-db-r2dbc-postgres`.

The only supported relational adapter. Activated by `dc3.db.type=postgres`,
it registers the PostgreSQL dialect (identifier quoting, JSONB conventions)
backed by the `r2dbc-postgresql` driver. Adding another engine requires a
deliberate architecture change, not a new runtime switch.
