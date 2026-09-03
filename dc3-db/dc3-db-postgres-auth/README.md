# DC3 DB Postgres Auth

Maven module: `dc3-db-postgres-auth`.

R2DBC store adapters for the auth domain: tenants, users, principals,
credentials, service accounts, roles, permissions, menus, resources, audit
logs, OAuth/MCP runtime state and the resource registry. Implements the
reactive persistence ports declared by `dc3-common-auth`.

Auto-configuration registers the stores when a pooled `ConnectionFactory`,
reactive transaction boundary and page transaction are present.
