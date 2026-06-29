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

import {addLabel, deleteLabel, listLabel, updateLabel} from '@/api/label';
import {ENTITY_TYPE_OPTIONS} from '@/config/constant/enums';
import type {EntityListConfig} from '@/config/types/entityList';
import {nameRules, remarkRules} from '@/utils/formRuleUtil';

export const createLabelConfig = (t: ComposerTranslation): EntityListConfig => ({
  name: 'label',
  title: t('nav.settingsLabel'),
  editable: true,
  searchFields: [
    {
      prop: 'labelName',
      label: t('settings.label.labelName'),
      kind: 'input',
      placeholder: t('settings.label.labelNamePlaceholder'),
    },
    {prop: 'entityTypeFlag', label: t('settings.common.entityType'), kind: 'select', options: ENTITY_TYPE_OPTIONS},
    {prop: 'enableFlag', label: t('common.enableFlag'), kind: 'enableFlag', includeAll: true},
  ],
  columns: [
    {prop: 'labelName', label: t('settings.label.labelName'), minWidth: 160},
    {prop: 'labelCode', label: t('settings.label.labelCode'), kind: 'code', minWidth: 150},
    {prop: 'entityTypeFlag', label: t('settings.common.entityType'), width: 110},
    {prop: 'labelColor', label: t('settings.label.labelColor'), kind: 'color', width: 130},
    {prop: 'enableFlag', label: t('common.enable'), kind: 'enable', width: 90},
    {prop: 'remark', label: t('common.remark'), minWidth: 180},
    {prop: 'createTime', label: t('common.createTime'), kind: 'time', width: 165},
  ],
  fields: [
    {
      prop: 'entityTypeFlag',
      label: t('settings.common.entityType'),
      kind: 'select',
      options: ENTITY_TYPE_OPTIONS,
      required: true,
    },
    {prop: 'labelColor', label: t('settings.label.labelColor'), kind: 'color'},
    {
      prop: 'labelName',
      label: t('settings.label.labelName'),
      placeholder: t('settings.label.labelNamePlaceholder'),
      maxlength: 32,
      rules: nameRules(t, t('common.entityLabel')),
    },
    {
      prop: 'labelCode',
      label: t('settings.label.labelCode'),
      placeholder: t('settings.label.labelCodePlaceholder'),
      maxlength: 32,
    },
    {prop: 'enableFlag', label: t('common.enableFlag'), kind: 'enableFlag'},
    {prop: 'remark', label: t('common.remark'), kind: 'textarea', span: 24, maxlength: 300, rules: remarkRules(t)},
  ],
  defaultForm: () => ({
    entityTypeFlag: 'DEVICE',
    labelColor: '#F4F4F5',
    labelName: '',
    labelCode: '',
    enableFlag: 'ENABLE',
    remark: '',
  }),
  list: listLabel,
  add: addLabel,
  update: updateLabel,
  remove: deleteLabel,
  detail: {routeName: 'settingsLabelDetail'},
  confirmDeleteText: t('settings.label.confirmDelete'),
  emptyText: t('settings.label.empty'),
});
