# DC3 DB Runtime

Maven module: `dc3-db-runtime`.

Spring Boot runtime for the relational persistence layer: wires the single
R2DBC `ConnectionFactory` pool, reactive transaction manager, page-transaction
boundary, durable operation repository, the single PostgreSQL dialect adapter
(PostgreSQL is the only supported engine and registers unconditionally), and
the schema-fingerprint startup gate that refuses to boot centers against a
mismatched database contract.
