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

import {addMenu, deleteMenu, listMenuTree, updateMenu} from '@/api/menu';
import {MENU_LEVEL_OPTIONS, MENU_TYPE_OPTIONS} from '@/config/constant/enums';
import {iconNames} from '@/config/constant/icons';
import type {EntityListConfig} from '@/config/types/entityList';
import {authNameRules, remarkRules} from '@/utils/formRuleUtil';

export const createMenuConfig = (t: ComposerTranslation): EntityListConfig => ({
  name: 'menu',
  title: t('nav.settingsMenu'),
  mode: 'tree',
  rowKey: 'id',
  defaultExpandAll: true,
  editable: true,
  searchFields: [
    {
      prop: 'menuName',
      label: t('settings.menu.menuName'),
      kind: 'input',
      placeholder: t('settings.menu.menuNamePlaceholder'),
    },
    {
      prop: 'menuCode',
      label: t('settings.menu.menuCode'),
      kind: 'input',
      placeholder: t('settings.menu.menuCodePlaceholder'),
    },
    {
      prop: 'menuTypeFlag',
      label: t('settings.menu.menuType'),
      kind: 'select',
      options: MENU_TYPE_OPTIONS,
      placeholder: t('common.all'),
    },
    {prop: 'enableFlag', label: t('common.enableFlag'), kind: 'enableFlag', includeAll: true},
  ],
  columns: [
    {prop: 'menuName', label: t('settings.menu.menuName'), minWidth: 220},
    {prop: 'menuCode', label: t('settings.menu.menuCode'), kind: 'code', minWidth: 180},
    {prop: 'menuTypeFlag', label: t('settings.menu.menuType'), minWidth: 100},
    {prop: 'menuLevel', label: t('settings.menu.menuLevel'), minWidth: 90},
    {prop: 'menuIndex', label: t('settings.menu.menuIndex'), minWidth: 80},
    {prop: 'menuExt.content.url', label: t('settings.menu.menuUrl'), minWidth: 160},
    {prop: 'menuExt.content.icon', label: t('settings.menu.menuIcon'), kind: 'icon', width: 90},
    {prop: 'enableFlag', label: t('common.enable'), kind: 'enable', width: 90},
    {prop: 'createTime', label: t('common.createTime'), kind: 'time', width: 165},
  ],
  fields: [
    {
      prop: 'parentMenuId',
      label: t('settings.menu.parentMenuId'),
      kind: 'treeSelect',
      rules: [{required: true, message: t('settings.menu.parentMenuIdPlaceholder'), trigger: 'change'}],
      tree: {
        load: () => listMenuTree().then((res) => res.data || []),
        transform: (rows) => [{id: 0, menuName: 'Root', children: rows}],
        props: {label: 'menuName', children: 'children'},
        nodeKey: 'id',
      },
    },
    {
      prop: 'menuName',
      label: t('settings.menu.menuName'),
      placeholder: t('settings.menu.menuNamePlaceholder'),
      maxlength: 32,
      rules: authNameRules(t, t('common.entityMenu')),
    },
    {
      prop: 'menuCode',
      label: t('settings.menu.menuCode'),
      placeholder: t('settings.menu.menuCodePlaceholder'),
      maxlength: 64,
      rules: [{required: true, whitespace: true, message: t('settings.menu.menuCodePlaceholder'), trigger: 'blur'}],
    },
    {
      prop: 'titleZh',
      label: t('settings.menu.titleZh'),
      placeholder: t('settings.menu.titleZhPlaceholder'),
      maxlength: 64,
      rules: [{required: true, whitespace: true, message: t('settings.menu.titleZhPlaceholder'), trigger: 'blur'}],
    },
    {
      prop: 'titleEn',
      label: t('settings.menu.titleEn'),
      placeholder: t('settings.menu.titleEnPlaceholder'),
      maxlength: 64,
      rules: [{required: true, whitespace: true, message: t('settings.menu.titleEnPlaceholder'), trigger: 'blur'}],
    },
    {
      prop: 'menuTypeFlag',
      label: t('settings.menu.menuType'),
      kind: 'select',
      options: MENU_TYPE_OPTIONS,
      rules: [{required: true, message: t('settings.menu.menuTypeRequired'), trigger: 'change'}],
    },
    {
      prop: 'menuLevel',
      label: t('settings.menu.menuLevel'),
      kind: 'select',
      options: MENU_LEVEL_OPTIONS,
      rules: [{required: true, message: t('settings.menu.menuLevelRequired'), trigger: 'change'}],
    },
    {prop: 'menuIndex', label: t('settings.menu.menuIndex'), kind: 'number'},
    {
      prop: 'icon',
      label: t('settings.menu.menuIcon'),
      kind: 'select',
      placeholder: t('settings.menu.menuIconPlaceholder'),
      options: iconNames.map((name) => ({label: name, value: name})),
    },
    {
      prop: 'url',
      label: t('settings.menu.menuUrl'),
      placeholder: t('settings.menu.menuUrlPlaceholder'),
      maxlength: 256,
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
    parentMenuId: 0,
    menuName: '',
    menuCode: '',
    menuTypeFlag: 'COMMON',
    menuLevel: 'C1',
    menuIndex: 0,
    titleZh: '',
    titleEn: '',
    icon: '',
    url: '',
    enableFlag: 'ENABLE',
    remark: '',
  }),
  fromRow: (row) => {
    const content = row?.menuExt?.content || {};
    const titles = content.titles || {};
    const legacyTitle =
      typeof content.title === 'string' && content.title ? t(content.title, content.title) : row?.menuName || '';
    return {
      parentMenuId: row?.parentMenuId ?? 0,
      menuIndex: row?.menuIndex ?? 0,
      titleZh: titles.zh || legacyTitle,
      titleEn: titles.en || legacyTitle,
      icon: content.icon || '',
      url: content.url || '',
    };
  },
  toPayload: (form) => {
    const {icon, url, titleZh, titleEn, ...rest} = form;
    return {
      ...rest,
      menuExt: {
        content: {
          titles: {zh: titleZh, en: titleEn},
          icon: icon || '',
          url: url || '',
          remark: rest.remark || '',
        },
      },
    };
  },
  list: listMenuTree,
  add: addMenu as EntityListConfig['add'],
  update: updateMenu as EntityListConfig['update'],
  remove: deleteMenu,
  detail: {routeName: 'settingsMenuDetail'},
  confirmDeleteText: t('settings.menu.confirmDelete'),
  emptyText: t('settings.menu.empty'),
});
