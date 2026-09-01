/**
 * Output formatters for command results.
 * - json: structured output for AI agents and scripts (default for non-TTY)
 * - table: human-readable tabular output (default for TTY)
 * - yaml: alternative structured format
 */

type FormatValue = string | number | boolean | null | undefined;

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function flattenObject(
  obj: Record<string, unknown>,
  prefix = "",
): Record<string, FormatValue> {
  const result: Record<string, FormatValue> = {};
  for (const [key, value] of Object.entries(obj)) {
    const path = prefix ? `${prefix}.${key}` : key;
    if (isRecord(value)) {
      Object.assign(result, flattenObject(value, path));
    } else if (Array.isArray(value)) {
      result[path] = JSON.stringify(value);
    } else {
      result[path] = value as FormatValue;
    }
  }
  return result;
}

function formatTable(data: unknown): string {
  if (Array.isArray(data)) {
    if (data.length === 0) return "(empty)";
    const headers = Object.keys(data[0] as object);
    const rows = data.map((item) =>
      headers.map((h) => String((item as Record<string, unknown>)[h] ?? "")),
    );
    const colWidths = headers.map((h, i) =>
      Math.max(h.length, ...rows.map((r) => r[i].length)),
    );
    const sep = colWidths.map((w) => "-".repeat(w)).join("-+-");
    const headerRow = headers.map((h, i) => h.padEnd(colWidths[i])).join(" | ");
    const dataRows = rows
      .map((r) => r.map((c, i) => c.padEnd(colWidths[i])).join(" | "))
      .join("\n");
    return `${headerRow}\n${sep}\n${dataRows}`;
  }

  if (isRecord(data)) {
    const flat = flattenObject(data);
    const maxKeyLen = Math.max(...Object.keys(flat).map((k) => k.length));
    return Object.entries(flat)
      .map(([k, v]) => `${k.padEnd(maxKeyLen)} │ ${v ?? "null"}`)
      .join("\n");
  }

  return String(data);
}

export type OutputFormat = "json" | "table" | "yaml";

export function formatOutput(
  data: unknown,
  format: OutputFormat = "json",
): string {
  if (data === undefined) return "";
  switch (format) {
    case "json":
      return JSON.stringify(data, null, 2);
    case "table":
      return formatTable(data);
    case "yaml": {
      // Simple YAML serialization for flat objects
      const json = JSON.parse(JSON.stringify(data));
      if (Array.isArray(json)) {
        return json.map((item) => `- ${JSON.stringify(item)}`).join("\n");
      }
      if (isRecord(json)) {
        return Object.entries(json)
          .map(([k, v]) => `${k}: ${JSON.stringify(v)}`)
          .join("\n");
      }
      return String(json);
    }
    default:
      return JSON.stringify(data, null, 2);
  }
}

/**
 * Detect the best output format: table for interactive TTY, json otherwise.
 */
export function detectFormat(explicit?: string): OutputFormat {
  if (explicit === "json" || explicit === "table" || explicit === "yaml") {
    return explicit;
  }
  return process.stdout.isTTY ? "table" : "json";
}

/**
 * Print formatted output and exit. Used at the end of every command.
 */
export function printAndExit(
  data: unknown,
  format: OutputFormat = "json",
  exitCode = 0,
): never {
  const output = formatOutput(data, format);
  if (output) process.stdout.write(output + "\n");
  process.exit(exitCode);
}
