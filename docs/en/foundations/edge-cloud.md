---
title: Edge & Cloud Architecture
---

<script setup>
import EdgeCloudDiagram from '../../.vitepress/theme/components/EdgeCloudDiagram.vue'
</script>


# Edge & Cloud Architecture

The IoT platform layer is not "a server" — it is a continuum stretching from the field to the data center. Where each
piece of computation runs — close to the device or centralized in the cloud — sets the system's latency, bandwidth,
availability and privacy boundaries. This chapter explains how cloud, edge and device divide the work, what actually
distinguishes edge computing from fog computing, when computation *must* be pushed down to the edge, and finally how it
lands in IoT DC3's platform layer: one gateway plus four center services, where protocol drivers can sit near the field,
center services can be centralized, and Facade modes switch between distributed and in-process with a single flag.

By the end you can decide where a given collection task, alarm rule, or command dispatch belongs — on the device, at the
edge, or in the cloud.

## What This Layer Is / Why It Exists

We split the IoT platform layer into three tiers because each tier lives under fundamentally different physical
constraints.

**Device** is the hardware itself — sensors, actuators, PLCs, meters. It has minimal compute, speaks only its own
protocol, and exists to turn physical quantities into transmittable signals and to land commands onto registers. It
should not carry business logic, and it certainly cannot run model inference.

**Cloud** is the remote data center — near-unlimited compute, cheap storage, easy centralized management and global
analytics. It excels at funneling data from thousands of devices into one place to run historical analysis, train
models, and serve a unified API and UI. Its cost is "distance": every hop crosses a wide-area network whose latency,
bandwidth and stability are out of your hands.

**Edge** is the tier wedged between the two — deployed near the devices on site (the workshop, the substation, the
building's equipment room), stronger than a device but weaker than the cloud, yet **only one LAN hop from the device**.
Its entire reason for existing is to do, in place, the computation that "can't wait for the cloud, and shouldn't."

Why do we need the edge at all? It comes down to four hard realities:

- **Latency**: an emergency-stop interlock on a production line demands millisecond response; a command that round-trips
  through the cloud — tens to hundreds of milliseconds — may arrive after the accident. The tighter the control loop,
  the more the decision must be made nearby.
- **Bandwidth**: one vibration sensor produces thousands of samples per second; shipping the full raw waveform of
  hundreds of devices to the cloud is both expensive and unsustainable over a WAN. The edge first downsamples, extracts
  features, aggregates — and ships only the *meaningful results*.
- **Availability**: the link from field to cloud will drop. While it is down, collection cannot stop, local interlocks
  cannot fail, alarms cannot go silent — the edge must run autonomously offline and backfill once connectivity returns.
- **Privacy & compliance**: camera frames, process recipes, energy-usage detail often may not — or should not — leave
  the plant. Processing sensitive data in place at the edge and uploading only desensitized results is a hard
  requirement in many industries.

Put differently: **the cloud owns "breadth," the edge owns "speed" and "resilience," the device owns "connection."**
They do not replace one another — they are one continuum cut along responsibility lines.

## Key Technologies & Trade-offs

### Edge Computing vs. Fog Computing: often conflated, differently weighted

Both terms mean "push computation down from the cloud toward the data source." The difference is **which layer it lands
on, and who carries it**:

- **Edge computing**: computation happens at the very edge of the network — on the device itself or on an edge gateway
  right next to it. It emphasizes "as close to the point of data origin as possible" — single-point, lightweight, tied
  to a specific site.
- **Fog computing**: proposed by groups such as OpenFog, it emphasizes building a **layered, distributed
  compute-and-networking tier** *between* device and cloud — possibly spanning multiple gateways, local servers, even
  regional facilities. It is a coordinated fabric, not a single edge node.

A practical mnemonic: edge computing is the act of "doing the work at the edge"; fog computing is "organizing that edge
compute into a coordinated, scheduled tier of infrastructure." The rest of this chapter does not force the distinction —
we use "edge" to mean "the near-source compute tier between device and cloud."

### How the Device–Edge–Cloud Tiers Collaborate

The diagram below lays out each tier's responsibilities and the data/command flows. What matters is not what sits in
each box, but **which edge is a LAN and which is a WAN** — that is what decides where each piece of computation belongs.

<EdgeCloudDiagram lang="en" />

Solid lines are uplink data, dashed lines are downlink commands; device-to-edge is "one LAN hop," edge-to-cloud is the
WAN crossing. Once that boundary is clear, the "where does it go" answer surfaces on its own: latency-sensitive logic
that must keep working offline goes to the **edge** (or even the device); the global view, mass storage and model
training go to the **cloud**.

### The Edge Gateway's Responsibilities

The edge gateway is the load-bearing wall of this tier. It must carry at least five jobs:

- **Protocol adaptation**: normalize the field's motley protocols (Modbus, OPC UA, BACnet…) into one unified data shape
  the platform understands. This is its most basic and least skippable duty.
- **Filtering & aggregation**: downsample, deduplicate, extract features, and window-aggregate before uplink, spending
  bandwidth where it counts.
- **Local cache & backfill**: persist data locally when the link is down, replay it in order once it recovers, losing no
  points.
- **Edge autonomy**: keep collecting, run local rules and interlocks, and raise alarms in place while offline — without
  depending on a cloud heartbeat.
- **Security boundary**: as the sole ingress/egress between the field network and the outside, carry authentication,
  encryption and minimal exposure — field devices are never bared directly to the public network.

The trade-off: the more the gateway carries, the more autonomous and jitter-resistant the field becomes — but operations
and consistency grow harder too (more edge nodes mean config sync, version upgrades and observability all become
problems). **How much the edge does versus the cloud is this tier's central design choice.**

### Digital Twin: the physical entity mirrored on the digital side

A digital twin is a **continuously synchronized digital mirror** of each physical device / line / plant: it aggregates
that entity's live point values, historical curves, model structure and operating state, letting you observe, simulate,
even predict the physical side from the digital one.

It depends on the whole chain above: the device collects, the edge aggregates, the cloud consolidates — only then does
the twin have "living" data to feed on. The twin usually lives in the cloud (it needs the global data and compute), but
its **real-time refresh** depends on the edge promptly pushing the latest values up. Its value is reorganizing "
scattered point values" into an "entity-centric" view — exactly the leap from raw data toward intelligent operations.

### The Cloud Platform's Core Capabilities

A cloud-side platform is typically built around four capability families, mapping neatly onto "how devices are managed,
how connections are managed, how data is used, how rules run":

- **Device Management**: modeling, registration, lifecycle, remote config and firmware for devices/profiles/points —
  answering "what devices exist, and what can they do."
- **Connection Management**: device online/offline state, heartbeat and timeout, authentication and sessions —
  answering "who is connected, and how stable is it."
- **Rule Engine**: condition-triggered actions over the data stream — threshold alarms, linkage, forwarding — turning "
  data" into "action."
- **Data Service**: time-series storage, query and aggregation, outward-facing APIs — turning a flood of point values
  into a consumable data asset.

Together these four form the cloud platform's skeleton. Worth stressing: not all of them must stay in the cloud. **The
rule engine and parts of the data service can perfectly well be pushed down to the edge** — and that is precisely the
practical space of "how edge and cloud divide the work."

## Engineering Notes

A few principles run through the design of any cloud–edge–device system:

- **Allocate compute by "can it wait for the cloud."** Ask first: can this logic stop while the link is down? What
  can't (control interlocks, local alarms, collection caching) goes to the edge; what can wait and needs a global view (
  trend analysis, model training, cross-plant comparison) goes to the cloud.
- **Ship "results," not "raw streams."** Let the edge aggregate and extract features first; WAN bandwidth is forever
  scarce — don't spend it on redundancy you could compress away locally.
- **The edge must run autonomously offline.** Design the cloud as "a dependency that will drop": core functions can't
  stall while it's down, and must backfill and reconcile automatically on recovery.
- **The command path needs explicit failure semantics.** Downlink commands cross a WAN and time out more easily; you
  must distinguish "success," "failure," and "timeout," and never fake a failure as success — or upper layers will make
  worse decisions on false data.
- **One data model spans all three tiers.** If the device's raw signal, the edge's aggregate, and the cloud's stored
  value all speak differently, twins and analytics are off the table. A stable "point semantics" should run unbroken
  from edge to cloud.

## How It Lands in IoT DC3

IoT DC3's platform layer is not one monolith but [one gateway plus four center services](../architecture/services), with
protocol drivers connecting the field on the south side. This shape distributes naturally across "edge" and "cloud."

**Protocol drivers = the layer you can push to the edge.** DC3's protocol drivers (`dc3-driver-*`) handle protocol
adaptation and near-field collection — exactly the edge gateway's core role. Drivers do **not** talk to the Data Center
directly; they exchange asynchronously over RabbitMQ — point values flow north, commands flow south. That async
decoupling is the very precondition for splitting edge and cloud deployment: drivers can run close to the field, with
the MQ buffer absorbing WAN jitter so collection never back-pressures into dropped connections when the cloud slows
down.

**The four center services = the centralizable cloud-side capabilities.
** [Auth Center dc3-center-auth, Manager Center dc3-center-manager, Data Center dc3-center-data, Agentic Center dc3-center-agentic](../architecture/services)
cover the "device management / connection management / rule engine / data service" set above — of which connection
management (device/driver online-offline state, lease expiry) and the rule (alarm) engine fall mainly on Data Center
`dc3-center-data`, while device/profile/point metadata is handled by Manager Center; the centers also provide
auth/tenancy and LLM/tool-calling capabilities. Point values ultimately land in a TimescaleDB time-series store and
become queryable — the concrete form of the cloud-side "data service."

**Facade mode = the edge/cloud division switch.** Calls between center services are written against the contract
interfaces in `dc3-common-facade-api`; at runtime `DC3_FACADE_MODE` picks the implementation: [
`grpc` (the distributed default)](../architecture/facade-modes) makes each center its own process collaborating across
processes — fitting a "centers centralized in the cloud, drivers scattered at the edge" topology; `local` (single
process) collapses all centers into one process on one machine, fitting local and small single-host setups. In other
words, "how edge and cloud divide the work, distributed or not" is collapsed in DC3 into one deployment flag, not two
codebases — the same business logic switches between the two shapes by changing `DC3_FACADE_MODE`.

::: tip Mapping this chapter's concepts onto DC3

- Edge gateway's "protocol adapt / filter / collect" → protocol drivers `dc3-driver-*`
- Async decoupling buffer between edge and cloud → RabbitMQ (point values up / commands down)
- Cloud-side "device metadata management" → Manager Center `dc3-center-manager`
- Cloud-side "connection/state management" (online-offline, lease expiry) → Data Center `dc3-center-data`
- Cloud-side "data service" → Data Center `dc3-center-data` + TimescaleDB
- Edge/cloud division deployment switch → Facade mode `DC3_FACADE_MODE`
  :::

::: info On the boundary of "edge autonomy"
The "offline edge autonomy" this chapter describes is a general goal of cloud–edge–device architecture. In DC3, drivers
deployed near the field and decoupled from the centers via MQ provide the structural basis for this division; how much
local capability a given driver retains while offline depends on that driver's implementation. When in doubt, treat the
driver's source and the [Architecture](../architecture/) description as authoritative.
:::

For the full service topology, port assignments and startup dependencies, see [Architecture](../architecture/)
and [Services & Topology](../architecture/services); for the interface-wiring detail behind the edge/cloud split,
see [Facade Modes](../architecture/facade-modes).

## Further Reading

- [Time-Series Data & Stream Processing](./data-pipeline) — how point values are stored, aggregated and streamed once in
  the cloud
- [IoT Protocols & Wireless Networks](./iot-protocols) — what protocols run on the device-to-edge hop, and how to weigh
  them
- [IoT Technology Overview](./) — the four-layer reference architecture and DC3's overall positioning
- [Architecture](../architecture/) — how DC3's gateway + four centers + drivers collaborate
- [Services & Topology](../architecture/services) — the six deployable units, ports and startup dependencies
- [Facade Modes](../architecture/facade-modes) — `grpc` vs `local`: switching between distributed and in-process
