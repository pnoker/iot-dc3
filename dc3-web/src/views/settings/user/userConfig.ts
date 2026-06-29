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

import {addUser, deleteUser, listUser, updateUser} from '@/api/user';
import type {EntityListConfig} from '@/config/types/entityList';
import {AUTH_NAME_PATTERN, EMAIL_PATTERN, NAME_PATTERN, PHONE_PATTERN} from '@/utils/formRuleUtil';

interface UserHandlers {
  onAssignRoles: (row: Record<string, any>) => void;
}

export const createUserConfig = (t: ComposerTranslation, handlers: UserHandlers): EntityListConfig => ({
  name: 'user',
  title: t('nav.settingsUser'),
  editable: true,
  searchFields: [
    {
      prop: 'nickName',
      label: t('settings.user.nickName'),
      kind: 'input',
      placeholder: t('settings.user.nickNamePlaceholder'),
    },
    {
      prop: 'userName',
      label: t('settings.user.userName'),
      kind: 'input',
      placeholder: t('settings.user.userNamePlaceholder'),
    },
    {
      prop: 'phone',
      label: t('settings.user.phone'),
      kind: 'input',
      placeholder: t('settings.user.phonePlaceholder'),
    },
    {
      prop: 'email',
      label: t('settings.user.email'),
      kind: 'input',
      placeholder: t('settings.user.emailPlaceholder'),
    },
    {prop: 'enableFlag', label: t('common.enableFlag'), kind: 'enableFlag', includeAll: true},
  ],
  columns: [
    {prop: 'nickName', label: t('settings.user.nickName'), minWidth: 120},
    {prop: 'userName', label: t('settings.user.userName'), minWidth: 140},
    {prop: 'phone', label: t('settings.user.phone'), minWidth: 140},
    {prop: 'email', label: t('settings.user.email'), minWidth: 180},
    {prop: 'enableFlag', label: t('common.enable'), kind: 'enable', width: 90},
    {prop: 'createTime', label: t('common.createTime'), kind: 'time', width: 165},
  ],
  fields: [
    {
      prop: 'userName',
      label: t('settings.user.userName'),
      placeholder: t('settings.user.userNamePlaceholder'),
      maxlength: 32,
      disabledOnEdit: true,
      rules: [
        {required: true, whitespace: true, message: t('settings.user.userNamePlaceholder'), trigger: 'blur'},
        {min: 2, max: 32, message: t('common.authNameLength'), trigger: 'blur'},
        {pattern: AUTH_NAME_PATTERN, message: t('common.authNameFormat'), trigger: 'blur'},
      ],
    },
    {
      prop: 'nickName',
      label: t('settings.user.nickName'),
      placeholder: t('settings.user.nickNamePlaceholder'),
      maxlength: 32,
      rules: [
        {required: true, whitespace: true, message: t('settings.user.nickNamePlaceholder'), trigger: 'blur'},
        {min: 2, max: 32, message: t('common.nameLength'), trigger: 'blur'},
        {pattern: NAME_PATTERN, message: t('common.nameFormat'), trigger: 'blur'},
      ],
    },
    {
      prop: 'phone',
      label: t('settings.user.phone'),
      placeholder: t('settings.user.phonePlaceholder'),
      maxlength: 11,
      rules: [{pattern: PHONE_PATTERN, message: t('settings.user.phoneFormat'), trigger: 'blur'}],
    },
    {
      prop: 'email',
      label: t('settings.user.email'),
      placeholder: t('settings.user.emailPlaceholder'),
      maxlength: 128,
      rules: [{pattern: EMAIL_PATTERN, message: t('settings.user.emailFormat'), trigger: 'blur'}],
    },
    {prop: 'enableFlag', label: t('common.enableFlag'), kind: 'enableFlag'},
  ],
  defaultForm: () => ({
    userName: '',
    nickName: '',
    phone: '',
    email: '',
    enableFlag: 'ENABLE',
  }),
  toPayload: (form) => {
    const next = {...form};
    if (!next.phone) delete next.phone;
    if (!next.email) delete next.email;
    return next;
  },
  list: listUser,
  add: addUser,
  update: updateUser,
  remove: deleteUser,
  detail: {routeName: 'settingsUserDetail'},
  extraActions: [
    {
      key: 'assignRoles',
      label: t('settings.user.assignRoles'),
      type: 'warning',
      onClick: handlers.onAssignRoles,
    },
  ],
  confirmDeleteText: t('settings.user.confirmDelete'),
  emptyText: t('settings.user.empty'),
});
