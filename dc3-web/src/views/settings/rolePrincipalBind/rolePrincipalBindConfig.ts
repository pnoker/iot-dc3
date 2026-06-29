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

import {listRole} from '@/api/role';
import {addRolePrincipalBind, deleteRolePrincipalBind, listRolePrincipalBind} from '@/api/rolePrincipalBind';
import type {EntityListConfig} from '@/config/types/entityList';

import {principalIdField, principalNameRelation, roleNameRelation} from '../relations';

const PRINCIPAL_TYPE_OPTIONS = [
  {label: 'USER', value: 'USER'},
  {label: 'SERVICE_ACCOUNT', value: 'SERVICE_ACCOUNT'},
  {label: 'SYSTEM', value: 'SYSTEM'},
];

export const createRolePrincipalBindConfig = (t: ComposerTranslation): EntityListConfig => ({
  name: 'role-principal-bind',
  title: t('nav.settingsRolePrincipalBind'),
  editable: true,
  searchFields: [
    {prop: 'roleId', label: t('settings.rolePrincipalBind.roleId'), kind: 'input'},
    {prop: 'principalId', label: t('settings.rolePrincipalBind.principalId'), kind: 'input'},
  ],
  columns: [
    {
      prop: 'roleId',
      label: t('settings.rolePrincipalBind.roleId'),
      minWidth: 160,
      formatter: (row, ctx) => ctx.relations.roleName?.[String(row.roleId)] || String(row.roleId ?? '-'),
    },
    {
      prop: 'principalId',
      label: t('settings.rolePrincipalBind.principalId'),
      minWidth: 160,
      formatter: (row, ctx) => ctx.relations.principalName?.[String(row.principalId)] || String(row.principalId ?? '-'),
    },
    {prop: 'principalType', label: t('settings.rolePrincipalBind.principalType'), minWidth: 150},
    {prop: 'createTime', label: t('common.createTime'), kind: 'time', width: 165},
  ],
  fields: [
    {
      prop: 'roleId',
      label: t('settings.rolePrincipalBind.roleId'),
      kind: 'treeSelect',
      tree: {
        load: async () => {
          const res: any = await listRole({page: {current: 1, size: 1000}});
          return res?.data?.records || [];
        },
        props: {label: 'roleName', value: 'id'},
      },
      required: true,
    },
    principalIdField(t('settings.rolePrincipalBind.principalId')),
    {
      prop: 'principalType',
      label: t('settings.rolePrincipalBind.principalType'),
      kind: 'select',
      options: PRINCIPAL_TYPE_OPTIONS,
      required: true,
    },
  ],
  defaultForm: () => ({roleId: '', principalId: '', principalType: 'USER'}),
  relations: [roleNameRelation(), principalNameRelation()],
  list: listRolePrincipalBind,
  add: addRolePrincipalBind,
  remove: deleteRolePrincipalBind,
  confirmDeleteText: t('settings.rolePrincipalBind.confirmDelete'),
  emptyText: t('settings.rolePrincipalBind.empty'),
});
