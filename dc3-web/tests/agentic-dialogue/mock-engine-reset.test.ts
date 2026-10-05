/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import {describe, expect, it} from 'vitest';

import {completeAgenticChatCompletion} from '@/api/agentic';
import {db} from '@/mock/db';

import {ensureMockChatEngine, resetMockChatEngine} from './helpers/chatHarness';

/**
 * The mock db is a module singleton and the chat engine persists every turn
 * into it. Corpus cases stay isolated by construction (each turn gets a
 * fresh conversation id); this file guards the escape hatch for tests that
 * deliberately reuse one conversation id — resetMockChatEngine must clear
 * the accumulated turns so earlier cases cannot bleed into later ones.
 */
describe('mock chat engine reset', () => {
  const fixedTurn = (conversationId: string) =>
    completeAgenticChatCompletion({
      model: 'dc3-agentic',
      messages: [{role: 'user', content: '风机温度趋势如何'}],
      stream: false,
      conversationId,
      temperature: undefined,
      maxTokens: undefined,
      attachments: [],
      reasoning: false,
    });

  const persistedTurns = (conversationId: string) =>
    db.agenticMessages.filter((message) => String(message.conversationId) === conversationId);

  it('clears accumulated agentic collections so a reused conversationId starts clean', async () => {
    ensureMockChatEngine();
    resetMockChatEngine();
    expect(db.agenticMessages).toHaveLength(0);

    await fixedTurn('fixed-conversation');
    const firstRun = persistedTurns('fixed-conversation');
    expect(firstRun.length).toBeGreaterThan(0);

    // Without the reset the second run over the same conversation id would
    // read the first run's rows (e.g. messageIndex continuation) and both
    // runs' rows would pile up.
    resetMockChatEngine();
    expect(db.agenticMessages).toHaveLength(0);

    await fixedTurn('fixed-conversation');
    expect(persistedTurns('fixed-conversation')).toHaveLength(firstRun.length);
  });
});
