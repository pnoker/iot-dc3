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

// Assistant UI state that must survive a refresh and travel in the URL:
// which mode the panel is in (closed / docked / workbench) and which
// conversation is selected. The URL query is the primary record (shareable,
// survives refresh as-is); localStorage mirrors it so in-app navigations
// that drop the query can still restore on reload.

/** Visibility mode of the assistant panel: hidden, docked beside the chat, or full-screen workbench. */
export type AgenticUiMode = 'closed' | 'open' | 'workbench';

/** Persisted assistant UI state: panel mode plus the selected conversation, if any. */
export interface AgenticUiState {
  mode: AgenticUiMode;
  conversationId?: string;
}

export const AGENTIC_UI_STATE_KEY = 'dc3-agentic-ui-state';

const MODES: AgenticUiMode[] = ['closed', 'open', 'workbench'];

/**
 * Read the persisted UI state; the URL query wins over localStorage.
 *
 * @param query current route query (agentic=<mode>, session=<id>)
 * @returns the restored state, or undefined when nothing was recorded
 */
export const readAgenticUiState = (query: Record<string, unknown>): AgenticUiState | undefined => {
  const fromUrl = parseQuery(query);
  if (fromUrl) return fromUrl;
  try {
    const raw = localStorage.getItem(AGENTIC_UI_STATE_KEY);
    if (!raw) return undefined;
    const parsed = JSON.parse(raw) as AgenticUiState;
    if (!parsed || !MODES.includes(parsed.mode)) return undefined;
    return { mode: parsed.mode, conversationId: parsed.conversationId || undefined };
  } catch {
    return undefined;
  }
};

/**
 * Mirror the UI state into localStorage (the URL is written separately
 * through the router so navigation history stays clean).
 *
 * @param state current assistant UI state
 */
export const writeAgenticUiState = (state: AgenticUiState): void => {
  try {
    localStorage.setItem(AGENTIC_UI_STATE_KEY, JSON.stringify(state));
  } catch {
    // storage may be unavailable (private mode) — the URL still carries state
  }
};

/**
 * Query params to merge into the route for the given state; undefined values
 * drop their param.
 *
 * @param state current assistant UI state
 * @returns query fragment with agentic/session keys
 */
export const agenticUiQuery = (state: AgenticUiState): Record<string, string | undefined> => ({
  agentic: state.mode === 'closed' ? undefined : state.mode,
  session: state.conversationId || undefined,
});

const parseQuery = (query: Record<string, unknown>): AgenticUiState | undefined => {
  const rawMode = typeof query.agentic === 'string' ? query.agentic : undefined;
  const rawSession = typeof query.session === 'string' ? query.session : undefined;
  if (rawMode === undefined && rawSession === undefined) return undefined;
  const mode: AgenticUiMode = rawMode !== undefined && MODES.includes(rawMode as AgenticUiMode) ? (rawMode as AgenticUiMode) : rawSession ? 'open' : 'closed';
  return { mode, conversationId: rawSession || undefined };
};
