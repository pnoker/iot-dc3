# dc3-cli

IoT DC3 Platform CLI — AI-ready command-line interface for the DC3 IoT platform.

## Installation

```bash
npm install -g dc3-cli
```

## Quick Start

```bash
# 1. Configure the gateway
dc3 config set gateway http://localhost:8000

# 2. Log in (prompts for tenant, username, and password interactively)
dc3 auth login

# 3. Start using
dc3 device list
dc3 point read 456789
dc3 dashboard stats
```

Non-interactive login (CI, scripts, agents): set `DC3_PASSWORD` and pass the identity flags —
no prompt is ever attempted when stdin is not a terminal:

```bash
export DC3_PASSWORD='your-password'
dc3 auth login --tenant default --username admin
```

## Commands

Create-verb commands are named `add` to mirror the backend CRUD conventions. `create` remains
available as a deprecated compat alias: invoking it still routes to the same action, prints a
one-line warning on stderr (`warning: 'create' is a deprecated compat alias of 'add' ... use
'dc3 device add'`), and help lists it as `add (deprecated alias: create)`. The alias is removed
after one release.

### Configuration (`dc3 config`)

```bash
dc3 config set gateway <url>                    # bare http/https origin, no path or query
dc3 config set auth.tenant <tenant>
dc3 config set auth.username <name>
dc3 config set auth.store <type>                # keychain | encrypted | env | prompt
dc3 config set settings.output_format <format>  # json | table | yaml
dc3 config get <key>
dc3 config list
dc3 config profile create <name> [--gateway <url>] [--tenant <tenant>] [--username <name>] [--store <type>] [--switch]
dc3 config profile use <name>
dc3 config profile delete <name>
dc3 config reset [--yes]
```

`config reset` deletes **all** local state in one shot: profiles and settings (`config.json`),
saved tokens (`tokens.json`), and stored passwords (encrypted file / OS keychain entries).
It asks for an interactive confirmation (`This will delete all profiles, settings, saved
tokens, and stored passwords. Continue?`); pass `--yes` to skip the prompt, which is required
when stdin is not a terminal.

Profile creation is first-class: `config profile create prod --gateway https://iot.example.com`
(validated per field; nothing is persisted when a value is rejected).

### Authentication (`dc3 auth`)

```bash
dc3 auth login                                                # interactive
dc3 auth login --tenant <tenant> --username <name>            # password from DC3_PASSWORD
dc3 auth login --tenant <tenant> --username <name> --password <secret>
dc3 auth login --store <store>                                # keychain | encrypted | env | prompt
dc3 auth login --no-save                                      # never persist the password
dc3 auth login --oauth --client-id <id> --client-secret <secret> [--scope <scope>]
dc3 auth logout
dc3 auth status [--all]
dc3 auth token [--header]
```

- Password input order: `--password`, then `DC3_PASSWORD`, then the interactive prompt. With a
  non-interactive stdin and no source available, login fails fast with a usage error naming the
  flag to pass — it never hangs and never fakes success.
- `--no-save` records `prompt` as the profile's credential store and skips persistence entirely:
  the password is never written anywhere, so token expiry requires a manual re-login.
- `--oauth` performs the OAuth `client_credentials` flow (visible in `auth login --help`).
  OAuth tickets are the only ones accepted at the gateway MCP endpoint.
- Login output is machine-stable: `expires_at` is ISO-8601 UTC, and `password_saved` /
  `credential_store` report what actually happened. When the configured store is unavailable on
  this machine, `password_saved` is `false` and a stderr warning explains that silent token
  renewal is impossible until a working store is configured.
- `auth logout` clears local state (token + stored password) even when the remote token
  cancellation fails; if the gateway-side cancel fails, a warning names the token that survives
  server-side until expiry.

### Credential storage and silent renewal

| Store       | Description                                                                          | Use case                |
| ----------- | ------------------------------------------------------------------------------------ | ----------------------- |
| `keychain`  | OS-level keychain (macOS Keychain, Linux Secret Service, Windows Credential Manager) | Daily use (default)     |
| `encrypted` | AES-256-GCM encrypted file at `~/.dc3/credentials.enc` with a random key at `~/.dc3/credentials.key` (0600 / restricted ACL) | Fallback when keychain unavailable |
| `env`       | `DC3_PASSWORD` environment variable                                                  | CI/CD, scripting        |
| `prompt`    | No storage; the password is asked for when needed                                   | Maximum security, no auto-renewal |

Password resolution for silent token renewal walks a documented chain: the profile's configured
store first, then `keychain` → `encrypted` → `env` (`DC3_PASSWORD`); the first non-empty
password wins and unavailable stores are skipped without error. Saving always targets the
configured store only and reports honestly (`password_saved: false` plus a stderr warning when
the store is not available — e.g. the Windows Credential Manager PowerShell module is not
installed).

The encrypted store never writes a plaintext echo of any entry: the ciphertext alone is on disk
and only the separate random key file can decrypt it. Corrupt state files are quarantined to a
timestamped sibling with a warning instead of being silently reset.

### Device (`dc3 device`)

```bash
dc3 device list [--driver-id <id>] [--profile-id <id>] [--group-id <id>] [--offset <offset>] [--limit <limit>]
dc3 device get <id>
dc3 device add --name <name> --driver-id <id> --profile-id <id> [--description <description>] [--group-id <id>]
dc3 device update <id> --version <n> [--name <name>] [--driver-id <id>] [--profile-id <id>] [--description <description>]
dc3 device delete <id> --version <n>
dc3 device count --driver-id <id>
dc3 device status <id>
dc3 device import <file.xlsx> --driver-id <id> --profile-id <id> [--idempotency-key <key>] [--no-wait] [--poll-interval <ms>] [--wait-timeout <seconds>]
dc3 device import-template --driver-id <id> --profile-id <id> [--output <path>]
```

`device import` submits a canonical multipart request (`request` JSON part plus `file` XLSX
part) with an idempotency key (a UUIDv4 by default). By default it polls the returned durable
operation until a terminal status, with exponential backoff from `--poll-interval` (floor
100 ms) bounded by `--wait-timeout` (default 600 s, `0` gives up after the first poll; the
operation's own expiry can only tighten the deadline). A timeout ends with a structured
non-zero error (`kind: "timeout"`) instead of hanging. Terminal statuses `FAILED`,
`CANCELLED`, and `EXPIRED` exit 1. Use `--no-wait` to return the `202 Accepted` resource
immediately. `device import-template` downloads the matching XLSX template
(`device-import-template.xlsx` by default, override with `--output`) shaped for the given
driver/profile pair.

### Driver (`dc3 driver`)

```bash
dc3 driver list [--offset <offset>] [--limit <limit>]
dc3 driver get <id>
dc3 driver add --name <name> --service-name <name> --service-host <host> [--code <code>] [--type DRIVER_CLIENT]
dc3 driver update <id> --version <n> [--name <name>] [--service-name <name>] [--service-host <host>] [--type <type>]
dc3 driver delete <id> --version <n>
dc3 driver status <id>
```

### Point (`dc3 point`)

```bash
dc3 point list [--device-id <id>] [--profile-id <id>] [--offset <offset>] [--limit <limit>]
dc3 point get <id>
dc3 point read <id>
dc3 point history <id> --device-id <id> [--limit <limit>] [--cursor <cursor>]
dc3 point write <id> --device-id <id> --value <value>
dc3 point add --name <name> --profile-id <id> [--type FLOAT] [--rw READ_ONLY] [--value-decimal <n>] [--base-value <value>] [--multiple <value>] [--unit <unit>]
dc3 point update <id> --version <n> [--name <name>] [--profile-id <id>] [--type <type>] [--rw <rw>] [--value-decimal <n>] [--base-value <value>] [--multiple <value>] [--unit <unit>]
dc3 point delete <id> --version <n>
```

History is cursor-paged: each response carries the opaque `cursor` of the next page, passed
back as `--cursor`. `--limit` is the page size (default 100). `--base-value` and `--multiple`
are sent as JSON numbers to match the backend's decimal fields.

### Profile (`dc3 profile`)

```bash
dc3 profile list [--device-id <id>] [--type <type>] [--offset <offset>] [--limit <limit>]
dc3 profile get <id>
dc3 profile add --name <name> [--type <type>]
dc3 profile update <id> --version <n> [--name <name>] [--type <type>]
dc3 profile delete <id> --version <n>
```

### Group & Label (`dc3 group`, `dc3 label`)

```bash
dc3 group list [--offset <offset>] [--limit <limit>]
dc3 group get <id>
dc3 group add --name <name>
dc3 group update <id> --version <n> [--name <name>]
dc3 group delete <id> --version <n>

dc3 label list [--offset <offset>] [--limit <limit>]
dc3 label get <id>
dc3 label add --name <name> [--code <code>] [--color <color>]
dc3 label update <id> --version <n> [--name <name>] [--code <code>] [--color <color>]
dc3 label delete <id> --version <n>
```

### Event (`dc3 event`)

```bash
dc3 event list [--device-id <id>] [--profile-id <id>] [--offset <offset>] [--limit <limit>]
dc3 event get <id>
dc3 event history [--offset <offset>] [--limit <limit>]
dc3 event add --name <name> --profile-id <id> [--type INFO] [--level LOW]
dc3 event update <id> --version <n> [--name <name>] [--profile-id <id>] [--type <type>] [--level <level>]
dc3 event delete <id> --version <n>
```

### Command (`dc3 command`)

```bash
dc3 command list [--device-id <id>] [--profile-id <id>] [--offset <offset>] [--limit <limit>]
dc3 command get <id>
dc3 command add --name <name> --profile-id <id> [--type CUSTOM] [--call-type SYNC] [--timeout <seconds>]
dc3 command update <id> --version <n> [--name <name>] [--profile-id <id>] [--type <type>] [--call-type <type>] [--timeout <seconds>]
dc3 command delete <id> --version <n>
dc3 command call --device-id <id> --command-id <id> [--params '{"key":"value"}']
dc3 command history <recordId>
dc3 command history-list [--offset <offset>] [--limit <limit>]
```

### Alert (`dc3 alert`)

Overview and confirmation surface:

```bash
dc3 alert stats
dc3 alert list [--source device] [--offset <offset>] [--limit <limit>]
dc3 alert latest [--limit <limit>]
dc3 alert confirm --source device --id <id>
dc3 alert unconfirm --source device --id <id>
dc3 alert trend [--days <days>]
dc3 alert top-sources [--days <days>] [--limit <limit>]
dc3 alert type-distribution [--days <days>]
```

Deep-analysis surface (each supports `--days`, `--limit`, and repeatable `--query <key>=<value>`
passthrough unless noted):

```bash
dc3 alert activity [--days <days>] [--limit <limit>] [--query source=device]
dc3 alert storm-sources [--days <days>] [--limit <limit>] [--query source=device]
dc3 alert flapping [--days <days>] [--limit <limit>] [--query source=device]
dc3 alert correlation [--days <days>] [--limit <limit>] [--query source=device]
dc3 alert peer-deviation [--days <days>] [--limit <limit>] [--query source=device]
dc3 alert aging [--days <days>] [--limit <limit>] [--query source=device]
dc3 alert mtta [--days <days>] [--limit <limit>] [--query source=device]
dc3 alert change-impact [--days <days>] [--query source=device]
dc3 alert latency
dc3 alert silent-sources [--baseline-days <days>] [--silent-minutes <minutes>] [--limit <limit>]
dc3 alert coverage-gap [--limit <limit>]
dc3 alert point-profile <point_id>
dc3 alert bulk-confirm --args '{"items":[{"source":"device","id":789}]}'
dc3 alert bulk-confirm --args-file payload.json
```

`--query` is a true collector: every occurrence is kept and URL-encoded. `--args` / `--args-file`
are mutually exclusive and exactly one is required; the JSON must be an object.

### Dashboard (`dc3 dashboard`)

```bash
dc3 dashboard stats
dc3 dashboard timeseries [--granularity hour] [--range-hours <hours>]
dc3 dashboard top [--dimension device] [--range-hours <hours>] [--limit <limit>]
dc3 dashboard topology [--mode cardinality]
dc3 dashboard health
dc3 dashboard stream [--limit <limit>]
dc3 dashboard driver-stats
dc3 dashboard device-stats [--top-n <n>]
```

`--granularity` accepts `hour | day`, `--dimension` accepts `device | driver | point`, and
`--mode` accepts `cardinality | volume`.

### Topic (`dc3 topic`)

```bash
dc3 topic list [--offset <offset>] [--limit <limit>]
```

### Analytics (`dc3 analytics`) — AI data-analysis surface

Nine coarse-grained statistical reads over point time series; each op posts one JSON body and
returns a self-contained conclusion:

```bash
dc3 analytics list
dc3 analytics run query_history --args '{"pointId":456789,"days":7}'
dc3 analytics run trend_analysis --args '{"pointId":456789}'
dc3 analytics run query_latest --args-file payload.json
```

`--args` (inline JSON object) and `--args-file <path>` (JSON file; `-` reads stdin) are
mutually exclusive and exactly one is required. `--args-file` is the escape hatch for payloads
beyond the OS argv size limit (~32k characters on Windows).

### AI providers (`dc3 provider`)

```bash
dc3 provider list
dc3 provider add --name <name> --base-url <url> [--type OPENAI_COMPATIBLE] [--api-key <key>] [--default] [--enable] [--disable]
dc3 provider update <id> [--name <name>] [--base-url <url>] [--type ANTHROPIC] [--api-key <key>] [--default] [--enable] [--disable]
dc3 provider delete <id>
dc3 provider check --id <id> [--level BOTH] [--model <model>]
dc3 provider check --base-url <url> [--type OPENAI_COMPATIBLE] [--api-key <key>] [--level L1] [--model <model>]
```

`--type` accepts `OPENAI_COMPATIBLE | ANTHROPIC`; `--level` accepts `L1` (model list only),
`L2` (chat probe only), or `BOTH`. `check --base-url` is a draft-mode probe of an unsaved
endpoint. Updates send only the add-time field whitelist — audit and telemetry fields from the
list response are never echoed back, and an omitted `--api-key` keeps the stored one.

### AI models (`dc3 model`)

```bash
dc3 model list
dc3 model config-list
dc3 model add --model <model> --provider-id <id> [--label <label>] [--no-stream] [--no-tool-call] [--vision] [--reasoning] [--temperature <t>] [--max-tokens <n>] [--default] [--disable]
dc3 model update <id> [--model <model>] [--label <label>] [--provider-id <id>] [--no-stream] [--no-tool-call] [--vision] [--reasoning] [--temperature <t>] [--max-tokens <n>] [--default] [--disable]
dc3 model delete <id>
dc3 model check <id>
```

`--temperature` is validated to 0.0–2.0 and `--max-tokens` to a positive integer client-side.
Streaming, tool calling, vision, and reasoning default to on, off, off, and off respectively.

### Chat attachments (`dc3 attachment`)

```bash
dc3 attachment upload <file> --conversation-id <id>
dc3 attachment list --conversation-id <id>
```

Upload sends a real multipart/form-data `file` part (the filename rides the part disposition).

### Sessions & approvals (`dc3 session`, `dc3 action`)

Conversation lifecycle plus the high-risk tool-call approval loop:

```bash
dc3 session list [--offset <offset>] [--limit <limit>]
dc3 session get <conversation_id>
dc3 session messages <conversation_id>
dc3 session rename <conversation_id> --name <name>
dc3 session delete <conversation_id> [--yes]

dc3 action pending --conversation-id <conversation_id> [--offset <offset>] [--limit <limit>]
dc3 action confirm <action_id> [--yes]
dc3 action reject <action_id> [--yes]
```

`session delete` and `action confirm`/`reject` are destructive and gated by an interactive
confirmation; declining prints `Cancelled` and issues zero requests. Pass `--yes` to skip the
prompt (scripts, CI, non-interactive agents).

### Tool Catalog (`dc3 tools`)

MCP transport over the same OAuth ticket as REST (dual-transport design):

```bash
dc3 tools list
dc3 tools call read_device --args '{"deviceId":456789}'
dc3 tools call read_device --args-file payload.json
```

Requires `dc3 auth login --oauth`; classic login tickets are not accepted at `/mcp`. `--args`
and `--args-file` (file, or `-` for stdin) are mutually exclusive and exactly one is required;
the arguments JSON must be an object.

### Chat — AI Agent (`dc3 chat`)

```bash
dc3 chat "Is the temperature of device 1 normal?"
dc3 chat --model gpt-4o --stream "Analyze the device data"
dc3 chat --conversation-id <id> "Continue this conversation"
```

`--stream` renders the SSE deltas as they arrive and terminates on the `[DONE]` sentinel
without waiting for the server to close the connection.

## Global Options

Root options are recognized only **before** the first subcommand, e.g.
`dc3 --profile prod --format json device list`. After the subcommand, a root flag is a strict
unknown-option usage error, so leaf options like `device update <id> --version <n>` can never
be hijacked by the root `--version`.

| Option                       | Description                                                              |
| ---------------------------- | ------------------------------------------------------------------------ |
| `--profile <name>`           | Use a specific profile for this invocation (exit 1 if it does not exist) |
| `--format json\|table\|yaml` | Global output format                                                     |

### Output format resolution

Every command accepts its own `--format` option. The effective format is resolved as:

1. the command's `--format` option,
2. the global `--format` option,
3. the persisted `settings.output_format` (set once via `dc3 config set settings.output_format <fmt>`; unset by default),
4. the TTY default: `table` on an interactive terminal, `json` when piped.

An explicit but invalid or empty `--format` value (e.g. `--format ''`) is rejected with a usage
error; it is never silently replaced by the default.

## Multi-Profile

```bash
# Development
dc3 config profile create default
dc3 config profile use default
dc3 config set gateway http://localhost:8000
dc3 auth login

# Production
dc3 config profile create prod --gateway https://iot.example.com --switch
dc3 auth login

# Switch
dc3 config profile use default
dc3 device list
```

## AI Agent Integration

### Via Shell (any AI coding tool)

```bash
# Claude Code / Codex / Gemini CLI / Hermes / OpenCode can run:
dc3 device list --format json
dc3 point read 456789 --format json
dc3 dashboard health --format json
```

### Error contract (machines parse stdout, humans read stderr)

Every failure is reported through a single chokepoint with **stable exit codes** and a
machine-readable envelope on **stdout** whenever the effective format is `json` or `yaml`
(`table` keeps stdout human-clean):

- STDERR always gets exactly one human line: `Error: <message>`.
- STDOUT gets one JSON document:

```json
{
  "ok": false,
  "error": { "kind": "network", "code": "NETWORK", "message": "Network request failed (...)" }
}
```

Discriminate failures via `error.kind` — not via the exit code alone:

| `error.kind`   | Meaning                                            | Exit code |
| -------------- | -------------------------------------------------- | --------- |
| `usage`        | malformed command line (unknown option/command, invalid flag value, excess arguments) | 1 |
| `validation`   | business validation failed before or after a gateway round-trip                     | 1 |
| `auth`         | gateway rejected authentication (HTTP 401/403)                                      | 3 |
| `network`      | request never reached the gateway (DNS, socket, TLS)                                | 2 |
| `api`          | non-2xx gateway response (`code` carries `API_<status>` or `INTERNAL`)               | 1 |
| `timeout`      | a long-running operation exceeded its deadline                                      | 1 |

Success paths always emit exactly one machine document and exit 0; stderr stays empty.
Streaming surfaces (`chat --stream`) suppress the stdout envelope once partial output has been
written, so a late failure never appends a second document.

### Help and discovery

- Bare `dc3` prints the full help to **stdout** and exits **0** (treat it as the command
  index, not an error).
- `dc3 <group> --help` and `dc3 help <group> <subcommand> ...` print the resolved command's
  help to stdout with exit 0 — `help` resolves every path segment.
- An unknown command or unknown help target is a single-line stderr diagnostic plus the stdout
  envelope, exit 1 (never a help dump).

### Via Gateway MCP (any MCP-compatible tool)

Configure your AI tool to connect to the Gateway MCP endpoint. The configuration schema key is
`type` (not `transport`):

```jsonc
// Claude Code: .mcp.json
{
  "mcpServers": {
    "dc3": {
      "type": "http",
      "url": "http://localhost:8000/mcp",
    },
  },
}
```

The AI agent will discover all platform API tools automatically.

## Exit Codes

| Code | Meaning                                                                        |
| ---- | ------------------------------------------------------------------------------ |
| 0    | Success                                                                        |
| 1    | Business error: usage, validation, gateway API (incl. HTTP 404/409/5xx), timeout |
| 2    | Network error (gateway unreachable)                                            |
| 3    | Authentication error (needs login)                                             |

Within code 1, agents discriminate via the stdout envelope's `error.kind` (see the error
contract above).

## License

AGPL-3.0-or-later (see the repository's `LICENSE-AGPL.txt`).
