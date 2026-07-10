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
