import { Command } from "commander";
import { dc3Client } from "../core/client.js";
import { detectFormat, printAndExit } from "../utils/format.js";
import {
  deleteManagerResource,
  parseNonNegativeInteger,
  updateManagerResource,
} from "../utils/manager.js";

const POINT_BASE = "/api/v3/manager/point";

export function registerPointCommand(program: Command): void {
  const point = program
    .command("point")
    .description("Point (datapoint) management");

  // dc3 point list
  point
    .command("list")
    .description("List points")
    .option("--device-id <id>", "Filter by device ID")
    .option("--profile-id <id>", "Filter by profile ID")
    .option("--offset <n>", "Zero-based result offset", "0")
    .option("--limit <n>", "Maximum items to return", "20")
    .option("--format <format>", "Output format")
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const body: Record<string, unknown> = {
        offset: Number(opts.offset),
        limit: Number(opts.limit),
      };
      if (opts.deviceId) body.deviceId = opts.deviceId;
      if (opts.profileId) body.profileId = opts.profileId;
      const result = await dc3Client.post(`${POINT_BASE}/list`, body);
      printAndExit(result, format);
    });

  // dc3 point get <id>
  point
    .command("get <id>")
    .description("Get point by ID")
    .option("--format <format>", "Output format")
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(
        `${POINT_BASE}/get_by_id?id=${encodeURIComponent(id)}`,
      );
      printAndExit(result, format);
    });

  // dc3 point read <id>
  point
    .command("read <id>")
    .description("Read the latest value of a point")
    .option("--format <format>", "Output format")
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.post("/api/v3/data/point_value/latest", {
        pointId: id,
        offset: 0,
        limit: 1,
      });
      printAndExit(result, format);
    });

  // dc3 point history <id>
  point
    .command("history <id>")
    .description("Read point value history")
    .option("--limit <n>", "Number of records per page", "100")
    .option("--cursor <cursor>", "Opaque cursor from the previous page")
    .option("--device-id <id>", "Device ID (required for history)")
    .option("--format <format>", "Output format")
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      if (!opts.deviceId) {
        printAndExit(
          { ok: false, message: "--device-id is required for history query" },
          format,
          1,
        );
      }
      const result = await dc3Client.get(
        `/api/v3/data/point_value/history?device_id=${opts.deviceId}&point_id=${id}&limit=${opts.limit}${opts.cursor ? `&cursor=${encodeURIComponent(opts.cursor)}` : ""}`,
      );
      printAndExit(result, format);
    });

  // dc3 point write <id> (asynchronous command submission)
  point
    .command("write <id>")
    .description("Write a value to a point")
    .requiredOption("--value <value>", "Value to write")
    .option("--device-id <id>", "Device ID (required)")
    .option("--confirm", "Require user confirmation (pending action)", false)
    .option("--format <format>", "Output format")
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      if (!opts.deviceId) {
        printAndExit(
          {
            ok: false,
            message: "--device-id is required for write operations",
          },
          format,
          1,
        );
      }
      const result = await dc3Client.post("/api/v3/data/point_command/write", {
        deviceId: opts.deviceId,
        pointId: id,
        value: opts.value,
      });
      printAndExit(result, format);
    });

  // dc3 point create
  point
    .command("create")
    .description("Create a new point")
    .requiredOption("--name <name>", "Point name")
    .requiredOption("--profile-id <id>", "Profile ID")
    .option("--type <type>", "Point type", "FLOAT")
    .option("--rw <type>", "Read/write type", "READ_ONLY")
    .option(
      "--value-decimal <n>",
      "Decimal precision",
      parseNonNegativeInteger,
      3,
    )
    .option("--base-value <value>", "Base value", "0")
    .option("--multiple <value>", "Scale multiplier", "1")
    .option("--unit <unit>", "Unit")
    .option("--format <format>", "Output format")
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const body: Record<string, unknown> = {
        pointName: opts.name,
        profileId: opts.profileId,
        pointTypeFlag: opts.type,
        rwFlag: opts.rw,
        valueDecimal: opts.valueDecimal,
        baseValue: opts.baseValue,
        multiple: opts.multiple,
      };
      if (opts.unit) body.unit = opts.unit;
      const result = await dc3Client.post(`${POINT_BASE}/add`, body);
      printAndExit(result, format);
    });

  // dc3 point update <id>
  point
    .command("update <id>")
    .description("Update a point")
    .requiredOption(
      "--version <n>",
      "Expected optimistic-lock version",
      parseNonNegativeInteger,
    )
    .option("--name <name>", "New point name")
    .option("--profile-id <id>", "Profile ID")
    .option("--type <type>", "Point type")
    .option("--rw <type>", "Read/write type")
    .option("--value-decimal <n>", "Decimal precision", parseNonNegativeInteger)
    .option("--base-value <value>", "Base value")
    .option("--multiple <value>", "Scale multiplier")
    .option("--unit <unit>", "New unit")
    .option("--format <format>", "Output format")
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await updateManagerResource(POINT_BASE, id, opts.version, {
        ...(opts.name ? { pointName: opts.name } : {}),
        ...(opts.profileId ? { profileId: opts.profileId } : {}),
        ...(opts.type ? { pointTypeFlag: opts.type } : {}),
        ...(opts.rw ? { rwFlag: opts.rw } : {}),
        ...(opts.valueDecimal !== undefined
          ? { valueDecimal: opts.valueDecimal }
          : {}),
        ...(opts.baseValue !== undefined ? { baseValue: opts.baseValue } : {}),
        ...(opts.multiple !== undefined ? { multiple: opts.multiple } : {}),
        ...(opts.unit !== undefined ? { unit: opts.unit } : {}),
      });
      printAndExit(result, format);
    });

  // dc3 point delete <id>
  point
    .command("delete <id>")
    .description("Delete a point")
    .requiredOption(
      "--version <n>",
      "Expected optimistic-lock version",
      parseNonNegativeInteger,
    )
    .option("--format <format>", "Output format")
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await deleteManagerResource(POINT_BASE, id, opts.version);
      printAndExit(result, format);
    });
}
