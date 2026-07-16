---
title: "Facade Modes: grpc and local"
---

<script setup>
import FacadeModesDiagram from '../../.vitepress/theme/components/FacadeModesDiagram.vue'
</script>


# Facade Modes: grpc and local

Inter-center calls between center services — the Data Center asking the Manager Center for devices, the Agentic Center
asking the Data Center for point values — can run one of two ways. In `grpc` mode each service runs as its own process
and calls happen cross-process. In `local` mode all centers collapse into a single process and calls are direct,
in-process method invocations. This page explains that the `dc3.facade.mode` switch controls **deployment topology**,
not transport protocol — and why business code never has to change when you flip it.

> You are here: you've read the [System Architecture Overview](./) and [Services and Topology](./services), and want to
> understand how the centers actually interconnect.

## A topology switch, not a protocol choice

First, the common misreading: `dc3.facade.mode` is not a "gRPC vs REST" transport pick. It's a choice about how many
processes the center services **run as**.

Business code never touches the other side's gRPC stubs or protobuf classes directly. It depends on a set of *
*protocol-neutral `*Facade` interfaces** — contracts defined in `dc3-common-facade-api` (16 interfaces such as
`DeviceFacade`, `PointValueFacade`, `TenantFacade`, `PermissionFacade`, and so on). Each interface has two
implementations, and one is selected and wired at startup based on the value of `dc3.facade.mode`:

- `grpc` mode wires the gRPC implementation (`dc3-common-facade-grpc`, e.g. `DeviceGrpcFacade`). It makes a
  cross-process gRPC call to reach the independently running target center.
- `local` mode wires the in-process implementation (`dc3-common-facade-local-*`, e.g. `DeviceLocalFacade`). It calls the
  target Service inside the same process directly, **with no network overhead**.

The Javadoc on the `DeviceFacade` interface itself says as much:

> `DeviceLocalFacade` — in-process call into `DeviceService`, selected when `dc3.facade.mode=local` (single deployment).
> `DeviceGrpcFacade` — gRPC call against Manager Center, selected when `dc3.facade.mode=grpc` (distributed deployment,
> default).

Both implementations satisfy the same interface and return the same BO/Page types, so **switching modes changes not a
single line of business code** — it only swaps the injected Bean.

## How the two modes are wired

Wiring relies on Spring Boot's `@ConditionalOnProperty`. The gRPC auto-configuration takes effect when
`dc3.facade.mode=grpc` or when the property is absent (`matchIfMissing = true`); the local auto-configuration takes
effect only when `dc3.facade.mode=local`. For any given `*Facade` interface, the switch alone decides which
implementation gets wired.

<FacadeModesDiagram lang="en" />

In the diagram, `9400` is the Manager Center's gRPC port (in grpc mode the Data Center reaches it across processes). In
local mode the Manager Center's `DeviceService` lives in the same JVM as the caller, `DeviceLocalFacade` makes a direct
method call, and no port is involved at all.

::: info Naming correspondence between interfaces and implementations
The interface names in `facade-api` are `*Facade` (e.g. `DeviceFacade`). The gRPC implementations consistently add the
`Grpc` infix (`DeviceGrpcFacade`); the in-process implementations add the `Local` infix (`DeviceLocalFacade`). So
`*GrpcFacade` is always the cross-process one, and `*LocalFacade` is always the in-process one.
:::

## When to use which

The choice comes down to how many processes you want the center services to run as:

| Dimension        | `grpc` (default)                                       | `local`                                         |
|------------------|--------------------------------------------------------|-------------------------------------------------|
| Deployment shape | Each center as a separate process, distributed         | All centers in one process, monolithic          |
| Call style       | Cross-process gRPC                                     | In-process method call, no network overhead     |
| Best for         | Distributed deployment, horizontal scaling, production | Local development, small single-node, debugging |
| Typical pairing  | Full compose stack (multiple services)                 | `dc3-center-single` monolithic service          |

Use `grpc` for distributed deployment or when you need to scale services independently: each center is a standalone
Spring Boot service that can scale and restart on its own. Use `local` for local development, a small single node, or
debugging. Paired with the `dc3-center-single` monolithic service — which collapses the four centers into one — it saves
you from launching multiple processes and the network round-trips between them. Startup is faster, and breakpoints land
directly.

::: tip How to choose

- You're bringing up the whole thing locally to develop or debug, or you just want a minimal single-node runnable → use
  `local`, run `dc3-center-single`.
- You're doing a distributed deployment, scaling centers independently, or this is production → use `grpc` (the
  default), with each center as its own process.
- When in doubt, use the default `grpc`: it's the established value for distributed envs; only the monolithic scenario
  needs an explicit switch to `local`.
  :::

## Where the default lives, and who overrides whom

The default differs by **deployment shape**, and this is the part to get right — otherwise a literal value in some
`application.yml` can mislead you:

- **Distributed centers** (such as the Manager Center): `application.yml` says
  `dc3.facade.mode: ${DC3_FACADE_MODE:grpc}`, and the distributed orchestration explicitly sets the environment variable
  `DC3_FACADE_MODE=grpc` (see `.env.example` and `dc3/env/dev.env`). So the distributed default is `grpc`.
- **Monolithic service** `dc3-center-single`: `application.yml` defaults to
  `dc3.facade.mode: ${DC3_FACADE_MODE:local}` — a monolith runs in a single process, so it naturally takes the
  in-process facade.

::: warning The Auth Center's application.yml says local — don't be misled
The base `application.yml` of the Auth Center `dc3-center-auth` writes `dc3.facade.mode` directly as `local` (a local
override). But **under distributed deployment**, the orchestration-injected `DC3_FACADE_MODE=grpc` overrides it — Auth
actually runs as `grpc` in a distributed setup. To determine which mode a service actually uses, **trust the injected
environment variable**, not the literal value in some yml.
:::

Switching modes means changing only this one switch; business code and interface signatures stay put:

::: code-group

```bash [Environment variable]
# Distributed (default): each center as a separate process, cross-process gRPC
DC3_FACADE_MODE=grpc

# Monolithic: all centers in one process, in-process direct calls
DC3_FACADE_MODE=local
```

```yaml [application.yml]
dc3:
  facade:
    mode: ${DC3_FACADE_MODE:grpc}   # distributed centers default to grpc
    # mode: ${DC3_FACADE_MODE:local} # monolithic dc3-center-single defaults to local
```

:::

## Constraints and boundaries

- `grpc` and `local` are two implementations of the same set of `*Facade` interfaces, **not** two transport protocols;
  `local` is not a "facade over REST" tier. The environment-variables page calls `DC3_FACADE_MODE` the "facade protocol
  mode", but what it actually switches is which implementation gets wired — that is, the deployment topology. The code's
  `@ConditionalOnProperty` is the source of truth.
- The default is `grpc`: the gRPC auto-configuration carries `matchIfMissing = true`, so without explicit configuration
  the gRPC implementation is wired by default.
- `local` mode requires the target Service being called to live in the same process — it's designed for
  collapsed-process setups like `dc3-center-single`. Setting separate multi-center processes to `local` leaves them
  unable to find the peer Service.
- Switching modes doesn't change business code, but it does change the runtime topology and the failure domain. Under
  `grpc`, one center crashing doesn't drag the others down. Under `local`, they share the same JVM process.

## Further reading

- [Services and Topology](./services) — the six services, their ports, gRPC ports, and startup dependency order
- [System Architecture Overview](./) — the full picture of how the gateway + four centers + drivers cooperate
