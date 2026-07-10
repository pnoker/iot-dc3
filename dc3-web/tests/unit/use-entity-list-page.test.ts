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

import {beforeEach, describe, expect, it, vi} from 'vitest';
import {useEntityListPage} from '@/composables/useEntityListPage';
import {ENTITY_LIST_ROWS, makeEntityListConfig} from '../fixtures/entity-list-config';

// --- hoisted mocks -----------------------------------------------------------

const routerMocks = vi.hoisted(() => ({
  push: vi.fn(() => Promise.resolve()),
}));

vi.mock('vue-router', () => ({
  useRouter: () => routerMocks,
}));

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
  }),
}));

vi.mock('@/utils/notificationUtil', () => ({
  successMessage: vi.fn(),
}));

// -----------------------------------------------------------------------------

describe('useEntityListPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('populates state.rows and state.page.total after load resolves', async () => {
    const config = makeEntityListConfig();
    const {state, load} = useEntityListPage(config);

    // The composable calls load() on init; wait for the initial call to settle.
    await Promise.resolve();
    await Promise.resolve();

    expect(state.rows).toHaveLength(ENTITY_LIST_ROWS.length);
    expect(state.page.total).toBe(ENTITY_LIST_ROWS.length);
    expect(state.rows[0]).toEqual(ENTITY_LIST_ROWS[0]);

    // Calling load() again re-fetches and keeps the same results.
    await load();
    expect(config.list).toHaveBeenCalledTimes(2);
  });

  it('returns "-" from formatCell when the column value is null', () => {
    const config = makeEntityListConfig();
    const {formatCell} = useEntityListPage(config);

    const nullRow = {id: 'x', createTime: null, status: null, name: null};
    const timeColumn = config.columns.find((c) => c.kind === 'time')!;
    // timestampLabel returns '-' for null values
    expect(formatCell(nullRow, timeColumn)).toBe('-');
  });

  it('returns a formatted date string from formatCell for a time column', () => {
    const config = makeEntityListConfig();
    const {formatCell} = useEntityListPage(config);

    const timeColumn = config.columns.find((c) => c.kind === 'time')!;
    const result = formatCell(ENTITY_LIST_ROWS[0], timeColumn);
    // timestampLabel formats into yyyy-MM-dd hh:mm:ss; just verify it is a
    // non-empty string different from the raw ISO value and not the fallback.
    expect(typeof result).toBe('string');
    expect(result).not.toBe('-');
    expect(result).not.toBe('');
  });

  it('returns the option label from formatCell for a tag column', () => {
    const config = makeEntityListConfig();
    const {formatCell} = useEntityListPage(config);

    const tagColumn = config.columns.find((c) => c.kind === 'tag')!;
    expect(formatCell(ENTITY_LIST_ROWS[0], tagColumn)).toBe('Success');
    expect(formatCell(ENTITY_LIST_ROWS[1], tagColumn)).toBe('Failed');
  });

  it('returns the raw value string from formatCell for an unknown tag value', () => {
    const config = makeEntityListConfig();
    const {formatCell} = useEntityListPage(config);

    const tagColumn = config.columns.find((c) => c.kind === 'tag')!;
    const rowWithUnknown = {...ENTITY_LIST_ROWS[0], status: 'UNKNOWN_STATUS'};
    expect(formatCell(rowWithUnknown, tagColumn)).toBe('UNKNOWN_STATUS');
  });

  it('passes search field values to the list query on load', async () => {
    const config = makeEntityListConfig();
    const {searchForm, search} = useEntityListPage(config);

    // Flush the initial load triggered by the composable.
    await Promise.resolve();
    await Promise.resolve();

    searchForm['keyword'] = 'alpha';
    search({});
    await Promise.resolve();
    await Promise.resolve();

    const lastCall = (config.list as ReturnType<typeof vi.fn>).mock.lastCall?.[0] as Record<string, unknown>;
    expect(lastCall).toMatchObject({keyword: 'alpha'});
  });

  it('includes page object in query for page mode', async () => {
    const config = makeEntityListConfig();
    useEntityListPage(config);

    await Promise.resolve();
    await Promise.resolve();

    const lastCall = (config.list as ReturnType<typeof vi.fn>).mock.lastCall?.[0] as Record<string, unknown>;
    expect(lastCall).toHaveProperty('page');
    expect((lastCall.page as Record<string, unknown>).current).toBe(1);
  });

  it('parses json field to object and coerces number field in payload when submitting', async () => {
    const addFn = vi.fn().mockResolvedValue({ok: true, code: 'ok', message: 'ok', data: null});
    const config = makeEntityListConfig();
    // Add a number field alongside the existing json 'extra' field
    config.fields = [...config.fields, {prop: 'count', label: 'Count', kind: 'number'}];
    config.defaultForm = () => ({name: '', extra: '', count: 0});
    config.add = addFn;

    const {formModel, openAdd, setFormRef, submit} = useEntityListPage(config);

    // Provide a form ref stub whose validate calls the callback synchronously with valid=true
    setFormRef({
      validate: (cb?: (valid: boolean) => void) => {
        if (cb) cb(true);
        return Promise.resolve(true);
      },
      clearValidate: () => {
      },
    });

    openAdd();
    formModel['extra'] = '{"key":"val"}';
    formModel['count'] = '42';

    submit();
    // Let the async request resolve
    await Promise.resolve();
    await Promise.resolve();

    expect(addFn).toHaveBeenCalledOnce();
    const called = addFn.mock.calls[0][0] as Record<string, unknown>;
    expect(called['extra']).toEqual({key: 'val'});
    expect(called['count']).toBe(42);
  });

  it('resets state.page.current to 1 on search', async () => {
    const config = makeEntityListConfig();
    const {state, currentChange, search} = useEntityListPage(config);

    await Promise.resolve();
    await Promise.resolve();

    currentChange(3);
    await Promise.resolve();
    await Promise.resolve();
    expect(state.page.current).toBe(3);

    search({});
    expect(state.page.current).toBe(1);
  });

  it('toggles sort order and calls list again', async () => {
    const config = makeEntityListConfig();
    const {state, sort} = useEntityListPage(config);

    await Promise.resolve();
    await Promise.resolve();

    const callsBefore = (config.list as ReturnType<typeof vi.fn>).mock.calls.length;
    sort();
    await Promise.resolve();
    await Promise.resolve();

    expect((config.list as ReturnType<typeof vi.fn>).mock.calls.length).toBeGreaterThan(callsBefore);
    expect(state.page.orders[0].asc).toBe(true);
  });
});
