import { Command } from "commander";
import { dc3Client } from "../core/client.js";
import { detectFormat, printAndExit } from "../utils/format.js";
import {
  deleteManagerResource,
  parseNonNegativeInteger,
  updateManagerResource,
} from "../utils/manager.js";

const DRIVER_BASE = "/api/v3/manager/driver";

export function registerDriverCommand(program: Command): void {
  const driver = program.command("driver").description("Driver management");

  driver
    .command("list")
    .description("List drivers")
    .option("--offset <n>", "Zero-based result offset", "0")
    .option("--limit <n>", "Maximum items to return", "20")
    .option("--format <format>", "Output format")
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.post(`${DRIVER_BASE}/list`, {
        offset: Number(opts.offset),
        limit: Number(opts.limit),
      });
      printAndExit(result, format);
    });

  driver
    .command("get <id>")
    .description("Get driver by ID")
    .option("--format <format>", "Output format")
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(
        `${DRIVER_BASE}/get_by_id?id=${encodeURIComponent(id)}`,
      );
      printAndExit(result, format);
    });

  driver
    .command("create")
    .description("Create a driver")
    .requiredOption("--name <name>", "Driver name")
    .requiredOption("--service-name <name>", "Driver service name")
    .requiredOption("--service-host <host>", "Driver service IPv4 address")
    .option("--code <code>", "Stable driver code")
    .option("--type <type>", "Driver type", "DRIVER_CLIENT")
    .option("--format <format>", "Output format")
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.post(`${DRIVER_BASE}/add`, {
        driverName: opts.name,
        serviceName: opts.serviceName,
        serviceHost: opts.serviceHost,
        driverTypeFlag: opts.type,
        ...(opts.code ? { driverCode: opts.code } : {}),
      });
      printAndExit(result, format);
    });

  driver
    .command("update <id>")
    .description("Update a driver")
    .requiredOption(
      "--version <n>",
      "Expected optimistic-lock version",
      parseNonNegativeInteger,
    )
    .option("--name <name>", "Driver name")
    .option("--service-name <name>", "Driver service name")
    .option("--service-host <host>", "Driver service IPv4 address")
    .option("--type <type>", "Driver type")
    .option("--format <format>", "Output format")
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await updateManagerResource(
        DRIVER_BASE,
        id,
        opts.version,
        {
          ...(opts.name ? { driverName: opts.name } : {}),
          ...(opts.serviceName ? { serviceName: opts.serviceName } : {}),
          ...(opts.serviceHost ? { serviceHost: opts.serviceHost } : {}),
          ...(opts.type ? { driverTypeFlag: opts.type } : {}),
        },
      );
      printAndExit(result, format);
    });

  driver
    .command("delete <id>")
    .description("Delete a driver")
    .requiredOption(
      "--version <n>",
      "Expected optimistic-lock version",
      parseNonNegativeInteger,
    )
    .option("--format <format>", "Output format")
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await deleteManagerResource(DRIVER_BASE, id, opts.version);
      printAndExit(result, format);
    });

  driver
    .command("status <id>")
    .description("Get driver status")
    .option("--format <format>", "Output format")
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.post("/api/v3/data/driver/status/list", {
        id,
      });
      printAndExit(result, format);
    });
}
