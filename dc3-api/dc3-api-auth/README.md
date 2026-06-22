# DC3 API Auth

## Overview

`dc3-api-auth` provides gRPC service definitions for authentication and authorization in the IoT DC3 platform. It
defines the interfaces for tenant management, user authentication,
token validation, and local credential lookup.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-api-auth
- **Version**: 2026.5.22
- **Package**: `io.github.pnoker.api.center.auth`

## Proto Definitions

### tenant.proto

Defines tenant-related RPC calls and data structures.

**Service**: `TenantApi`

- `GetByCode` - Query tenant information by tenant code

**Key Messages**:

- `GrpcCodeQuery` - Request wrapper for tenant code queries
- `GrpcRTenantDTO` - Response wrapper containing tenant information
- `GrpcTenantDTO` - Tenant data structure (name, code, enable flag)

### token.proto

Defines token validation RPC calls and data structures.

**Service**: `TokenApi`

- `CheckValid` - Validate authentication tokens

**Key Messages**:

- `GrpcLoginQuery` - Request wrapper with login credentials (tenant, name, password, token)
- `GrpcRTokenDTO` - Response wrapper with validation result

### user.proto

Defines user-related RPC calls and data structures.

**Service**: `UserApi`

- `GetById` - Query user information by user ID
- `GetByPrincipalId` - Query user information by principal ID

**Key Messages**:

- `GrpcIdQuery` - Request wrapper for user ID queries
- `GrpcRUserDTO` - Response wrapper containing user information
- `GrpcUserDTO` - User data structure (nickname, username, phone, email)

### local_credential.proto

Defines local credential lookup RPC calls and data structures.

**Service**: `LocalCredentialApi`

- `GetByLoginName` - Query local credential information by login name

**Key Messages**:

- `GrpcLoginNameQuery` - Request wrapper for login-name queries
- `GrpcRLocalCredentialDTO` - Response wrapper containing local credential information
- `GrpcLocalCredentialDTO` - Local credential data structure (login name, principal ID)

### permission.proto

Defines permission lookup RPC calls for RBAC.

**Service**: `PermissionApi`

- `ListPermissionCodes` - List the permission codes granted to a principal

### resource_registry.proto

Defines resource registry synchronization RPC calls.

**Service**: `ResourceRegistryApi`

- `Sync` - Synchronize API/menu resource definitions into the registry

### mcp_runtime.proto

Defines MCP (Model Context Protocol) runtime RPC calls for AI tool integration.

**Service**: `McpRuntimeApi`

- `Introspect` - Introspect an MCP connection
- `ListTools` - List the tools exposed by an MCP connection
- `ResolveTool` - Resolve a tool definition by name
- `AuthorizeToolCall` - Authorize an MCP tool invocation
- `Audit` - Record an MCP tool-call audit entry

## Dependencies

This module depends on common proto definitions:

- `api/common/base.proto` - Base entity fields (ID, timestamps)
- `api/common/r.proto` - Common response wrapper

## Usage

### 1. Add Dependency

```xml

<dependency>
    <groupId>io.github.pnoker</groupId>
    <artifactId>dc3-api-auth</artifactId>
    <version>2026.5.22</version>
</dependency>
```

### 2. Import Proto Files

```protobuf
import "api/center/auth/tenant.proto";
import "api/center/auth/token.proto";
import "api/center/auth/user.proto";
import "api/center/auth/local_credential.proto";
```

### 3. Implement Service

```java
public class TenantServiceImpl extends TenantApiGrpc.TenantApiImplBase {
    @Override
    public void getByCode(GrpcCodeQuery request,
                          StreamObserver<GrpcRTenantDTO> responseObserver) {
        // Implementation
    }
}
```

## API Features

### Multi-Tenancy Support

- Tenant isolation through tenant codes
- Tenant-scoped user management
- Cross-tenant operations support

### Authentication Flow

1. Client queries tenant by code
2. Local credential lookup via `LocalCredentialApi`
3. Token validation via `TokenApi`
4. User information retrieval by principal via `UserApi`

### Security Features

- Server-side password hashing
- Token-based authentication
- User credential management
- Login tracking with principal IDs

## Data Models

### Tenant Model

- **tenant_name**: Display name of the tenant
- **tenant_code**: Unique tenant identifier
- **enable_flag**: Active/inactive status (1=enabled, 0=disabled)

### User Model

- **nick_name**: Display name
- **user_name**: Unique username
- **phone**: Contact phone number
- **email**: Email address
- **social_ext**: Encrypted social account information
- **identity_ext**: Encrypted identity verification data

### Login Lookup Model

- **login_name**: Username for authentication
- **user_id**: Reference to user entity
- **principal_id**: Reference to unified auth principal
- **enable_flag**: Account status

## Build Instructions

```bash
# Build the module
mvn -s ../../.mvn/settings.xml clean package

# Install to local repository
mvn -s ../../.mvn/settings.xml clean install
```

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)
