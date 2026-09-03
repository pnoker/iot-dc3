import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { Command } from "commander";
import { registerCommandCommand } from "../src/commands/command.js";
import { registerDeviceCommand } from "../src/commands/device.js";
import { registerDriverCommand } from "../src/commands/driver.js";
import { registerEventCommand } from "../src/commands/event.js";
import { registerPointCommand } from "../src/commands/point.js";
import { registerProfileCommand } from "../src/commands/profile.js";

type Register = (_program: Command) => void;
type FetchCall = { url: string; init: RequestInit };

const fetchCalls: FetchCall[] = [];

vi.mock("../src/core/config-manager.js", () => ({
  configManager: {
    getActiveProfile: vi.fn(async () => ({
      gateway: "http://gw.test/",
      tenant: "t",
      username: "u",
    })),
    load: vi.fn(async () => ({ current_profile: "default" })),
    getSettings: vi.fn(async () => ({ renewal_threshold_hours: 12 })),
  },
}));

vi.mock("../src/core/token-manager.js", () => ({
  tokenManager: {
    getState: vi.fn(async () => null),
    needsRenewal: vi.fn(async () => false),
  },
}));

const cases: Array<{
  entity: string;
  register: Register;
  nameField: string;
  stableField: [string, unknown];
}> = [
  {
    entity: "driver",
    register: registerDriverCommand,
    nameField: "driverName",
    stableField: ["driverCode", "driver-code"],
  },
  {
    entity: "device",
    register: registerDeviceCommand,
    nameField: "deviceName",
    stableField: ["driverId", "101"],
  },
  {
    entity: "profile",
    register: registerProfileCommand,
    nameField: "profileName",
    stableField: ["profileCode", "profile-code"],
  },
  {
    entity: "point",
    register: registerPointCommand,
    nameField: "pointName",
    stableField: ["profileId", "102"],
  },
  {
    entity: "command",
    register: registerCommandCommand,
    nameField: "commandName",
    stableField: ["timeout", 30],
  },
  {
    entity: "event",
    register: registerEventCommand,
    nameField: "eventName",
    stableField: ["eventTypeFlag", "INFO"],
  },
];

function buildProgram(register: Register): Command {
  const program = new Command();
  program.exitOverride();
  program.configureOutput({ writeErr: () => undefined });
  register(program);
  return program;
}

async function run(register: Register, args: string[]): Promise<string> {
  const program = buildProgram(register);
  let output = "";
  vi.spyOn(process.stdout, "write").mockImplementation((chunk) => {
    output += String(chunk);
    return true;
  });
  vi.spyOn(process, "exit").mockImplementation((() => undefined) as never);
  await program.parseAsync(args, { from: "user" });
  return output;
}

describe("manager optimistic-lock write commands", () => {
  beforeEach(() => {
    fetchCalls.length = 0;
    vi.stubGlobal(
      "fetch",
      vi.fn(async (url: string, init?: RequestInit) => {
        const request = { url, init: init ?? {} };
        fetchCalls.push(request);
        if (request.init.method === "DELETE") {
          return new Response(null, { status: 204 });
        }
        if (url.includes("/get_by_id")) {
          const entity = url.match(/\/manager\/([^/]+)\//)?.[1] ?? "resource";
          const testCase = cases.find((item) => item.entity === entity);
          return new Response(
            JSON.stringify({
              id: "id A&B",
              version: 7,
              [testCase?.nameField ?? "name"]: "old-name",
              [testCase?.stableField[0] ?? "stable"]: testCase?.stableField[1],
            }),
            { status: 200 },
          );
        }
        return new Response(String(request.init.body), { status: 200 });
      }),
    );
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  for (const testCase of cases) {
    it(`${testCase.entity} delete uses DELETE with id and version`, async () => {
      const output = await run(testCase.register, [
        testCase.entity,
        "delete",
        "id A&B",
        "--version",
        "3",
      ]);

      expect(fetchCalls).toHaveLength(1);
      expect(fetchCalls[0].init.method).toBe("DELETE");
      expect(fetchCalls[0].url).toBe(
        `http://gw.test/api/v3/manager/${testCase.entity}/delete?id=id%20A%26B&version=3`,
      );
      expect(output).toBe("");
    });

    it(`${testCase.entity} delete rejects a missing version`, async () => {
      await expect(
        buildProgram(testCase.register).parseAsync(
          [testCase.entity, "delete", "1"],
          { from: "user" },
        ),
      ).rejects.toMatchObject({
        code: "commander.missingMandatoryOptionValue",
      });
      expect(fetchCalls).toHaveLength(0);
    });

    it(`${testCase.entity} update merges the current resource and caller version`, async () => {
      await run(testCase.register, [
        testCase.entity,
        "update",
        "id A&B",
        "--version",
        "3",
        "--name",
        "new-name",
      ]);

      expect(fetchCalls).toHaveLength(2);
      expect(fetchCalls[0].init.method).toBe("GET");
      expect(fetchCalls[0].url).toBe(
        `http://gw.test/api/v3/manager/${testCase.entity}/get_by_id?id=id%20A%26B`,
      );
      expect(fetchCalls[1].init.method).toBe("POST");
      expect(fetchCalls[1].url).toBe(
        `http://gw.test/api/v3/manager/${testCase.entity}/update`,
      );
      expect(JSON.parse(String(fetchCalls[1].init.body))).toMatchObject({
        id: "id A&B",
        version: 3,
        [testCase.nameField]: "new-name",
        [testCase.stableField[0]]: testCase.stableField[1],
      });
    });
  }
});
