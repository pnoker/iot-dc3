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

import {on} from '../dispatch';
import {matches, paginate} from '../query';
import {newId, stamp} from '../crud';
import {fail, ok, responseOf} from '../response';
import {menuTree} from '../seed/menuTree';
import type {MenuNode} from '@/store/modules/menu';

type MenuRow = Omit<MenuNode, 'children'> & {
  createTime?: string;
  operateTime?: string;
  remark?: string;
};

/** Flatten the seed tree while preserving the parent relationship. */
const flattenMenu = (nodes: MenuNode[], parentMenuId = '0', level = 1): MenuRow[] => {
  const out: MenuRow[] = [];
  for (const node of nodes) {
    const {children, ...row} = node;
    out.push({
      ...row,
      parentMenuId,
      menuLevel: row.menuLevel || `C${Math.min(level, 3)}`,
      menuTypeFlag: row.menuTypeFlag || (children?.length ? 'TITLE' : 'COMMON'),
      enableFlag: row.enableFlag === 'ENABLED' ? 'ENABLE' : row.enableFlag,
      createTime: '2026-07-15 09:30:00',
      operateTime: '2026-08-01 14:20:00',
    });
    if (children?.length) out.push(...flattenMenu(children, String(node.id), level + 1));
  }
  return out;
};

const rows: MenuRow[] = flattenMenu(menuTree);

/** Rebuild the nested response expected by the router and tree selectors. */
const buildTree = (source: MenuRow[]): MenuNode[] => {
  const byParent = new Map<string, MenuRow[]>();
  for (const row of source) {
    const parent = String(row.parentMenuId ?? '0');
    const siblings = byParent.get(parent) || [];
    siblings.push(row);
    byParent.set(parent, siblings);
  }

  const nest = (row: MenuRow): MenuNode => {
    const children = (byParent.get(String(row.id)) || [])
      .slice()
      .sort((a, b) => Number(a.menuIndex ?? 0) - Number(b.menuIndex ?? 0))
      .map(nest);
    return children.length ? {...row, children} : {...row};
  };

  return (byParent.get('0') || [])
    .slice()
    .sort((a, b) => Number(a.menuIndex ?? 0) - Number(b.menuIndex ?? 0))
    .map(nest);
};

const findById = (id: unknown) => rows.find((row) => String(row.id) === String(id));

const descendantsOf = (id: string): Set<string> => {
  const descendants = new Set<string>([id]);
  let changed = true;
  while (changed) {
    changed = false;
    for (const row of rows) {
      if (descendants.has(String(row.parentMenuId)) && !descendants.has(String(row.id))) {
        descendants.add(String(row.id));
        changed = true;
      }
    }
  }
  return descendants;
};

const filterMenuRows = (body: Record<string, unknown> = {}): MenuRow[] => {
  const values = (value: unknown) => {
    if (Array.isArray(value)) return value.filter((item) => item !== '' && item !== 'ALL').map(String);
    if (value === undefined || value === null || value === '' || value === 'ALL') return [];
    return [String(value)];
  };
  const typeFilter = values(body.menuTypeFlag);
  const enableFilter = values(body.enableFlag);
  const matchesRow = (row: MenuRow) =>
    matches(row.menuName, body.menuName) &&
    matches(row.menuCode, body.menuCode) &&
    (typeFilter.length === 0 || typeFilter.includes(String(row.menuTypeFlag))) &&
    (enableFilter.length === 0 || enableFilter.includes(String(row.enableFlag)));

  const matched = rows.filter(matchesRow);
  if (matched.length === rows.length) return rows;
  const byId = new Map(rows.map((row) => [String(row.id), row]));
  const included = new Set<string>();
  for (const row of matched) {
    let current: MenuRow | undefined = row;
    const seen = new Set<string>();
    while (current && !seen.has(String(current.id))) {
      const id = String(current.id);
      seen.add(id);
      included.add(id);
      const parentId: string = String(current.parentMenuId ?? '0');
      current = parentId === '0' ? undefined : byId.get(parentId);
    }
  }
  return rows.filter((row) => included.has(String(row.id)));
};

export function registerMenuHandlers(): void {
  // Drives the router guard + Layout top nav. menu store takes res when
  // it is an array, so we wrap the tree in an R envelope.
  on('post', 'api/v3/auth/menu/list_tree', (ctx) =>
    responseOf(ctx.config, ok(buildTree(filterMenuRows(ctx.body || {})))),
  );

  // Menu management table (settingsMenu route).
  on('post', 'api/v3/auth/menu/list', (ctx) =>
    responseOf(
      ctx.config,
      ok(paginate(rows, ctx.body, (row) =>
        matches(row.menuName, ctx.body?.menuName) &&
        matches(row.menuCode, ctx.body?.menuCode) &&
        (ctx.body?.menuTypeFlag == null || ctx.body.menuTypeFlag === '' || ctx.body.menuTypeFlag === 'ALL' || String(row.menuTypeFlag) === String(ctx.body.menuTypeFlag)) &&
        (ctx.body?.enableFlag == null || ctx.body.enableFlag === '' || ctx.body.enableFlag === 'ALL' || String(row.enableFlag) === String(ctx.body.enableFlag)),
      )),
    ),
  );

  on('get', 'api/v3/auth/menu/get_by_id', (ctx) =>
    responseOf(ctx.config, ok(findById(ctx.params.id) || rows[0] || {})),
  );

  on('post', 'api/v3/auth/menu/add', (ctx) => {
    const parentId = String(ctx.body?.parentMenuId ?? '0');
    if (parentId !== '0' && !findById(parentId)) {
      return responseOf(ctx.config, fail('R4041', 'Parent menu not found', 404), 404);
    }
    const row: MenuRow = {
      ...ctx.body,
      id: newId(),
      parentMenuId: parentId,
      createTime: stamp(),
      operateTime: stamp(),
    };
    rows.push(row);
    return responseOf(ctx.config, ok(String(row.id)));
  });

  on('post', 'api/v3/auth/menu/update', (ctx) => {
    const index = rows.findIndex((row) => String(row.id) === String(ctx.body?.id));
    const current = index >= 0 ? rows[index] : undefined;
    if (!current) return responseOf(ctx.config, fail('R4041', 'Menu not found', 404), 404);
    const parentId = String(ctx.body?.parentMenuId ?? current.parentMenuId ?? '0');
    if (parentId !== '0' && !findById(parentId)) {
      return responseOf(ctx.config, fail('R4041', 'Parent menu not found', 404), 404);
    }
    // A parent that is the menu itself or one of its descendants detaches the
    // whole branch from the root — buildTree would silently drop it.
    if (parentId !== '0' && descendantsOf(String(current.id)).has(parentId)) {
      return responseOf(ctx.config, fail('R4042', 'Parent menu cannot be the menu itself or its descendant', 400), 400);
    }
    rows[index] = {...current, ...ctx.body, parentMenuId: parentId, operateTime: stamp()};
    return responseOf(ctx.config, ok(String(current.id)));
  });

  on(['post', 'delete'], 'api/v3/auth/menu/delete', (ctx) => {
    const id = String(ctx.params.id ?? ctx.body?.id ?? '');
    const ids = descendantsOf(id);
    const before = rows.length;
    for (let index = rows.length - 1; index >= 0; index -= 1) {
      const row = rows[index];
      if (row && ids.has(String(row.id))) rows.splice(index, 1);
    }
    return responseOf(ctx.config, ok(before === rows.length ? '' : id));
  });
}
