---
title: Profile (Thing Model)
---

<script setup>
import ProfileRelationDiagram from '../../../.vitepress/theme/components/ProfileRelationDiagram.vue'
import ProfileLifecycleDiagram from '../../../.vitepress/theme/components/ProfileLifecycleDiagram.vue'
</script>

# Profile (Thing Model) <Badge type="tip" text="Thing Model+" />

> **A Profile is the "capability template for one kind of device"** <Badge type="tip" text="Thing Model+" />—it aggregates
> the [Points](./point), [Commands](./command), and [Events](./event) shared by devices of the same model, describing "
> what this kind of device can sample, control, and report". A [Device](./device) belongs to exactly one Profile, and
> many
> devices can reuse the same Profile.

## What it is / why it exists

Imagine onboarding 100 temperature-and-humidity sensors of the same model. Configuring "temperature point, humidity
point, calibration command, fault event" on each one separately means 100 rounds of duplicate work—change one thing and
you change it 100 times. A Profile solves exactly this: **factor the capability definitions out into a single template**
that device instances merely reference.

An analogy with product vs. physical units: a Profile is like a "product spec sheet / factory specification", and a
device is like "one physical unit manufactured to that spec". You write the spec once and can build many units from it.

::: tip How Profile relates to the "Thing Model"
The "Thing Model" is a common industry design for modeling device capabilities. DC3's **Profile** is a **peer
abstraction**—both answer "what capabilities does a kind of device have". DC3 did not adopt the `Product` / `ThingModel`
naming; it chose **Profile**, and its capabilities are **stronger** than a typical thing model: a Profile supports
[sharing scopes](#enumerations) (reuse across tenant / driver / user), version evolution, a weakly-structured `profileExt`
extension, and more—more flexible than the fixed "one product, one thing model" structure. Think of it as
**Profile ⊇ Thing Model**: anything a thing model can express, a Profile can too, but not vice versa (see
the [design philosophy](../../architecture/domain-model)).
:::

**Profile vs. Thing Model (see the "plus" at a glance):**

| Dimension        | Thing Model (industry-generic) | Profile (DC3) <Badge type="tip" text="Thing Model+" />        |
|------------------|--------------------------------|--------------------------------------------------------------|
| Positioning      | An abstraction for device-capability modeling | Peer abstraction, stronger (a superset)       |
| Capability set   | Properties / services / events | [Points](./point) / [Commands](./command) / [Events](./event) |
| Reuse scope      | Usually fixed per product      | Three sharing scopes: tenant / driver / user (`profileShareFlag`) |
| Versioning       | Typically no explicit version  | Explicit `version`, queryable and evolvable                  |
| Extension fields | Relatively fixed structure     | `profileExt` weakly-structured extension (can carry category / tags) |
| Creation source  | —                              | `profileTypeFlag`: system / driver / user                    |
| Device binding   | Implementation-dependent       | Exactly one (`Device.profileId`, single foreign key)         |

> In one line: **a Profile is the enhanced version of a thing model**—it keeps the peer "capability template for a class
> of devices" abstraction and layers platform capabilities (sharing, versioning, extension) on top.

**Three pairs that are easy to confuse:**

- **Profile vs. Device**: a Profile is the "class" (defined once); a Device is the "instance" (many onboarded). The
  point "temperature" is defined on the Profile, while "sensor #3's temperature right now = 25.3℃"
  —a [PointValue](./point-value)—is a device's runtime data.
- **Profile vs. Driver**: a Profile describes "which capabilities a device has" (business semantics);
  a [Driver](./driver) describes "which protocol and how to connect" (connectivity). The same Profile can be paired with
  different drivers; the two are orthogonal.
- **Aggregates vs. owns**: a Profile does not "store" the data of points / commands / events—it is only their **owning
  root**. `Point`, `Command`, and `Event` each link back to the Profile via `profileId`.

## Key fields

Profile `ProfileBO` (table `dc3_profile`):

| Field              | Type                 | Meaning                                                                                  |
|--------------------|----------------------|------------------------------------------------------------------------------------------|
| `profileName`      | String               | Profile name (for display)                                                               |
| `profileCode`      | String               | Profile code, unique within a tenant, serves as the model identifier                     |
| `profileShareFlag` | ProfileShareTypeEnum | Sharing scope, see below                                                                 |
| `profileTypeFlag`  | ProfileTypeEnum      | Creation source, see below                                                               |
| `version`          | Integer              | Model version, queryable and set manually                                                |
| `profileExt`       | ProfileExt (JSON)    | Weakly-structured extension field (designed to carry content such as `category`, `tags`) |
| `enableFlag`       | EnableFlagEnum       | Enabled / disabled state                                                                 |
| `tenantId`         | Long                 | Owning [Tenant](./tenant)                                                                |

::: tip A Profile does not hold its sub-capabilities as fields
`ProfileBO` carries no list of points / commands / events—they are independent entities that link back through their own
`profileId` foreign key. To find "which capabilities this Profile has", query `Point` / `Command` / `Event` separately
rather than reading a field on `ProfileBO`.
:::

## Enumerations

**Sharing scope `profileShareFlag` (`ProfileShareTypeEnum`)**—controls who can reuse this Profile:

| Enum     | code   | Meaning                                                                   |
|----------|--------|---------------------------------------------------------------------------|
| `TENANT` | tenant | Shared within the tenant; all devices under the tenant may reference it   |
| `DRIVER` | driver | Shared within a driver; devices belonging to that driver may reference it |
| `USER`   | user   | Private to the user; visible only to its creator                          |

**Creation source `profileTypeFlag` (`ProfileTypeEnum`)**:

| Enum     | code   | Meaning               |
|----------|--------|-----------------------|
| `SYSTEM` | system | Built into the system |
| `DRIVER` | driver | Created by a driver   |
| `USER`   | user   | Created by a user     |

## Relationship with other concepts

<ProfileRelationDiagram lang="en" />

A Profile is the owning root of three kinds of capability—[Point](./point), [Command](./command), and [Event](./event)
—which side by side answer "what this kind of device can do". A [Device](./device) binds **exactly one** Profile via
`profileId`—a single foreign key, not a many-to-many relation. How a device connects is decided by
its [Driver](./driver), orthogonal to the Profile.

## Lifecycle

<ProfileLifecycleDiagram lang="en" />

First create the Profile and fill in its points / commands / events, then have many devices of the same model bind it;
at runtime devices sample point values, receive commands, and report events according to the template; when capabilities
change, bump `version`.

::: warning A device can bind only one Profile
Early versions let a device bind multiple Profiles (`dc3_profile_bind` many-to-many); this has since converged to the
single foreign key `Device.profileId`: **a device belongs to exactly one Profile**, while one Profile may be reused by
many devices. A device's point set comes only from the single Profile its `profileId` points to—never mixed across
Profiles.
:::

## Example

Create a Profile for the "ZS-100 temperature-and-humidity sensor": `profileCode = ZS-100`, `profileShareFlag = TENANT` (
shared within the tenant), `version = 1`. Under it define two points (`temperature`, `humidity`), one command (
`CALIBRATE`), and one event (`SENSOR_FAULT`). The 100 sensors of this model onboarded afterward each point their
`Device.profileId` at this single Profile to reuse all of its capabilities; next time you add a `max` constraint to the
temperature point, you edit the Profile in one place, all 100 devices take effect at once, and `version` rises to 2.

## API

Profile management endpoints are prefixed with `/profile` (Manager service):

| Method | Path                         | Description                        |
|--------|------------------------------|------------------------------------|
| POST   | `/profile/add`               | Create a Profile                   |
| POST   | `/profile/update`            | Update Profile metadata            |
| POST   | `/profile/delete`            | Delete a Profile                   |
| GET    | `/profile/get_by_id`         | Get a Profile by ID                |
| POST   | `/profile/list`              | Page through Profiles              |
| GET    | `/profile/list_by_device_id` | List the Profile bound by a device |

## Further reading

- [Point](./point) — the data / control points a Profile aggregates
- [Command](./command) — the action-type capabilities a Profile aggregates
- [Event](./event) — the reporting capabilities a Profile aggregates
- [Device](./device) — the instance of a Profile, bound via `profileId`
- [Concepts overview](../concepts) — a tour of all core concepts
- [Domain model](../../architecture/domain-model) — where Profile sits in DC3's domain language
