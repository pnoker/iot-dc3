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

import type {ComposerTranslation} from 'vue-i18n';

import {listApi} from '@/api/api';
import {listDeviceByIds} from '@/api/device';
import {listDriverByIds} from '@/api/driver';
import {listPointByIds} from '@/api/point';
import {listProfileByIds} from '@/api/profile';
import {addResource, deleteResource, listResourceTree, updateResource} from '@/api/resource';
import {RESOURCE_SCOPE_OPTIONS, RESOURCE_TYPE_OPTIONS} from '@/config/constant/enums';
import {useMenuStore} from '@/store';
import type {ApiRecord, DeviceRecord, DriverRecord, PointRecord, ProfileRecord} from '@/config/types';
import type {EntityColumnContext, EntityListConfig} from '@/config/types/entityList';
import {authNameRules, positiveIntegerRules, remarkRules, requiredSelectRule} from '@/utils/formRuleUtil';
import {logger} from '@/utils/log';

type LinkableResourceType = 'DRIVER' | 'DEVICE' | 'POINT' | 'PROFILE' | 'API' | 'MENU';
type EntityRecord = DriverRecord | DeviceRecord | PointRecord | ProfileRecord | ApiRecord;

const LINKABLE_TYPES: LinkableResourceType[] = ['DRIVER', 'DEVICE', 'POINT', 'PROFILE', 'API', 'MENU'];

const ENTITY_ROUTE_MAP: Record<LinkableResourceType, string> = {
  DRIVER: 'driverDetail',
  DEVICE: 'deviceDetail',
  POINT: 'pointDetail',
  PROFILE: 'profileDetail',
  API: 'settingsApiDetail',
  MENU: 'settingsMenuDetail',
};

// Virtual grouping nodes registered by ResourceRegistrySync carry entity_id=0.
export const isGroupingNode = (row: Record<string, any>): boolean => {
  return !row.entityId || String(row.entityId) === '0';
};

const resourceType = (row: Record<string, any>): LinkableResourceType | undefined => {
  const type = String(row.resourceTypeFlag || '') as LinkableResourceType;
  return LINKABLE_TYPES.includes(type) ? type : undefined;
};

const isEntityLinkable = (row: Record<string, any>): boolean => {
  if (isGroupingNode(row)) return false;
  return Boolean(resourceType(row));
};

const formatEntityId = (row: Record<string, any>, ctx: EntityColumnContext): string => {
  if (isGroupingNode(row)) return '—';
  return ctx.relations.entityNames?.[String(row.entityId)] || String(row.entityId);
};

/** Resolve the detail route for a linkable entity row. Exported for the shell. */
export const resolveEntityRoute = (row: Record<string, any>): string | undefined => {
  const type = resourceType(row);
  if (!type) return undefined;
  return ENTITY_ROUTE_MAP[type];
};

// Resolve entityId → display name for the loaded resource rows. Aggregates ids
// by resource type and resolves each via its bulk-lookup wrapper; MENU names
// come from the cached pinia tree, APIs from the (capped) full list.
const resolveEntityNames = async (records: Record<string, any>[]): Promise<Record<string, string>> => {
  const map: Record<string, string> = {};
  const driverIds: string[] = [];
  const deviceIds: string[] = [];
  const pointIds: string[] = [];
  const profileIds: string[] = [];
  const apiIds: string[] = [];
  const menuIds: string[] = [];
  for (const r of records) {
    if (isGroupingNode(r)) continue;
    const id = String(r.entityId);
    switch (r.resourceTypeFlag) {
      case 'DRIVER':
        driverIds.push(id);
        break;
      case 'DEVICE':
        deviceIds.push(id);
        break;
      case 'POINT':
        pointIds.push(id);
        break;
      case 'PROFILE':
        profileIds.push(id);
        break;
      case 'API':
        apiIds.push(id);
        break;
      case 'MENU':
        menuIds.push(id);
        break;
    }
  }

  const fill = (ids: string[], res: R<Record<string, EntityRecord>>, nameKey: keyof EntityRecord) => {
    const data = res.data || {};
    ids.forEach((id) => {
      const item = data[id];
      const name = item?.[nameKey];
      if (name) map[id] = String(name);
    });
  };

  const promises: Promise<void>[] = [];
  if (driverIds.length)
    promises.push(
      listDriverByIds(driverIds)
        .then((r) => fill(driverIds, r, 'driverName'))
        .catch((e) => logger.debug('bulk name lookup failed', e))
    );
  if (deviceIds.length)
    promises.push(
      listDeviceByIds(deviceIds)
        .then((r) => fill(deviceIds, r, 'deviceName'))
        .catch((e) => logger.debug('bulk name lookup failed', e))
    );
  if (pointIds.length)
    promises.push(
      listPointByIds(pointIds)
        .then((r) => fill(pointIds, r, 'pointName'))
        .catch((e) => logger.debug('bulk name lookup failed', e))
    );
  if (profileIds.length)
    promises.push(
      listProfileByIds(profileIds)
        .then((r) => fill(profileIds, r, 'profileName'))
        .catch((e) => logger.debug('bulk name lookup failed', e))
    );
  // APIs have no bulk-lookup endpoint; pull the whole list (capped at 1000,
  // already 10x the realistic API count on a single tenant) and resolve from
  // that map.
  if (apiIds.length)
    promises.push(
      listApi({page: {size: 1000, current: 1}})
        .then((r) => {
          const apiRecords = r.data?.records || [];
          const byId = new Map(apiRecords.map((a) => [String(a.id), a.apiName]));
          apiIds.forEach((id) => {
            const name = byId.get(id);
            if (name) map[id] = name;
          });
        })
        .catch((e) => logger.debug('bulk name lookup failed', e))
    );
  // Menus are already cached in the pinia store for the top-nav; reuse the
  // cached tree instead of hitting the network again.
  if (menuIds.length) {
    const menuStore = useMenuStore();
    menuIds.forEach((id) => {
      const node = menuStore.findById(id);
      if (node) map[id] = node.menuName;
    });
  }
  await Promise.all(promises);
  return map;
};

// Ordered so the Parent picker always shows the same type ordering across
// tenants — missing types are skipped.
const TYPE_ORDER = ['MENU', 'API', 'DATA', 'DEVICE', 'POINT', 'PROFILE', 'DRIVER'];

const flattenTree = (nodes: any[]): any[] => {
  const out: any[] = [];
  const walk = (ns: any[]) => {
    for (const n of ns || []) {
      out.push(n);
      if (n.children) walk(n.children);
    }
  };
  walk(nodes || []);
  return out;
};

// Parent picker layout: a virtual "Root" (id=0) that commits top-level, plus
// one disabled group node per resource type with that type's resources nested
// underneath. Cross-type parent/child links in the source tree are dropped: a
// node parented to a different type becomes a root inside its own type group.
const buildParentTreeOptions = (t: ComposerTranslation, treeData: any[]) => {
  const flat = flattenTree(treeData || []);
  const buckets: Record<string, any[]> = {};
  for (const n of flat) {
    const type = String(n.resourceTypeFlag || 'OTHER');
    if (!buckets[type]) buckets[type] = [];
    buckets[type].push({
      id: n.id,
      parentResourceId: n.parentResourceId,
      resourceName: n.resourceName,
      resourceCode: n.resourceCode,
      resourceTypeFlag: type,
      children: [],
    });
  }
  const treesByType: Record<string, any[]> = {};
  for (const [type, nodes] of Object.entries(buckets)) {
    const byId = new Map<string, any>();
    for (const n of nodes) byId.set(String(n.id), n);
    const roots: any[] = [];
    for (const n of nodes) {
      const pid = n.parentResourceId != null ? String(n.parentResourceId) : null;
      const parent = pid && byId.get(pid);
      if (parent) parent.children.push(n);
      else roots.push(n);
    }
    treesByType[type] = roots;
  }
  const present = Object.keys(treesByType);
  const ordered = TYPE_ORDER.filter((x) => present.includes(x));
  for (const x of present) if (!ordered.includes(x)) ordered.push(x);

  const groups = ordered.map((type) => ({
    // Prefix the id so it can't collide with a real BIGINT resource id coming
    // back from the server. Disabled marks it unselectable.
    id: `__group_${type}`,
    resourceName: type,
    disabled: true,
    children: treesByType[type],
  }));
  return [{id: 0, resourceName: t('settings.resource.rootResource')}, ...groups];
};

interface ResourceHandlers {
  onEntityClick: (row: Record<string, any>) => void;
}

export const createResourceConfig = (t: ComposerTranslation, handlers: ResourceHandlers): EntityListConfig => ({
  name: 'resource',
  title: t('nav.settingsResource'),
  mode: 'tree',
  rowKey: 'id',
  defaultExpandAll: true,
  editable: true,
  searchFields: [
    {
      prop: 'resourceName',
      label: t('settings.resource.resourceName'),
      kind: 'input',
      placeholder: t('settings.resource.resourceNamePlaceholder'),
    },
    {
      prop: 'resourceCode',
      label: t('settings.resource.resourceCode'),
      kind: 'input',
      placeholder: t('settings.resource.resourceCodePlaceholder'),
    },
    {
      prop: 'resourceTypeFlags',
      label: t('settings.resource.resourceType'),
      kind: 'select',
      multiple: true,
      options: RESOURCE_TYPE_OPTIONS,
      placeholder: t('common.all'),
    },
    {
      prop: 'resourceScopeFlags',
      label: t('settings.resource.resourceScope'),
      kind: 'select',
      multiple: true,
      options: RESOURCE_SCOPE_OPTIONS,
      placeholder: t('common.all'),
    },
    {prop: 'enableFlag', label: t('common.enableFlag'), kind: 'enableFlag', includeAll: true},
  ],
  columns: [
    {prop: 'resourceName', label: t('settings.resource.resourceName'), minWidth: 220},
    {prop: 'resourceCode', label: t('settings.resource.resourceCode'), kind: 'code', minWidth: 180},
    {prop: 'serviceName', label: t('settings.resource.serviceName'), minWidth: 160},
    {prop: 'resourceTypeFlag', label: t('settings.resource.resourceType'), minWidth: 120},
    {prop: 'resourceScopeFlag', label: t('settings.resource.resourceScope'), minWidth: 100},
    {
      prop: 'entityId',
      label: t('settings.resource.entity'),
      kind: 'link',
      minWidth: 140,
      formatter: (row, ctx) => formatEntityId(row, ctx),
      linkable: (row) => isEntityLinkable(row),
      onClick: handlers.onEntityClick,
    },
    {prop: 'remark', label: t('common.remark'), minWidth: 140},
    {prop: 'enableFlag', label: t('common.enable'), kind: 'enable', width: 90},
    {prop: 'createTime', label: t('common.createTime'), kind: 'time', width: 165},
  ],
  relations: [
    {
      key: 'entityNames',
      load: async (rows) => {
        // Ensure the menu tree is cached before resolving MENU-typed entity
        // names — fetchTree is idempotent and skips the network if loaded.
        await useMenuStore().fetchTree();
        return resolveEntityNames(rows);
      },
    },
  ],
  fields: [
    {
      prop: 'parentResourceId',
      label: t('settings.resource.parentResourceId'),
      kind: 'treeSelect',
      required: true,
      placeholder: t('settings.resource.parentResourceIdPlaceholder'),
      tree: {
        load: () => listResourceTree({}).then((res) => res.data || []),
        transform: (rows) => buildParentTreeOptions(t, rows),
        props: {label: 'resourceName', children: 'children', disabled: 'disabled'},
        nodeKey: 'id',
        checkStrictly: true,
      },
    },
    {
      prop: 'resourceName',
      label: t('settings.resource.resourceName'),
      placeholder: t('settings.resource.resourceNamePlaceholder'),
      required: true,
      maxlength: 32,
      rules: authNameRules(t, t('common.entityResource')),
    },
    {
      prop: 'resourceCode',
      label: t('settings.resource.resourceCode'),
      placeholder: t('settings.resource.resourceCodePlaceholder'),
      maxlength: 32,
    },
    {
      prop: 'resourceTypeFlag',
      label: t('settings.resource.resourceType'),
      kind: 'select',
      required: true,
      placeholder: t('settings.resource.resourceTypePlaceholder'),
      options: [
        {label: 'DRIVER', value: 'DRIVER'},
        {label: 'PROFILE', value: 'PROFILE'},
        {label: 'POINT', value: 'POINT'},
        {label: 'DEVICE', value: 'DEVICE'},
        {label: 'DATA', value: 'DATA'},
        {label: 'MENU', value: 'MENU'},
        {label: 'API', value: 'API'},
      ],
      rules: requiredSelectRule(t('settings.resource.resourceTypePlaceholder')),
    },
    {
      prop: 'entityId',
      label: t('settings.resource.entityId'),
      kind: 'input',
      placeholder: t('settings.resource.entityIdPlaceholder'),
      maxlength: 19,
      rules: positiveIntegerRules(t, t('settings.resource.entityIdPlaceholder')),
    },
    {prop: 'enableFlag', label: t('common.enableFlag'), kind: 'enableFlag'},
    {
      prop: 'remark',
      label: t('common.remark'),
      kind: 'textarea',
      span: 24,
      maxlength: 300,
      rules: remarkRules(t),
    },
  ],
  defaultForm: () => ({
    parentResourceId: 0,
    resourceName: '',
    resourceCode: '',
    resourceTypeFlag: '',
    entityId: '',
    enableFlag: 'ENABLE',
    remark: '',
  }),
  rowEditable: (row) => !isGroupingNode(row),
  rowDeletable: (row) => !isGroupingNode(row),
  list: listResourceTree,
  add: addResource as EntityListConfig['add'],
  update: updateResource as EntityListConfig['update'],
  remove: deleteResource,
  detail: {routeName: 'settingsResourceDetail'},
  confirmDeleteText: t('settings.resource.confirmDelete'),
  emptyText: t('settings.resource.empty'),
});
