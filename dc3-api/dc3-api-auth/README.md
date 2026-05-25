# DC3 API Auth

## Overview

`dc3-api-auth` provides gRPC service definitions for authentication and authorization in the IoT DC3 platform. It
defines the interfaces for tenant management, user authentication,
token validation, and user login operations.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-api-auth
- **Version**: 2026.5.22
- **Package**: `io.github.pnoker.api.center.auth`

## Proto Definitions

### tenant.proto

Defines tenant-related RPC calls and data structures.

**Service**: `TenantApi`

- `SelectByCode` - Query tenant information by tenant code

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

- `SelectById` - Query user information by user ID

**Key Messages**:

- `GrpcIdQuery` - Request wrapper for user ID queries
- `GrpcRUserDTO` - Response wrapper containing user information
- `GrpcUserDTO` - User data structure (nickname, username, phone, email)

### user_login.proto

Defines user login-related RPC calls and data structures.

**Service**: `UserLoginApi`

- `SelectByName` - Query user login information by login name

**Key Messages**:

- `GrpcNameQuery` - Request wrapper for name-based queries
- `GrpcRUserLoginDTO` - Response wrapper containing login information
- `GrpcUserLoginDTO` - User login data structure (login name, user ID, password ID)

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
import "api/common/auth/tenant.proto";
import "api/common/auth/token.proto";
import "api/common/auth/user.proto";
import "api/common/auth/user_login.proto";
```

### 3. Implement Service

```java
public class TenantServiceImpl extends TenantApiGrpc.TenantApiImplBase {
    @Override
    public void selectByCode(GrpcCodeQuery request,
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
2. User login validation via `UserLoginApi`
3. Token validation via `TokenApi`
4. User information retrieval via `UserApi`

### Security Features

- Encrypted password storage (salt-based)
- Token-based authentication
- User credential management
- Login tracking with password IDs

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

### User Login Model

- **login_name**: Username for authentication
- **user_id**: Reference to user entity
- **user_password_id**: Reference to password record
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
