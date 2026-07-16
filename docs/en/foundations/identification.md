---
title: Auto-Identification & Positioning
---

<script setup>
import IdentificationChoiceDiagram from '../../.vitepress/theme/components/IdentificationChoiceDiagram.vue'
import IdentificationEntityDiagram from '../../.vitepress/theme/components/IdentificationEntityDiagram.vue'
</script>


# Auto-Identification & Positioning

The first step in any IoT system is letting machines recognize every object and every place in the physical world. This
chapter covers the part of the perception layer that speaks not in measured physical quantities but in **identity** and
**coordinates**: auto-identification — barcodes, RFID, NFC — answers "what is this, which one is it"; positioning —
GNSS, cell-tower, UWB, Bluetooth beacons — answers "where is it." By the end you will know the range, capacity and cost
boundaries of each technology, and how the idea of "give every thing an identity" lands in IoT DC3 as `deviceId` and
`tenantId`.

> You are here: the perception layer already turns physical quantities into signals
> via [Sensing & Measurement](./sensing); this chapter adds the parallel sensing track of "identity and location." Next,
> see [Fieldbus & Protocols](./fieldbus) for how that data leaves the field.

## What This Layer Is / Why It Exists

Sensors answer "how much"; identification and positioning answer "which one" and "where." Both belong to the perception
layer, but what they produce is not a continuous analog value — it is a **discrete identifier** and a **spatial
coordinate**. They are the "primary key" and the "address" that map real-world objects onto digital records.

Why a whole category for this? Because a machine facing thousands of physical objects cannot distinguish, track, or bind
history to them without identity. A carton scanned at dozens of nodes from factory to shelf; a forklift roaming a
warehouse the system must keep locating — identification gives an object a **stable name**, positioning gives it a *
*live coordinate**, and only together do they make the physical world truly **addressable**.

These technologies share a profile: low information density (often just a number), fast reads, and a per-unit cost low
enough to deploy at scale. That is why almost every engineering trade-off here turns on one triangle — **range,
capacity, and per-item cost**. Longer range needs more power or a battery; larger capacity needs a richer chip; and
scale forces the cost down hard. Understand the triangle and you understand where each technology below fits.

## Key Technologies & Trade-offs

Start with identification. The **barcode (1D)** is the cheapest identity carrier: black-and-white bars encoding a
dozen-odd digits, carried by a sheet of paper and a drop of ink — but small in capacity, requiring close-range optical
alignment, and unreadable once smudged. The **2D code (QR / DataMatrix)** encodes in two dimensions, jumping to
kilobytes of capacity with built-in error correction, so a partially damaged code still recovers — hence its reach from
payments to equipment nameplates. But it is still optical: it needs line of sight.

**RFID** swaps optics for radio waves, and its core value is **no line of sight, batch reads**. It comes in three bands.
**Low frequency LF (~125 kHz)** penetrates well and resists metal/liquid interference, but reads only centimeters at low
speed — used for animal chips and access cards. **High frequency HF (13.56 MHz)** reads tens of centimeters at moderate
speed and is the physical basis of NFC. **Ultra-high frequency UHF (860–960 MHz)** reaches several meters and
inventories hundreds of tags at once — the workhorse of warehouse and logistics batch identification, though prone to
metal and liquid reflection. By power source there are two kinds: a **passive tag** has no battery and harvests energy
from the reader's field — cheap (down to cents), near-infinite lifespan, but limited range; an **active tag** carries a
battery and transmits on its own — tens of meters of range and able to carry sensor data, but costly and lifespan-bound.
An RFID system is always a **reader** plus **tags**: the reader powers and transceives, the tag carries an ID and
responds.

**NFC** is essentially the close-range subset of 13.56 MHz HF RFID (typically within 4 cm), distinguished by being
peer-to-peer, bidirectional, and either active or passive — and already built into nearly every phone, which makes it
the de facto standard for tap-to-pair provisioning, mobile payment, and digital business cards.

Now positioning. **GNSS (Global Navigation Satellite System)** is the cornerstone of outdoor positioning, solving 3D
coordinates from the arrival-time differences of multiple satellite signals; the representative systems are the US **GPS
** and China's **BeiDou (BDS)**, and modern chips are usually multi-constellation, accurate to meters and to centimeters
with differential augmentation — but satellite signals do not pierce roofs, so it **mostly fails indoors**, with a slow
first fix and higher power draw. **Cell-tower positioning** estimates location from cellular cell info, needs no extra
hardware and works indoors and out, but is accurate only to tens or hundreds of meters — fit for rough or fallback
positioning. **UWB (Ultra-Wideband)** measures time-of-flight with nanosecond pulses, reaching **10–30 cm** indoor
accuracy — the leading choice for high-precision indoor positioning of people, assets, and robots, at the cost of
pre-deployed anchor stations and higher spend. **Bluetooth beacons** broadcast periodically and the receiver estimates
distance from signal strength (RSSI) — cheap to deploy and readable by any phone, but RSSI is environment-sensitive, so
accuracy is usually meter-level, fit for zone-level ("which exhibit area") rather than precise positioning.

Place both groups on a "range vs. cost" trade-off plane and the picture clears up:

<IdentificationChoiceDiagram lang="en" />

::: tip There is no "best," only "best fit"
Batch warehouse inventory → UHF RFID; outdoor fleet dispatch → GNSS; precise indoor people-tracking → UWB; lightweight
mobile provisioning → NFC. A single scenario often combines them — e.g. UWB real-time positioning plus 2D-code asset
registration.
:::

## Engineering Notes

When you put these systems into production, the recurring traps are rarely "which technology" — they are physical and
engineering details.

**Medium and environment** decide success. UHF RFID reads unreliably on metal shelves and liquid containers, needing
anti-metal tags or tuned antenna polarization; barcodes fail under grease, heat, and outdoor sun, so industrial sites
switch to laser marking or metal-nameplate 2D codes. Validate read rates against real conditions before committing, not
against lab specs.

**Band equals compliance**. RFID and UWB operate in regulated radio bands allocated differently by country (e.g. UHF
RFID is 865–868 MHz in Europe, 902–928 MHz in North America, 920–925 MHz in China). Cross-region deployments must
confirm device band and transmit power are compliant, or they will interfere with others or be banned.

**The identity scheme must be globally unique**. A chip alone is not enough — a number only means something if it never
repeats within a large enough scope. Industry built coding standards for exactly this, the most representative being *
*EPC (Electronic Product Code)** — a global object-coding scheme for RFID tags that packs "manufacturer + product +
serial" into one identifier, giving every individual item (not just every product type) a one-of-a-kind identity. EPC's
idea is the essence of IoT identification: **a thing can be tracked across the network only after it has a globally
unique ID**.

**Match accuracy to cost on demand**. Do not deploy UWB for a "which room" requirement, and do not expect Bluetooth
beacons to reach centimeters. Each order-of-magnitude gain in positioning accuracy usually costs a step-change in
hardware and deployment; first nail down how accurate the business truly needs, then pick the technology.

::: warning Not read ≠ does not exist
Both RFID and barcode scanning have miss rates, and all positioning has error. A system must tolerate "temporarily not
read" — back it with re-reads, redundant points, timeouts, and state machines, rather than assuming every read succeeds.
This is the same engineering reality as the "values may be missing" of sensor acquisition.
:::

## How It Lands in IoT DC3

The core idea of IoT identification — **give every thing a globally unique, attributable identity** — maps directly into
IoT DC3, only DC3 sits one layer higher: it does not itself read RFID tags or scan codes (that is the job of field
devices and acquisition terminals); it establishes a **digital identity and ownership boundary** for every object
onboarded to the platform.

DC3 **uniquely identifies** a [Device](../introduction/concepts/device) with `deviceId`. A specific field machine — a
PLC, a meter, a thermostat — corresponds to one `Device` on the platform, addressed stably across the whole system by
its `deviceId`, which binds its history and links its commands and events. This is one with the "give every thing an
identity" idea: EPC gives every item a network-wide unique code, `deviceId` gives every onboarded device a platform-wide
unique identifier — except DC3's identity is registered in software and depends on no particular physical-tag
technology.

DC3 **draws the ownership and isolation boundary** with the [Tenant](../introduction/concepts/tenant) (`tenantId`).
Every business record carries a `tenantId`, by which the platform slices data into non-crossing partitions — company A's
devices, points, and data are invisible to company B. If `deviceId` answers "which device is this," `tenantId` answers "
whose device is it, who may see it." The "identity + ownership" duality of identification technology (an EPC number plus
the manufacturer prefix it belongs to) is, in DC3, exactly the `deviceId + tenantId` combination.

<IdentificationEntityDiagram lang="en" />

::: info DC3 does not do "RFID tag management"
DC3's identity model is platform-level device registration and tenant isolation; it does not bundle field-side
identification features like RFID card issuance, reader management, or scan-based check-in/out. This chapter pairs
identification technology with DC3 to highlight the **shared identification idea** (a globally unique ID plus an
ownership boundary), not to claim DC3 provides those field capabilities. If a site uses RFID or scanning for
acquisition, that data enters through protocol drivers as ordinary data and still resolves to some `deviceId` under some
`tenantId`.
:::

In one line: identification and positioning make the physical world **addressable**; DC3 makes every onboarded object *
*addressable and attributable** — the former is IoT's entry point, the latter is where the platform begins to govern
those objects.

## Further Reading

- [Sensing & Measurement](./sensing) — the other half of the perception layer: turning physical quantities into
  computable signals
- [Fieldbus & Protocols](./fieldbus) — how data from identification and sensing leaves the field over a bus
- [IoT Technology Overview](./) — back to the four-layer reference architecture, to place identification and positioning
  globally
- [Device](../introduction/concepts/device) — how `deviceId` uniquely identifies one field device in DC3
- [Tenant](../introduction/concepts/tenant) — how `tenantId` draws the data ownership and isolation boundary
