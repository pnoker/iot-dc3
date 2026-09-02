# DC3 DB R2DBC Runtime

Spring Boot runtime for the relational persistence layer: wires the single
R2DBC `ConnectionFactory` pool, reactive transaction manager, page-transaction
boundary, durable operation repository and the schema-fingerprint startup gate
that refuses to boot centers against a mismatched database contract.
