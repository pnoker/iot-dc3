# DC3 DB R2DBC Core (`dc3-db-r2dbc-core`)

Framework-neutral contracts for the relational persistence layer: offset/cursor
pagination primitives, sort whitelists, signed cursor codec, tenant scope,
operation state machine, dialect interface and shared problem-details shape.

This module depends only on Reactor. It must stay free of Spring, driver and
DC3 business dependencies so facade, web and repository layers can share the
same contracts.
