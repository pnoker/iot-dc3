# Frontend Three-Terminal UX Architecture (ADR)

Status: accepted · Scope: dc3-web (and future dc3 app client) · Drivers: first-principles UX engineering

## Context

dc3-web is a Vue 3 management console for IoT operators. Its experience targets three device classes (desktop, tablet,
mobile) and must later accommodate a native app as a fourth host of the mobile experience. The legacy codebase was
desktop-first: a hard `min-width: 1280px` floor on
`body` and the layout shell, fixed header columns, a fixed 220px settings aside, and no JavaScript breakpoint system.

Ad hoc responsive fixes (a few `@media` blocks, `el-col` responsive props on ten pages) proved that patches do not
scale: every new page reintroduces the desktop assumptions. This ADR replaces patching with an architecture derived from
first principles.

## First principles

Experience quality reduces to four physical quantities, valid on every device:

| Element    | Question                                               | Budget            |
|------------|--------------------------------------------------------|-------------------|
| Perceive   | Does the user notice key state instantly?              | < 100ms           |
| Understand | Is the information hierarchy readable at first glance? | first screen < 3s |
| Act        | Can the user complete a task with minimal motor cost?  | fewest steps      |
| Feedback   | Does every action answer visibly and predictably?      | < 300ms           |

Devices differ in exactly four variables: input modality (mouse vs thumb), viewport geometry, usage context (long
sessions vs alarm response), and compute/bandwidth. The product is an operations console: state monitoring plus
high-consequence actions over long sessions.

From these, the architecture derives seven axioms. Every axiom is a rule with a falsifiable violation signature so it
can be enforced in review and CI.

## Axioms

**A1. Content semantics are device-independent.** Entity models, operation models, and schemas are authored once and
consumed by every presentation host. *Violation signature:* a second copy of a field definition for a different device.

**A2. Presentation is rebuilt per device class, not shrunk.** Layout follows container geometry via fluid primitives
(`minmax`, `auto-fit`, wrapping flex); coarse device-class switches are the only job of breakpoints. *Violation
signature:* a growing pile of one-off `@media` patches.

**A3. Input capability decides interaction mode.** Hover, right-click, and inline editing are mouse language; 44x44
targets, bottom sheets, and swipes are thumb language. Pointer capability (fine/coarse, hover) is a runtime property,
not a media-query guess — a tablet with a keyboard still deserves mouse interactions. *Violation signature:* hover-only
actions, or touch targets below 44px.

**A4. Feedback latency is the perceived product.** 100ms synchronous feedback, <1s skeletons, optimistic updates beyond
that. *Violation signature:* a button that does nothing until the network answers.

**A5. Consistency carries the user's memory.** Tokens -> components -> page templates, each with a single source of
truth; no hardcoded colors, radii, or magic widths. *Violation signature:* non-token color/spacing literals in SCSS.

**A6. State is part of the experience.** Preferences (theme, density, locale), navigation position, and draft forms
survive device and session boundaries. *Violation signature:* user context reset on device switch or refresh.

**A7. Accessibility is the quality baseline.** Contrast, keyboard focus, reduced motion, and zoom are non-negotiable —
they double as the spec for outdoor/site inspection scenarios.

## Layer model

```text
L1 Semantic   entity model + operation model + schemas (summary/detail)   device-independent, single truth
L2 Tokens     design tokens + breakpoints + theme (light/dark/auto) + density  shared values
L3 Components ~40 shared components, normalized behavior (loading/empty/error, a11y)
L4 Patterns   4 page templates: monitor / list / detail / edit — three physical implementations each
L5 Quality    measurement -> gates -> regression -> revision (closed loop)
```

Each layer depends only on the one below it. Changes propagate downward, never sideways.

## Boundary discipline (reuse contract)

The web UI shell (L3/L4) is not reusable by a native app; the layers below the rendering boundary are. To keep that
reuse real, the following are forbidden in L1/L2 and in a future shared `dc3-sdk` package:

- imports of `@/config/*` Vue/Element Plus infrastructure (axios instance,
  `ElMessage`, vue-i18n `ComposerTranslation`);
- Element Plus or Vue types in entity schema definitions (labels are i18n keys, not translation functions);
- device-specific values in token sources (tokens are host-independent values; SCSS/CSS are one rendering of them).

Enforcement: ADR review + lint rules (no non-token literals in SCSS, no framework imports below L2).

## Breakpoint contract

Single contract, aligned with Element Plus `el-col` semantics (A5):

| Tier | Range         | Terminal               |
|------|---------------|------------------------|
| xs   | < 768px       | mobile                 |
| sm   | 768 - 991px   | tablet                 |
| md   | 992 - 1199px  | tablet / small desktop |
| lg   | 1200 - 1919px | desktop                |
| xl   | >= 1920px     | wide desktop           |

JavaScript (`useBreakpoint`) and CSS must both read this contract — no second, hand-rolled breakpoint set anywhere.

## Decisions (derived, not chosen)

1. Mobile navigation: drawer menu, not bottom tabs — the task distribution is long-tail (home/alarms frequent, 40+
   settings pages rare), which bottom tabs model badly.
2. Mobile tables: summary card lists driven by the L1 summary schema, not horizontal scrolling. Cross-row comparison —
   the table's purpose — is destroyed by horizontal panning; a sticky first column is the accepted interim state.
3. Dark mode: in scope, as a product of L2 tokens, for long-session eye load and sharper alarm contrast, not as
   decoration.
4. Visual layer: tokenized Element Plus, no bespoke component library — bespoke UI violates A5 economics (maintenance
   grows with component count).

## Acceptance criteria

- No page-level horizontal scroll from 360px to 2560px (table containers exempt);
  `document.documentElement.scrollWidth <= window.innerWidth`.
- Lighthouse mobile: perf >= 90, CLS <= 0.1, LCP <= 2.5s.
- 100% touch targets >= 44x44; dialogs full-screen below 768px.
- Zero hardcoded color/spacing literals in SCSS (token lint gate).
- Playwright runs desktop, tablet, and mobile viewport projects; axe scan clean; visual regression on critical pages.

## Revision

2026-08: v1 — adopted with the L2 token/breakpoint/theme foundation (dc3-web src/styles/tokens.scss, theme.scss,
src/composables/useBreakpoint.ts, src/store/modules/app.ts).

2026-08: v2 — shell three-terminal forms shipped: shared NavMenu (horizontal ellipsis / vertical drawer), Settings
sidebar menu extracted and hosted in aside (desktop) / collapsed rail (tablet) / drawer (mobile), responsive login
panels, compact mobile pagination in ToolCard. Verified via artifacts/viewport-check.mjs: zero page-level overflow and
correct per-terminal DOM at 1440/834/390px viewports against the mock build.

2026-08: v3 — L1 de-frameworking (dc3-client-sdk Phase 0): Translator contract replaces vue-i18n ComposerTranslation
across all 16 entity config modules; dc3-sdk extraction design documented in docs/design/dc3-client-sdk.md.

2026-08: v4 — measurement gate shipped: Playwright gains chromium-desktop (1440x900), chromium-tablet (834x1112, touch),
and chromium-mobile (393x851, touch) projects; tests/e2e/specs/responsive.spec.ts gates the A2 overflow criterion
(scrollWidth <= clientWidth on login/home/settings), A3 shell adaptation (menu strip vs drawer, aside vs drawer), and an
A7 accessible-name smoke probe. All 12 gate tests green against the mock build. CI (ci-web.yml) runs the gate
automatically via pnpm test:e2e with chromium only. Fixes recorded: playwright 1.61.1/1.62.0 version split aligned to
1.62.0; ToolCard refresh/sort icon buttons gained aria-labels. Lighthouse budget and axe-core scans remain CI follow-ups
(no new deps this iteration).
