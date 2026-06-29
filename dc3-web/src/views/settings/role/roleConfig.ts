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

import {addRole, deleteRole, listRole, listRoleTree, updateRole} from '@/api/role';
import type {EntityListConfig} from '@/config/types/entityList';
import {authNameRules, remarkRules} from '@/utils/formRuleUtil';

interface RoleHandlers {
  onAssignResources: (row: Record<string, any>) => void;
}

const normalizeRolePayload = (p: Record<string, unknown>) => {
  const next = {...p};
  if (!next.parentRoleId) next.parentRoleId = 0;
  return next;
};

export const createRoleConfig = (t: ComposerTranslation, handlers: RoleHandlers): EntityListConfig => ({
  name: 'role',
  title: t('nav.settingsRole'),
  editable: true,
  searchFields: [
    {
      prop: 'roleName',
      label: t('settings.role.roleName'),
      kind: 'input',
      placeholder: t('settings.role.roleNamePlaceholder'),
    },
    {
      prop: 'roleCode',
      label: t('settings.role.roleCode'),
      kind: 'input',
      placeholder: t('settings.role.roleCodePlaceholder'),
    },
    {prop: 'enableFlag', label: t('common.enableFlag'), kind: 'enableFlag', includeAll: true},
  ],
  columns: [
    {prop: 'roleName', label: t('settings.role.roleName'), minWidth: 160},
    {prop: 'roleCode', label: t('settings.role.roleCode'), minWidth: 160},
    {prop: 'enableFlag', label: t('common.enable'), kind: 'enable', width: 90},
    {prop: 'remark', label: t('common.remark'), minWidth: 200},
    {prop: 'createTime', label: t('common.createTime'), kind: 'time', width: 165},
  ],
  fields: [
    {
      prop: 'parentRoleId',
      label: t('settings.role.parentRoleId'),
      kind: 'treeSelect',
      rules: [{required: true, message: t('settings.role.parentRoleIdPlaceholder'), trigger: 'change'}],
      tree: {
        load: () => listRoleTree().then((res: any) => (res.data as any[]) || []),
        transform: (rows: any[]) => [{id: 0, roleName: t('settings.role.rootRole'), children: rows}],
        props: {label: 'roleName', children: 'children'},
        nodeKey: 'id',
        checkStrictly: true,
      },
    },
    {
      prop: 'roleName',
      label: t('settings.role.roleName'),
      placeholder: t('settings.role.roleNamePlaceholder'),
      maxlength: 32,
      rules: authNameRules(t, t('common.entityRole')),
    },
    {
      prop: 'roleCode',
      label: t('settings.role.roleCode'),
      placeholder: t('settings.role.roleCodePlaceholder'),
      maxlength: 32,
    },
    {prop: 'enableFlag', label: t('common.enableFlag'), kind: 'enableFlag'},
    {prop: 'remark', label: t('common.remark'), kind: 'textarea', span: 24, maxlength: 300, rules: remarkRules(t)},
  ],
  defaultForm: () => ({
    parentRoleId: 0,
    roleName: '',
    roleCode: '',
    enableFlag: 'ENABLE',
    remark: '',
  }),
  list: listRole,
  add: (p) => addRole(normalizeRolePayload(p) as Parameters<typeof addRole>[0]),
  update: (p) => updateRole(normalizeRolePayload(p) as Parameters<typeof updateRole>[0]),
  remove: deleteRole,
  detail: {routeName: 'settingsRoleDetail'},
  extraActions: [
    {
      key: 'assignResources',
      label: t('settings.role.assignResources'),
      type: 'primary',
      onClick: handlers.onAssignResources,
    },
  ],
  confirmDeleteText: t('settings.role.confirmDelete'),
  emptyText: t('settings.role.empty'),
});
