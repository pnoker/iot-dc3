import { Command } from "commander";
import { dc3Client } from "../core/client.js";
import { detectFormat, printAndExit } from "../utils/format.js";
import {
  deleteManagerResource,
  parseNonNegativeInteger,
  updateManagerResource,
} from "../utils/manager.js";

const EVENT_BASE = "/api/v3/manager/event";

export function registerEventCommand(program: Command): void {
  const event = program
    .command("event")
    .description("Event management (runtime data)");

  // Manager CRUD
  event
    .command("list")
    .description("List configured events")
    .option("--device-id <id>", "Filter by device ID")
    .option("--profile-id <id>", "Filter by profile ID")
    .option("--offset <n>", "Zero-based result offset", "0")
    .option("--limit <n>", "Maximum items to return", "20")
    .option("--format <format>", "Output format")
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.post(`${EVENT_BASE}/list`, {
        offset: Number(opts.offset),
        limit: Number(opts.limit),
        ...(opts.deviceId ? { deviceId: opts.deviceId } : {}),
        ...(opts.profileId ? { profileId: opts.profileId } : {}),
      });
      printAndExit(result, format);
    });

  event
    .command("get <id>")
    .description("Get configured event by ID")
    .option("--format <format>", "Output format")
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(
        `${EVENT_BASE}/get_by_id?id=${encodeURIComponent(id)}`,
      );
      printAndExit(result, format);
    });

  // Runtime event history
  event
    .command("history")
    .description("List event history records")
    .option("--offset <n>", "Zero-based result offset", "0")
    .option("--limit <n>", "Maximum items to return", "20")
    .option("--format <format>", "Output format")
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.post("/api/v3/data/event_history/list", {
        offset: Number(opts.offset),
        limit: Number(opts.limit),
      });
      printAndExit(result, format);
    });

  event
    .command("create")
    .description("Create a new event configuration")
    .requiredOption("--name <name>", "Event name")
    .requiredOption("--profile-id <id>", "Profile ID")
    .option("--type <type>", "Event type", "INFO")
    .option("--level <level>", "Event severity", "LOW")
    .option("--format <format>", "Output format")
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.post(`${EVENT_BASE}/add`, {
        eventName: opts.name,
        profileId: opts.profileId,
        eventTypeFlag: opts.type,
        eventLevelFlag: opts.level,
      });
      printAndExit(result, format);
    });

  event
    .command("update <id>")
    .description("Update an event configuration")
    .requiredOption(
      "--version <n>",
      "Expected optimistic-lock version",
      parseNonNegativeInteger,
    )
    .option("--name <name>", "Event name")
    .option("--profile-id <id>", "Profile ID")
    .option("--type <type>", "Event type")
    .option("--level <level>", "Event severity")
    .option("--format <format>", "Output format")
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await updateManagerResource(EVENT_BASE, id, opts.version, {
        ...(opts.name ? { eventName: opts.name } : {}),
        ...(opts.profileId ? { profileId: opts.profileId } : {}),
        ...(opts.type ? { eventTypeFlag: opts.type } : {}),
        ...(opts.level ? { eventLevelFlag: opts.level } : {}),
      });
      printAndExit(result, format);
    });

  event
    .command("delete <id>")
    .description("Delete an event configuration")
    .requiredOption(
      "--version <n>",
      "Expected optimistic-lock version",
      parseNonNegativeInteger,
    )
    .option("--format <format>", "Output format")
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await deleteManagerResource(EVENT_BASE, id, opts.version);
      printAndExit(result, format);
    });
}
