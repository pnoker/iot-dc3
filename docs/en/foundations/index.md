---
title: IoT Technology Overview
---

# IoT Technology Overview

The Internet of Things is not a single technology — it is a layered system that brings the physical world into the
digital one. This part walks through the core knowledge of IoT along the industry's classic **four-layer reference
architecture** — perception, network, platform, and application, plus security that cuts across all four — and shows at
every layer how IoT DC3 implements it. By the end you will hold a complete map from "sensor to AI-driven operations,"
and know exactly where DC3 sits on that map.

## The Four-Layer Reference Architecture

Take an IoT system apart and data flows bottom-up while control flows top-down: the perception layer turns physical
quantities into digital signals, the network layer carries those signals reliably, the platform layer stores, manages
and computes over the data at scale, and the application layer turns data into business value. Security belongs to no
single layer — it is a cross-cutting concern that runs through all of them.

<FourLayersDiagram lang="en" />

This layering is not dogma but a set of **responsibility boundaries**: each layer solves only its own problem and
collaborates with its neighbors through clear interfaces. Its value is that any IoT platform, any IoT product, can be
located on this map. IoT DC3 is no exception.

## How DC3 Sits on the Four Layers

IoT DC3 is not another restatement of "generic IoT theory" — it is a **runnable implementation** of this four-layer
architecture. Layer by layer:

- **Perception → Profile and Point**. The physical quantities produced by field sensors, actuators and meters are
  modeled in DC3 as the [Profile](../introduction/concepts/profile), the [Device](../introduction/concepts/device) and
  the [Point](../introduction/concepts/point) — capturing semantics like "temperature" or "switch" stably.
  See [Sensing & Measurement](./sensing) and [Auto-ID & Positioning](./identification).
- **Network → protocol drivers**. Dozens of heterogeneous protocols — Modbus, OPC UA, MQTT, BACnet… — are unified by
  DC3's [28 protocol drivers](../drivers/) and normalized into semantically labeled point values.
  See [Industrial Buses & Protocols](./fieldbus) and [IoT Protocols & Wireless](./iot-protocols).
- **Platform → center services and the data plane**. Device metadata is managed by
  the [center services](../architecture/services); point values flow through
  the [data plane](../architecture/data-plane) into a TimescaleDB time-series store and become queryable.
  See [Edge & Cloud Architecture](./edge-cloud) and [Time-Series & Streaming](./data-pipeline).
- **Application → operations and AI**. On top of the data, DC3 offers [operations and alarms](../operation/), and
  through the [Agentic Center and MCP](../ai/) lets large language models join the sense–decide–act–feedback loop.
  See [Data Intelligence & AIoT](./aiot).
- **Security → auth, tenancy, RBAC**. The cross-cutting security shows up in DC3
  as [authentication, tenant isolation and RBAC](../architecture/auth-rbac), plus TLS on the wire.
  See [IoT Security](./security).

## How to Read This Part

You can read this part straight through as a structured primer on IoT, or jump in by layer: to learn how sensors are
chosen, read the **perception layer**; to weigh MQTT against NB-IoT, read the **network layer**; to sort out how edge
and cloud split the work, read the **platform layer**. Each chapter opens with "what this layer is, key technologies and
trade-offs, engineering notes," and closes with "how it lands in IoT DC3," so theory and implementation form a loop.

## Further Reading

- [Core Concepts](../introduction/concepts) — DC3's base objects: Profile, Device, Point, Tenant
- [Architecture](../architecture/) — DC3's own service topology, data plane and command plane
- [Connectivity & Drivers](../drivers/) — how 28 protocol drivers bring heterogeneous devices in
