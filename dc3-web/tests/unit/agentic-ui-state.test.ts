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

import {beforeEach, describe, expect, it} from 'vitest';

import {
  AGENTIC_UI_STATE_KEY,
  agenticUiQuery,
  readAgenticUiState,
  writeAgenticUiState,
} from '@/components/agentic/agenticUiState';

describe('agentic UI state persistence', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('prefers the URL query over localStorage', () => {
    writeAgenticUiState({mode: 'open', conversationId: 'from-storage'});
    const state = readAgenticUiState({agentic: 'workbench', session: 'from-url'});
    expect(state).toEqual({mode: 'workbench', conversationId: 'from-url'});
  });

  it('falls back to localStorage when the URL carries nothing', () => {
    writeAgenticUiState({mode: 'workbench', conversationId: 'conv-1'});
    expect(readAgenticUiState({})).toEqual({mode: 'workbench', conversationId: 'conv-1'});
  });

  it('tolerates corrupt storage and unknown modes', () => {
    localStorage.setItem(AGENTIC_UI_STATE_KEY, '{not json');
    expect(readAgenticUiState({})).toBeUndefined();
    localStorage.setItem(AGENTIC_UI_STATE_KEY, JSON.stringify({mode: 'banana'}));
    expect(readAgenticUiState({})).toBeUndefined();
  });

  it('builds query params that drop closed/empty state', () => {
    expect(agenticUiQuery({mode: 'closed'})).toEqual({agentic: undefined, session: undefined});
    expect(agenticUiQuery({mode: 'open', conversationId: 'conv-9'})).toEqual({
      agentic: 'open',
      session: 'conv-9',
    });
    expect(agenticUiQuery({mode: 'workbench'})).toEqual({agentic: 'workbench', session: undefined});
  });

  it('restores a bare session param as at least an open panel', () => {
    expect(readAgenticUiState({session: 'conv-3'})).toEqual({mode: 'open', conversationId: 'conv-3'});
  });
});
