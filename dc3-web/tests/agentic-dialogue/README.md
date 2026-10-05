# Agentic Assistant Dialogue Test Suite

Comprehensive dialogue tests for the AI assistant panel (`dc3-web` Agentic chat):
**1000+ generated phrasing cases** covering domain queries, conversational
behavior, and hostile/degenerate input, executed against the full offline
dialogue chain, plus an env-gated live LLM evaluation runner for a real backend.

## Layout

| Path | Purpose |
|---|---|
| `corpus/` | Shared dialogue corpus: `types.ts` (schema), `builders.ts` (template expansion), `families/` (domain / conversation / robustness case families), `index.ts` (assembly + validation) |
| `helpers/chatHarness.ts` | Runs cases through the real `@/api/agentic` SSE transport + the scripted mock chat engine (`src/mock/fetch.ts`) |
| `helpers/report.ts` | Report writers (`reports/*.json`, `reports/*.md`) |
| `dialogue-corpus.test.ts` | Corpus self-check: ≥1000 cases, unique ids, expectation consistency |
| `dialogue-contract.test.ts` | Contract tier: every case, streaming + blocking — clean termination, parseable SSE, non-empty reply |
| `dialogue-routing.test.ts` | Reply-quality tier: topic routing fidelity, reply language, charts/trace structure; renders `reports/offline-dialogue-report.md` |
| `dialogue-multiturn.test.ts` | Multi-turn continuity through the real Pinia store (transcript growth, roles, request shape, conversation-id reuse) |
| `dialogue-hardening.test.ts` | Rendering hardening: hostile payloads stay inert through marked → DOMPurify → `v-html` |
| `live/` | Live LLM evaluation: SSE client (`sse.ts`), rule scorer + optional LLM judge (`evaluator.ts`), env-gated runner (`dialogue-live.eval.test.ts`) |
| `reports/` | Generated artifacts (gitignored) |

## Realistic conversation scenarios

Unlike the per-utterance coverage corpus, **scenario tests** run coherent multi-turn
dialogues (8-11 turns per session) that mirror real user behavior — one session per
scenario, testing context continuity, tool call timing, topic management, and safety
boundaries mid-conversation.

| Path | Purpose |
|---|---|
| `corpus/scenarios.ts` | 12 business scenarios (inspection, alarm handling, energy audit, troubleshooting, topic switching, onboarding, emergency, security probes, corrections, batch analysis, casual→business, deep dive) |
| `scenario-corpus.test.ts` | Scenario integrity self-check |
| `live/scenario-live.eval.test.ts` | Live evaluation runner (env-gated) |

```powershell
# Live scenario evaluation (same env contract as dialogue-live)
$env:AGENTIC_SCENARIO_BASE_URL = 'http://127.0.0.1:8000'
pnpm --dir dc3-web eval:agentic-scenarios
```

Output: `reports/scenario-eval-report.md` with per-scenario turn-by-turn results.

## Running

```bash
# whole dialogue suite (offline, deterministic)
pnpm --dir dc3-web test:agentic-dialogue

# single tiers
pnpm --dir dc3-web vitest run tests/agentic-dialogue/dialogue-contract.test.ts
pnpm --dir dc3-web vitest run tests/agentic-dialogue/dialogue-routing.test.ts
```

The offline report is written to `tests/agentic-dialogue/reports/offline-dialogue-report.md`
(run `dialogue-contract.test.ts` first or in the same invocation so the report
picks up contract metrics).

## Live full-chain evaluation

The live runner logs in through the real auth flow (`/api/v3/auth/token/salt` →
`/token/generate`, httpOnly token cookie), replays the corpus against the
running stack (`POST /api/v3/agentic/chat/completions`, SSE — one user message
per request with server-side memory replay, exactly like the web client), and
scores each reply with hard rules (termination, non-empty, declared topic
keywords, secret-leakage regexes, refusal/compliance posture) plus soft
heuristics and an optional LLM judge. Session listing and per-conversation
message replay are verified through the gateway afterwards.

| Variable | Meaning |
|---|---|
| `AGENTIC_EVAL_BASE_URL` | Gateway/base URL of the running stack — enables the suite |
| `AGENTIC_EVAL_TENANT` / `AGENTIC_EVAL_LOGIN` | Tenant/login (default `default` / `dc3`) |
| `AGENTIC_EVAL_PASSWORD` | Login password (default the seeded dev credential `dc3dc3dc3`) |
| `AGENTIC_EVAL_COOKIE` | Skip login and use this raw `Cookie` header instead |
| `AGENTIC_EVAL_MODEL` | Model name sent in requests (default `dc3-agentic`) |
| `AGENTIC_EVAL_CONCURRENCY` | Parallel conversations (default `4`) |
| `AGENTIC_EVAL_TIMEOUT_MS` | Per-case timeout (default `120000`) |
| `AGENTIC_EVAL_FAMILY_FILTER` | Regex limiting evaluation to matching families (smoke runs) |
| `AGENTIC_EVAL_LIMIT` | Evaluate only the first N selected cases (smoke runs) |
| `AGENTIC_EVAL_JUDGE_URL` | Optional OpenAI-compatible judge base URL |
| `AGENTIC_EVAL_JUDGE_KEY` / `AGENTIC_EVAL_JUDGE_MODEL` | Judge credentials (default model `gpt-4o`) |

```powershell
# pwsh
$env:AGENTIC_EVAL_BASE_URL = 'http://127.0.0.1:8000'
pnpm --dir dc3-web eval:agentic-dialogue
```

Output: `reports/live-eval-report.md` and `reports/live-eval-<timestamp>.json`
with per-family pass rates, latency, and failing case ids.

## Corpus conventions

- Cases are generated deterministically from templates (`builders.ts`), so ids
  and counts are stable; `dialogue-corpus.test.ts` guards the shape.
- `expectations.scenario` is the ideal mock-engine topic route;
  `strictScenario` families carry engine keywords and must route exactly,
  while paraphrase families accept a fallback and are measured as routing
  fidelity in the report.
- `expectations.live` carries the scoring contract used by the live runner
  (`keywords`, `safety`, `toolUse`, `forbidden`, `judgeFocus`).
