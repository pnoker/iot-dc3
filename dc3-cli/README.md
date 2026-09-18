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

# 2. Log in
dc3 auth login

# 3. Start using
dc3 device list
dc3 point read 456789
dc3 dashboard stats
```

## Commands

### Configuration (`dc3 config`)

```bash
dc3 config set gateway <url>           # Set gateway URL
dc3 config set auth.tenant <tenant>    # Set tenant code
dc3 config set auth.username <name>    # Set username
dc3 config set auth.store <type>       # Set credential store (keychain|encrypted|env|prompt)
dc3 config get <key>                   # Get config value
dc3 config list                        # List all profiles and settings
dc3 config profile use <name>          # Switch profile
dc3 config reset                       # Reset all config
```

### Authentication (`dc3 auth`)

```bash
dc3 auth login                         # Interactive login
dc3 auth login --tenant default --username admin --password xxx  # Non-interactive
dc3 auth login --store keychain        # Save password to OS keychain
dc3 auth login --store encrypted       # Save password to encrypted file
dc3 auth login --store env             # Read password from DC3_PASSWORD env
dc3 auth login --no-save               # Don't save password (manual re-login)
dc3 auth logout                        # Log out and invalidate token
dc3 auth status                        # Check auth status
dc3 auth token                         # Display current JWT token
dc3 auth token --header                # Display full auth headers as JSON
```

### Device (`dc3 device`)

```bash
dc3 device list [--driver-id] [--profile-id] [--offset] [--limit]
dc3 device get <id>
dc3 device create --name "..." --driver-id "..." --profile-id "..."
dc3 device update <id> --version <n> [--name "..."] [--driver-id "..."] [--profile-id "..."]
dc3 device delete <id> --version <n>
dc3 device count --driver-id "..."
dc3 device status <id>
dc3 device import <file.xlsx> --driver-id "..." --profile-id "..." [--idempotency-key "..."] [--no-wait]
```

`device import` submits a canonical multipart request (`request` JSON part plus `file` XLSX part) with an idempotency key. It polls the returned durable operation until a terminal status by default; use `--no-wait` to return the `202 Accepted` resource immediately.

### Driver (`dc3 driver`)

```bash
dc3 driver list [--offset] [--limit]
dc3 driver get <id>
dc3 driver create --name "..." --service-name "..." --service-host "..." [--type DRIVER_CLIENT]
dc3 driver update <id> --version <n> [--name "..."] [--service-name "..."] [--service-host "..."]
dc3 driver delete <id> --version <n>
dc3 driver status <id>
```

### Point (`dc3 point`)

```bash
dc3 point list [--device-id] [--profile-id] [--offset] [--limit]
dc3 point get <id>
dc3 point read <id>                                      # Read latest value
dc3 point history <id> --device-id <did> [--count 100]   # Read history
dc3 point write <id> --device-id <did> --value 25.5      # Write value
dc3 point create --name "..." --profile-id "..." [--type FLOAT] [--rw READ_ONLY]
dc3 point update <id> --version <n> [--name "..."] [--profile-id "..."] [--type FLOAT]
dc3 point delete <id> --version <n>
```

### Profile (`dc3 profile`)

```bash
dc3 profile list [--device-id] [--type] [--offset] [--limit]
dc3 profile get <id>
dc3 profile create --name "..." [--type "..."]
dc3 profile update <id> --version <n> [--name "..."] [--type USER]
dc3 profile delete <id> --version <n>
```

### Group & Label (`dc3 group`, `dc3 label`)

```bash
dc3 group list [--offset] [--limit]
dc3 group get <id>
dc3 group create --name "..."

dc3 label list [--offset] [--limit]
dc3 label get <id>
```

### Event (`dc3 event`)

```bash
dc3 event list [--device-id] [--profile-id] [--offset] [--limit]
dc3 event get <id>
dc3 event create --name "..." --profile-id "..." [--type INFO] [--level LOW]
dc3 event update <id> --version <n> [--name "..."] [--profile-id "..."]
dc3 event delete <id> --version <n>
dc3 event history [--offset] [--limit]
```

### Command (`dc3 command`)

```bash
dc3 command list [--device-id] [--profile-id] [--offset] [--limit]
dc3 command get <id>
dc3 command create --name "..." --profile-id "..." [--type CUSTOM] [--call-type SYNC] [--timeout 30]
dc3 command update <id> --version <n> [--name "..."] [--profile-id "..."]
dc3 command delete <id> --version <n>
dc3 command call --device-id "..." --command-id "..." [--params '{"k":"v"}']
dc3 command history <recordId>
dc3 command history-list [--offset] [--limit]
```

### Alert (`dc3 alert`)

```bash
dc3 alert stats                       # Alert overview
dc3 alert list [--source device|driver|point] [--offset] [--limit]
dc3 alert latest [--limit 10]         # Latest alerts
dc3 alert confirm --source device --id 789
dc3 alert unconfirm --source device --id 789
dc3 alert trend [--days 30]           # Trend analysis
dc3 alert top-sources [--days 30]     # Top alert sources
```

### Sessions & approvals (`dc3 session`, `dc3 action`)

Conversation lifecycle plus the high-risk tool-call approval loop:

```bash
dc3 session list
dc3 session messages conv-abc123
dc3 session rename conv-abc123 --name "boiler watch"
dc3 session delete conv-abc123

dc3 action pending --conversation-id conv-abc123   # offset page of tool calls awaiting approval
dc3 action pending --conversation-id conv-abc123 --offset 50 --limit 50
dc3 action confirm action-xyz789
dc3 action reject  action-xyz789
```

### Analytics (`dc3 analytics`) — AI data-analysis surface

Nine coarse-grained statistical reads over point time series (S19 agent face); each op posts
one JSON body and returns a self-contained conclusion:

```bash
dc3 analytics list
dc3 analytics run query_history --args '{"pointId":456789,"days":7}'
dc3 analytics run trend_analysis --args '{"pointId":456789}'
```

### Alert deep analysis (`dc3 alert …`)

Beyond stats/list/confirm, the alert group exposes the diagnosis surface:
activity, storm-sources, flapping, correlation, peer-deviation, aging, mtta, change-impact,
latency (histogram), silent-sources, coverage-gap, and bulk-confirm. Generic flags:

```bash
dc3 alert aging --days 7
dc3 alert silent-sources --baseline-days 3 --limit 50
dc3 alert storm-sources --query source=device      # extra k=v passthrough
dc3 alert bulk-confirm --args '{"items":[{"source":"device","id":789}]}'
```

### Dashboard (`dc3 dashboard`)

```bash
dc3 dashboard stats                   # Today statistics
dc3 dashboard timeseries [--granularity hour] [--range-hours 24]
dc3 dashboard top [--dimension device] [--range-hours 24] [--limit 10]
dc3 dashboard topology [--mode cardinality]
dc3 dashboard health                  # System + protocol health
dc3 dashboard stream [--limit 20]     # Real-time data stream
dc3 dashboard driver-stats            # Driver statistics
dc3 dashboard device-stats [--top-n 10]
```

### Topic (`dc3 topic`)

```bash
dc3 topic list [--offset] [--limit]
```

### Tool Catalog (`dc3 tools`)

MCP transport over the same OAuth ticket as REST (dual-transport design):

```bash
dc3 tools list                          # catalog visible to the ticket's scopes
dc3 tools call read_device --args '{"deviceId":456789}'
```

Requires `dc3 auth login --oauth`; classic login tickets are not accepted at `/mcp`.

### Chat — AI Agent (`dc3 chat`)

```bash
dc3 chat "检查1号设备的温度是否正常"
dc3 chat --model gpt-4o --stream "分析设备数据"
dc3 chat --conversation-id <id>       # Continue conversation
```

## Global Options

| Option                       | Description                                           |
| ---------------------------- | ----------------------------------------------------- |
| `--profile <name>`           | Use a specific config profile                         |
| `--format json\|table\|yaml` | Output format (default: table for TTY, json for pipe) |
| `--verbose`                  | Show request/response details                         |
| `--ci`                       | CI mode: no colors, json output, strict exit codes    |

## Multi-Profile

```bash
# Development
dc3 config profile use default
dc3 config set gateway http://localhost:8000
dc3 auth login

# Production
dc3 config profile use prod
dc3 config set gateway https://iot.example.com
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

### Via Gateway MCP (any MCP-compatible tool)

Configure your AI tool to connect to the Gateway MCP endpoint:

```jsonc
// Claude Code: .mcp.json
{
  "mcpServers": {
    "dc3": {
      "transport": "http",
      "url": "http://localhost:8000/mcp",
    },
  },
}
```

The AI agent will discover all platform API tools automatically.

## Credential Storage

| Store       | Description                                                                          | Use Case                           |
| ----------- | ------------------------------------------------------------------------------------ | ---------------------------------- |
| `keychain`  | OS-level keychain (macOS Keychain, Linux Secret Service, Windows Credential Manager) | Daily use (default)                |
| `encrypted` | AES-256-GCM encrypted file at `~/.dc3/credentials.enc`                               | Fallback when keychain unavailable |
| `env`       | `DC3_PASSWORD` environment variable                                                  | CI/CD, scripting                   |
| `prompt`    | Interactive prompt on every use                                                      | Maximum security, no auto-renewal  |

## Exit Codes

| Code | Meaning                                      |
| ---- | -------------------------------------------- |
| 0    | Success                                      |
| 1    | Business error (invalid input, auth failure) |
| 2    | Network error (gateway unreachable)          |
| 3    | Authentication error (needs login)           |

## License

MIT
