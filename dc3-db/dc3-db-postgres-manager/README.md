# DC3 DB Postgres Manager

Maven module: `dc3-db-postgres-manager`.

R2DBC store adapters for the manager domain: drivers and driver leases,
devices, groups, labels, points, commands and command attributes, events and
event attributes, profiles, dashboards, topics and device import jobs.
Implements the reactive persistence ports declared by `dc3-common-manager`.

Auto-configuration registers the stores when a pooled `ConnectionFactory`,
reactive transaction boundary and page transaction are present.
