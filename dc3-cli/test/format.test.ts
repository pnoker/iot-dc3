import { describe, it, expect } from "vitest";
import { formatOutput } from "../src/utils/format";

describe("format utils", () => {
  it("formatOutput json should stringify", () => {
    const result = formatOutput({ ok: true, data: "test" }, "json");
    const parsed = JSON.parse(result);
    expect(parsed.ok).toBe(true);
    expect(parsed.data).toBe("test");
  });

  it("formatOutput json should handle arrays", () => {
    const result = formatOutput([{ id: 1, name: "a" }], "json");
    const parsed = JSON.parse(result);
    expect(parsed).toHaveLength(1);
    expect(parsed[0].id).toBe(1);
  });

  it("formatOutput table should handle arrays", () => {
    const result = formatOutput(
      [
        { id: 1, name: "Device A" },
        { id: 2, name: "Device B" },
      ],
      "table",
    );
    expect(result).toContain("Device A");
    expect(result).toContain("Device B");
    expect(result).toContain("id");
    expect(result).toContain("name");
  });

  it("formatOutput table should handle empty array", () => {
    const result = formatOutput([], "table");
    expect(result).toBe("(empty)");
  });

  it("formatOutput table should handle flat object", () => {
    const result = formatOutput(
      { gateway: "http://localhost:8000", tenant: "default" },
      "table",
    );
    expect(result).toContain("gateway");
    expect(result).toContain("default");
  });

  it("formatOutput should keep 204 results silent", () => {
    expect(formatOutput(undefined, "json")).toBe("");
    expect(formatOutput(undefined, "table")).toBe("");
    expect(formatOutput(undefined, "yaml")).toBe("");
  });
});

describe("table rendering polish (no trailing whitespace, ext blobs, clamping)", () => {
  /** Rows whose last column is empty on some records — the historical trailing-space source. */
  const heterogeneousRows = [
    { id: 1, name: "alpha" },
    { id: 2 },
    { id: 3, name: "gamma" },
  ];

  /** Offset-page envelope whose footer line must also stay clean. */
  const pagedRows = {
    total: 3,
    items: [
      { id: 1, name: "alpha" },
      { id: 2, name: "beta" },
    ],
    hasNext: true,
  };

  it("never pads the last column: no rendered line carries trailing whitespace", () => {
    for (const payload of [heterogeneousRows, pagedRows, [{ only: "column" }], [1, 2]]) {
      const table = formatOutput(payload, "table");
      for (const line of table.split("\n")) {
        expect(line, JSON.stringify(payload)).toMatch(/\S$/u);
      }
    }
  });

  it("flattens driverExt blobs one level: type/version inline, nulls dropped", () => {
    const table = formatOutput(
      [
        {
          id: 1,
          name: "virtual-driver",
          driverExt: { type: "VIRTUAL", version: null, content: null },
        },
        {
          id: 2,
          name: "mqtt-driver",
          driverExt: { type: "MQTT", version: "1.2.0", content: null },
        },
      ],
      "table",
    );

    // Readable inline rendering instead of the wire JSON form.
    expect(table).toContain("type=VIRTUAL");
    expect(table).toContain("type=MQTT version=1.2.0");
    // No raw JSON braces and no null-member artifacts leak into the cell.
    expect(table).not.toContain("{");
    expect(table).not.toContain('"content":null');
    expect(table).not.toContain('"version":null');
  });

  it("keeps compact JSON for non-ext object cells (scoping guard)", () => {
    const table = formatOutput([{ meta: { a: 1 } }], "table");

    expect(table).toContain('"a":1');
  });

  it("clamps over-long cells past the column budget with an ellipsis", () => {
    const long = "x".repeat(200);
    const table = formatOutput([{ id: 1, description: long }], "table");
    const lines = table.split("\n");

    expect(table).not.toContain(long);
    expect(table).toContain("…");
    // The clamped cell is the last column: the row ends with the ellipsis.
    expect(lines[2]).toMatch(/…$/u);
    // The separator proves the column stopped at the 80-char budget.
    expect(lines[1]).toHaveLength("id".length + "-+-".length + 80);
  });
});
