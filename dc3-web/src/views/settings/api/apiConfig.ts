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
