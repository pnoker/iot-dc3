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

import type {CaseExpectations, DialogueCase, ReplyLanguage} from './types';

/**
 * Fill `{slot}` placeholders in a template.
 * @param template - template string containing `{slot}` placeholders
 * @param slots - slot values keyed by slot name
 * @returns the interpolated utterance
 */
export const fill = (template: string, slots: Record<string, string>): string =>
  template.replace(/\{(\w+)\}/g, (match, key: string) => slots[key] ?? match);

/**
 * Expand one template across every slot combination (cartesian product).
 * @param template - template string containing `{slot}` placeholders
 * @param slotLists - candidate values per slot name
 * @returns one interpolated utterance per combination
 */
export const expand = (template: string, slotLists: Record<string, string[]>): string[] => {
  const keys = Object.keys(slotLists);
  let rows: Record<string, string>[] = [{}];
  for (const key of keys) {
    const next: Record<string, string>[] = [];
    for (const row of rows) {
      for (const value of slotLists[key] ?? []) {
        next.push({...row, [key]: value});
      }
    }
    rows = next;
  }
  return rows.map((row) => fill(template, row));
};

/**
 * Expand a list of complete templates across one shared slot combination set.
 * @param templates - complete templates, each containing `{slot}` placeholders
 * @param slotLists - candidate values per slot name
 * @returns one interpolated utterance per template × combination
 */
export const crossTemplates = (templates: string[], slotLists: Record<string, string[]>): string[] =>
  templates.flatMap((template) => expand(template, slotLists));

interface FamilyOptions {
  family: string;
  subfamily: string;
  language: DialogueCase['language'];
  expectations: CaseExpectations;
  notes?: string;
}

/**
 * Turn raw utterances into fully identified cases of one family. Entries are
 * either a single-turn string or a multi-turn script produced by {@link turns}.
 * @param options - family-wide metadata shared by every generated case
 * @param utterances - one entry per case
 * @returns the generated cases with stable `<family>-<nnn>` ids
 */
export const family = (options: FamilyOptions, utterances: Array<string | string[]>): DialogueCase[] =>
  utterances.map((utterance, index) => ({
    id: `${options.family}-${String(index + 1).padStart(3, '0')}`,
    family: options.family,
    subfamily: options.subfamily,
    language: options.language,
    turns: Array.isArray(utterance) ? utterance : [utterance],
    expectations: options.expectations,
    ...(options.notes ? {notes: options.notes} : {}),
  }));

/** Single-turn convenience wrapper around {@link family}. */
export const single = (options: FamilyOptions, utterances: string[]): DialogueCase[] =>
  family(options, utterances);

/** Build one multi-turn script entry from its ordered user turns. */
export const turns = (...parts: string[]): string[] => parts;

/**
 * Classify whether an utterance carries one of the mock engine's routing
 * keywords. Used by corpus self-checks, never to decide expectations — those
 * are declared per family so they stay reviewable.
 * @param text - utterance to inspect
 * @returns true when the utterance contains a routing keyword
 */
export const hasEngineKeyword = (text: string): boolean =>
  /温度|temp|发热|过热|能耗|用电|电量|energy|功率|驱动|driver|掉线|负载|位号|point|点位|实时/i.test(text);

export const zhOnly = (text: string): ReplyLanguage => (/[\u3400-\u9fff]/u.test(text) ? 'zh' : 'en');
