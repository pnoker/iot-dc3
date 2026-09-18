# DC3 Center

`dc3-center` contains the deployable backend applications. Most business logic lives in matching `dc3-common-*`
modules; center modules assemble dependencies, configuration, and process boundaries.

## Applications

| Module               | HTTP | gRPC | Base path  | Purpose                                                |
|----------------------|-----:|-----:|------------|--------------------------------------------------------|
| `dc3-center-single`  | 8100 | 9100 | `/single`  | auth, manager, and data in one JVM                     |
| `dc3-center-auth`    | 8300 | 9300 | `/auth`    | identity, authorization, OAuth2, and MCP authorization |
| `dc3-center-manager` | 8400 | 9400 | `/manager` | device and metadata management                         |
| `dc3-center-data`    | 8500 | 9500 | `/data`    | values, commands, events, and status                   |
| `dc3-center-agentic` | 8600 |  n/a | `/agentic` | AI-assisted operations                                 |

Ports are defaults; environment variables in each `application.yml` are authoritative. The distributed applications use
static, environment-overridable gRPC addresses. They do not depend on Nacos service discovery.

## Run locally

From the repository root:

```bash
make up-db
make up-dev GROUP=core
```

To run source processes instead, start one command per terminal:

```bash
make run SERVICE=auth
make run SERVICE=manager
make run SERVICE=data
make run SERVICE=agentic
```

Run the all-in-one process directly because the root Makefile does not define a `single` service alias:

```bash
mvn -s .mvn/settings.xml -pl dc3-center/dc3-center-single -am spring-boot:run
```

Start the gateway separately when testing public HTTP routes.

## Verification

```bash
mvn -s .mvn/settings.xml -q -f dc3-center/pom.xml -DskipTests compile
mvn -s .mvn/settings.xml -f dc3-center/pom.xml test
```

For runtime configuration, read the affected module's `application.yml` plus its active profile file. Do not duplicate
profile-specific host or port values in new documentation.
