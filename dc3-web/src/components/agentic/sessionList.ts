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

import type {AgenticSession} from '@/config/types';

/** Session row as consumed by the sessions rail and the docked history dropdown. */
export type SessionListItem = AgenticSession & {title: string};

/**
 * Archived sessions live in their own folded section (sessions rail) and never
 * mix into a live switcher. Shared by the rail and the docked history dropdown
 * so both surfaces apply the exact same rule — one data contract, two views.
 *
 * @param session - session row (sessionExt already normalized by the store)
 * @returns true when the session carries the archive mark
 */
export const isSessionArchived = (session: AgenticSession): boolean => Boolean(session.sessionExt?.archived);

/**
 * Title+summary substring match shared by the rail search box and the docked
 * history dropdown filter — one search contract on both surfaces.
 *
 * @param session - session row to test
 * @param query - the user's raw search input; blank matches everything
 * @returns true when the session matches the query
 */
export const sessionMatchesSearch = (session: SessionListItem, query: string): boolean => {
  const needle = query.trim().toLowerCase();
  if (!needle) return true;
  return `${session.title} ${session.summary || ''}`.toLowerCase().includes(needle);
};
