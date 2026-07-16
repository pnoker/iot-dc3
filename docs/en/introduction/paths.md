---
title: Choose a Path by Role
---

<script setup>
import PathsDecisionDiagram from '../../.vitepress/theme/components/PathsDecisionDiagram.vue'
</script>


# Choose a Path by Role

The docs cover everything from first evaluation to contributing, but the shortest path through them depends on what
you're here to do. Pick the role that fits you and read the pages in the order listed.

This decision diagram points you to the right lane at a glance; the detailed reading order for each is in the sections
below.

<PathsDecisionDiagram lang="en" />

## I want to evaluate the platform first

You want to know what it is and whether it's worth your time. Read in this order:

1. [Platform Positioning](./) — the problems it solves and how it compares to similar projects
2. [Core Concepts](./concepts) — the object model and mental model
3. [System Architecture Overview](../architecture/) — the whole picture in one diagram
4. Run a demo: bring up the stack from [Quick Start](../quickstart/), then import the sample data
   `iot-dc3/dc3/dependencies/postgres/demo/iot-dc3-demo.sql` to see real data flow.

## I need to onboard devices and run day-to-day operations

You onboard devices or run operations — connecting devices, reading their data, sending commands, and handling alarms:

1. [Core Concepts](./concepts) — start by telling apart drivers, profiles, devices, and points
2. [Your First Device: End to End](../quickstart/first-device) — walk the full pipeline with the virtual driver
3. [Device Onboarding](../operation/device-onboarding) — connect devices speaking a real protocol
4. [Data and Commands](../operation/data-commands) — collection, history queries, and read/write commands
5. [Alarms and Notifications](../operation/alarms) — set up rules and notification channels

## I'm a backend developer doing custom development

You want to extend the platform, most often by writing a new protocol driver:

1. [System Architecture Overview](../architecture/) → [Services and Topology](../architecture/services)
2. [Data Plane](../architecture/data-plane) and [Command Plane](../architecture/command-plane) — the two core pipelines
3. [Domain Model](../architecture/domain-model) — DO/BO/VO, facade boundaries, and CRUD verb conventions
4. [Driver Development](../development/driver-authoring) — derive a new driver from the `dc3-driver-virtual` template
5. [API Documentation](../development/api-documentation) and [Testing](../development/testing)

## I want to do automation / integrate AI

You want to drive the platform from scripts or AI agents:

1. [CLI Guide](../automation/cli) — run the platform from the `dc3` command line
2. [AI Agent / MCP Integration](../ai/mcp) — let agents read and write devices safely over MCP
3. [Agentic Center](../ai/agentic) — the platform's built-in conversations and tool calls

## I want to contribute

Drivers, fixes, and docs are all welcome:

1. [Development Overview and Conventions](../development/) — coding conventions and commit standards
2. [Testing](../development/testing) — local and CI test gates
3. [Contributing Guide](../community/contributing) · [Code of Conduct](../community/code-of-conduct) · [Security Policy](../community/security)
