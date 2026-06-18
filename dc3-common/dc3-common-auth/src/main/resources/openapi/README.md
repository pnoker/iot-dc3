# Static OpenAPI specs for MCP tool schemas

`McpOpenApiAggregator` reads `openapi-<service>.json` from this directory at runtime to
enrich MCP tools with input parameter schemas (request body + query/path parameters).

The API contract is a compile-time fact that changes rarely, so it is snapshotted here as a
versioned file instead of being fetched over HTTP at runtime. This keeps the auth service
free of any dependency on the other center services being reachable, and removes any need to
expose `/v3/api-docs` in production.

## Files

One file per center service, named with the **bare** service name; the aggregator expands it
to `dc3-center-<service>` so keys line up with `dc3_api.api_code`:

- `openapi-auth.json`
- `openapi-manager.json`
- `openapi-data.json`
- `openapi-agentic.json`

## Regenerating after an API contract change

Export from a running dev/test stack and copy the results here:

```bash
# from iot-dc3/
make openapi                     # writes dc3/doc/openapi/openapi-<svc>.json
cp dc3/doc/openapi/openapi-*.json \
   dc3-common/dc3-common-auth/src/main/resources/openapi/
```

Then rebuild and refresh the catalog (`POST /auth/mcp/tool/catalog/refresh`, or wait for the
scheduled refresh). A missing or stale file is non-fatal: affected tools simply ship without
a parameter schema until the file is updated.
