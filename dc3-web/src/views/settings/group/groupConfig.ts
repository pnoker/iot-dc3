/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import type {ComposerTranslation} from 'vue-i18n';

import {addGroup, deleteGroup, listGroup, updateGroup} from '@/api/group';
import {ENTITY_TYPE_OPTIONS} from '@/config/constant/enums';
import type {GroupRecord} from '@/config/types/manager';
import type {EntityListConfig} from '@/config/types/entityList';
import {nameRules, remarkRules} from '@/utils/formRuleUtil';

const GROUP_PAGE_QUERY = {page: {current: 1, size: 5000, orders: [{column: 'group_index', asc: true}]}};

const loadGroupRecords = async (): Promise<GroupRecord[]> => {
  const res = await listGroup(GROUP_PAGE_QUERY);
  return (res.data?.records || []) as GroupRecord[];
};

/** Build a sorted tree from flat GroupRecord rows. */
const buildTree = (rows: GroupRecord[]): GroupRecord[] => {
  const byId = new Map<string, GroupRecord & {children: GroupRecord[]}>();
  rows.forEach((row) => byId.set(String(row.id), {...row, children: []}));
  const roots: (GroupRecord & {children: GroupRecord[]})[] = [];
  byId.forEach((node) => {
    const parentId = node.parentGroupId ? String(node.parentGroupId) : '';
    const parent = parentId ? byId.get(parentId) : undefined;
    if (parent) {
      parent.children = parent.children || [];
      parent.children.push(node);
    } else {
      roots.push(node);
    }
  });
  const sort = (nodes: GroupRecord[]) => {
    nodes.sort((a, b) => Number(a.groupIndex ?? 0) - Number(b.groupIndex ?? 0));
    nodes.forEach((node) => sort((node.children as GroupRecord[]) || []));
  };
  sort(roots);
  return roots;
};

/** Compute set of IDs to exclude: current node + all its descendants (anti-cycle). */
const computeExcluded = (rows: GroupRecord[], currentId: string): Set<string> => {
  const ids = new Set<string>();
  if (!currentId) return ids;
  const childrenByParent = new Map<string, GroupRecord[]>();
  rows.forEach((row) => {
    const parentId = row.parentGroupId ? String(row.parentGroupId) : '';
    if (!childrenByParent.has(parentId)) childrenByParent.set(parentId, []);
    childrenByParent.get(parentId)!.push(row);
  });
  const visit = (id: string) => {
    ids.add(id);
    (childrenByParent.get(id) || []).forEach((c) => visit(String(c.id)));
  };
  visit(currentId);
  return ids;
};

const normalizeGroupPayload = (payload: Record<string, unknown>) => {
  const next = {...payload};
  if (!next.parentGroupId || String(next.parentGroupId) === '0') next.parentGroupId = null;
  return next;
};

export const createGroupConfig = (t: ComposerTranslation): EntityListConfig => ({
  name: 'group',
  title: t('nav.settingsGroup'),
  editable: true,
  searchFields: [
    {
      prop: 'groupName',
      label: t('settings.group.groupName'),
      kind: 'input',
      placeholder: t('settings.group.groupNamePlaceholder'),
    },
    {prop: 'groupTypeFlag', label: t('settings.common.entityType'), kind: 'select', options: ENTITY_TYPE_OPTIONS},
    {prop: 'enableFlag', label: t('common.enableFlag'), kind: 'enableFlag', includeAll: true},
  ],
  columns: [
    {prop: 'groupName', label: t('settings.group.groupName'), minWidth: 160},
    {prop: 'groupCode', label: t('settings.group.groupCode'), kind: 'code', minWidth: 150},
    {prop: 'groupTypeFlag', label: t('settings.common.entityType'), width: 110},
    {
      prop: 'parentGroupId',
      label: t('settings.group.parentGroupId'),
      minWidth: 150,
      formatter: (row, ctx) => {
        if (!row.parentGroupId || String(row.parentGroupId) === '0') return t('settings.group.rootGroup');
        return ctx.relations.parentName?.[String(row.parentGroupId)] || '-';
      },
    },
    {prop: 'enableFlag', label: t('common.enable'), kind: 'enable', width: 90},
    {prop: 'remark', label: t('common.remark'), minWidth: 180},
    {prop: 'createTime', label: t('common.createTime'), kind: 'time', width: 165},
  ],
  relations: [
    {
      key: 'parentName',
      load: async () => {
        const records = await loadGroupRecords();
        const map: Record<string, string> = {};
        records.forEach((g) => {
          map[String(g.id)] = g.groupName || String(g.id);
        });
        return map;
      },
    },
  ],
  fields: [
    {
      prop: 'groupTypeFlag',
      label: t('settings.common.entityType'),
      kind: 'select',
      options: ENTITY_TYPE_OPTIONS,
      required: true,
    },
    {
      prop: 'parentGroupId',
      label: t('settings.group.parentGroupId'),
      kind: 'treeSelect',
      tree: {
        load: loadGroupRecords,
        props: {label: 'groupName', children: 'children'},
        nodeKey: 'id',
        checkStrictly: true,
        transform: (rows: GroupRecord[], form: Record<string, any>) => {
          const excluded = computeExcluded(rows, String(form.id || ''));
          const filtered = rows.filter(
            (row) => row.groupTypeFlag === form.groupTypeFlag && !excluded.has(String(row.id))
          );
          return [{id: 0, groupName: t('settings.group.rootGroup'), children: buildTree(filtered)}];
        },
      },
    },
    {
      prop: 'groupName',
      label: t('settings.group.groupName'),
      placeholder: t('settings.group.groupNamePlaceholder'),
      maxlength: 32,
      rules: nameRules(t, t('common.entityGroup')),
    },
    {
      prop: 'groupCode',
      label: t('settings.group.groupCode'),
      placeholder: t('settings.group.groupCodePlaceholder'),
      maxlength: 32,
    },
    {prop: 'groupIndex', label: t('settings.group.groupIndex'), kind: 'number'},
    {prop: 'enableFlag', label: t('common.enableFlag'), kind: 'enableFlag'},
    {prop: 'remark', label: t('common.remark'), kind: 'textarea', span: 24, maxlength: 300, rules: remarkRules(t)},
  ],
  defaultForm: () => ({
    parentGroupId: null,
    groupTypeFlag: 'DEVICE',
    groupName: '',
    groupCode: '',
    groupIndex: 0,
    enableFlag: 'ENABLE',
    remark: '',
  }),
  list: listGroup,
  add: (payload) => addGroup(normalizeGroupPayload(payload) as Parameters<typeof addGroup>[0]),
  update: (payload) => updateGroup(normalizeGroupPayload(payload) as Parameters<typeof updateGroup>[0]),
  remove: deleteGroup,
  detail: {routeName: 'settingsGroupDetail'},
  confirmDeleteText: t('settings.group.confirmDelete'),
  emptyText: t('settings.group.empty'),
});
