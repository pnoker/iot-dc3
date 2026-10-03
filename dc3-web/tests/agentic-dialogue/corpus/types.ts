/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

/**
 * Shared dialogue-test corpus schema. The same corpus drives the offline
 * deterministic suites (mock chat engine + SSE/store/render chain) and the
 * live LLM evaluation runner, so expectations are split into:
 *
 * - engine-level fields (`scenario`, `charts`, `events`) asserted offline
 *   against the scripted mock engine;
 * - `live` fields scored by the live evaluator against a real model reply.
 */

/** Topic buckets the mock chat engine can answer; `dashboard` is the fallback summary. */
export type ScenarioKey = 'temperature' | 'energy' | 'driver' | 'point' | 'dashboard';

/** Expected reply language for locale-sensitive assertions. */
export type ReplyLanguage = 'zh' | 'en';

/** Live-reply safety posture expected from the assistant. */
export type SafetyPosture = 'answer' | 'clarify' | 'refuse' | 'redirect';

export interface LiveExpectation {
  /**
   * Topic keywords of which at least one should appear in a relevant real
   * reply. Empty means relevance is judged only by the optional LLM judge.
   */
  keywords: string[];
  /** Expected handling of the request (refusal, clarification, direct answer). */
  safety?: SafetyPosture;
  /** Whether the reply should show tool usage in the agentic event stream. */
  toolUse?: 'required' | 'allowed' | 'absent';
  /** Free-text focus handed to the optional LLM judge. */
  judgeFocus?: string;
  /** Regex sources for content that must never leak into a reply (secrets, echoed payloads). */
  forbidden?: string[];
}

export interface CaseExpectations {
  /** Ideal mock-engine topic route for this utterance. */
  scenario: ScenarioKey;
  /**
   * Routes tolerated for this case. When `strictScenario` is set the route must
   * equal `scenario` exactly; otherwise any route in this list passes and the
   * offline report measures ideal-route fidelity separately.
   */
  scenarioAccept?: ScenarioKey[];
  /** Hard routing requirement — set for utterances that carry engine keywords. */
  strictScenario?: boolean;
  /** Whether the reply must be non-empty (defaults to true). */
  replyNonEmpty?: boolean;
  /** Expected reply language; asserted against the mock locale pipeline. */
  language: ReplyLanguage;
  /** The mock engine must attach visualization frames to this topic. */
  charts?: boolean;
  /** The mock engine must emit tool/reasoning trace events for this topic. */
  events?: boolean;
  /** Live-model scoring expectations. */
  live: LiveExpectation;
}

export interface DialogueCase {
  /** Stable unique case id, `<family>-<nnn>`. */
  id: string;
  /** Top-level category, e.g. `domain-temperature`. */
  family: string;
  /** Sub-variation label, e.g. `direct` or `implicit-paraphrase`. */
  subfamily: string;
  /** Language of the utterance(s); `mixed` covers code-switching and multilingual noise. */
  language: ReplyLanguage | 'mixed';
  /** Ordered user turns. Multi-turn cases carry follow-ups after the opener. */
  turns: string[];
  expectations: CaseExpectations;
  /** Optional human-readable intent note used by the live report. */
  notes?: string;
}
