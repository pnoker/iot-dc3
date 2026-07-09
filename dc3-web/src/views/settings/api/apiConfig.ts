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
import type {EntityListConfig} from '@/config/types/entityList';

export const createApiConfig = (t: ComposerTranslation): EntityListConfig => ({
  name: 'api',
  title: t('nav.settingsApi'),
  editable: false,
  searchFields: [
    {
      prop: 'apiName',
      label: t('settings.api.apiName'),
      kind: 'input',
      placeholder: t('settings.api.apiNamePlaceholder'),
    },
    {
      prop: 'apiCode',
      label: t('settings.api.apiCode'),
      kind: 'input',
      placeholder: t('settings.api.apiCodePlaceholder'),
    },
    {
      prop: 'apiGroup',
      label: t('settings.api.apiGroup'),
      kind: 'input',
      placeholder: t('settings.api.apiGroupPlaceholder'),
    },
    {
      prop: 'serviceName',
      label: t('settings.api.serviceName'),
      kind: 'input',
      placeholder: t('settings.api.serviceNamePlaceholder'),
    },
    {
      prop: 'apiTypeFlag',
      label: t('settings.api.apiType'),
      kind: 'select',
      options: [
        {label: 'GET', value: 'GET'},
        {label: 'POST', value: 'POST'},
        {label: 'PUT', value: 'PUT'},
        {label: 'DELETE', value: 'DELETE'},
      ],
    },
    {prop: 'enableFlag', label: t('common.enableFlag'), kind: 'enableFlag', includeAll: true},
  ],
  columns: [
    {prop: 'apiName', label: t('settings.api.apiName'), minWidth: 160},
    {prop: 'apiCode', label: t('settings.api.apiCode'), kind: 'code', minWidth: 200},
    {prop: 'apiGroup', label: t('settings.api.apiGroup'), minWidth: 160},
    {prop: 'serviceName', label: t('settings.api.serviceName'), minWidth: 160},
    {prop: 'apiTypeFlag', label: t('settings.api.apiType'), minWidth: 100},
    {prop: 'enableFlag', label: t('common.enable'), kind: 'enable', width: 90},
    {prop: 'remark', label: t('common.remark'), minWidth: 140},
    {prop: 'createTime', label: t('common.createTime'), kind: 'time', width: 180},
    {prop: 'operateTime', label: t('common.operationTime'), kind: 'time', width: 180},
  ],
  fields: [],
  defaultForm: () => ({}),
  list: listApi,
  detail: {routeName: 'settingsApiDetail'},
  emptyText: t('settings.api.empty'),
});
