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

import {type ComponentOptions, defineComponent, h} from 'vue';
import {vi} from 'vitest';

/**
 * Shared Element Plus stub library for component tests.
 *
 * Centralised so each component test no longer duplicates 30+ lines of
 * inline-template stubs. The stubs intentionally only model the subset
 * of behaviour that the component contracts care about (event names,
 * exposed methods, a couple of props). They are NOT visual fidelity
 * stand-ins.
 */

export const createElButtonStub = () =>
  defineComponent({
    name: 'ElButton',
    props: ['icon', 'plain', 'type', 'circle', 'disabled'],
    emits: ['click'],
    setup(props, {emit, slots}) {
      return () =>
        h(
          'button',
          {
            type: 'button',
            class: ['el-button-stub', props.circle ? 'is-circle' : '', props.type ? `is-${props.type}` : ''],
            'data-icon': typeof props.icon === 'object' && props.icon ? (props.icon as {name?: string}).name : '',
            disabled: props.disabled,
            onClick: (event: MouseEvent) => emit('click', event),
          },
          slots.default?.()
        );
    },
  });

export const createElPaginationStub = () =>
  defineComponent({
    name: 'ElPagination',
    props: ['currentPage', 'pageSize', 'pageSizes', 'total'],
    emits: ['size-change', 'current-change'],
    setup(props, {emit}) {
      return () =>
        h('div', {class: 'el-pagination-stub'}, [
          h('span', `${props.currentPage}/${props.pageSize}/${props.total}`),
          h('button', {type: 'button', onClick: () => emit('size-change', 24)}, 'size'),
          h('button', {type: 'button', onClick: () => emit('current-change', 3)}, 'page'),
        ]);
    },
  });

/**
 * ElForm stub with mockable validate/resetFields.
 * Returns the spies alongside the component so tests can assert on them.
 */
export const createElFormStub = () => {
  const validate = vi.fn(() => Promise.resolve());
  const resetFields = vi.fn();

  const ElForm = defineComponent({
    name: 'ElForm',
    props: ['model', 'rules', 'inline'],
    setup(_, {expose, slots}) {
      expose({validate, resetFields});
      return () => h('form', {class: 'el-form-stub'}, slots.default?.());
    },
  });

  return {ElForm, validate, resetFields};
};

const passthrough = (tag: string, className: string): ComponentOptions => ({
  template: `<${tag} class="${className}"><slot /></${tag}>`,
});

/**
 * Bag of layout/decorator stubs that have no behaviour worth modeling.
 * Use as `stubs: { ...layoutStubs }`.
 */
export const layoutStubs: Record<string, ComponentOptions> = {
  // ElCard exposes a named `header` slot in real Element Plus; the
  // passthrough form would silently drop it. Render both header and
  // default slots so cards using `<template #header>` (DashboardCard,
  // TitleCard) keep their header markup discoverable in tests.
  ElCard: {
    template:
      '<section class="el-card-stub"><div class="el-card__header-stub" v-if="$slots.header"><slot name="header" /></div><div class="el-card__body-stub"><slot /></div></section>',
  },
  ElFormItem: passthrough('div', 'el-form-item-stub'),
  ElTooltip: passthrough('span', 'el-tooltip-stub'),
  ElCol: passthrough('div', 'el-col-stub'),
  ElRow: passthrough('div', 'el-row-stub'),
  ElDescriptions: passthrough('section', 'el-descriptions-stub'),
  ElDescriptionsItem: passthrough('div', 'el-descriptions-item-stub'),
  ElDialog: {template: '<section class="el-dialog-stub"><slot /><slot name="footer" /></section>'},
  ElDrawer: passthrough('section', 'el-drawer-stub'),
  ElEmpty: {template: '<div class="el-empty-stub" />'},
  ElPopconfirm: {template: '<span class="el-popconfirm-stub"><slot name="reference" /></span>'},
  ElTable: passthrough('section', 'el-table-stub'),
  ElTableColumn: {template: '<span class="el-table-column-stub" />'},
  ElTag: passthrough('span', 'el-tag-stub'),
  ElInput: {template: '<input class="el-input-stub" />'},
  ElInputNumber: {template: '<input class="el-input-number-stub" />'},
  ElOption: {template: '<option class="el-option-stub" />'},
  ElSelect: {template: '<select class="el-select-stub"><slot /></select>'},
  ElSegmented: {template: '<div class="el-segmented-stub"><slot /></div>'},
  ElCheckbox: {template: '<input type="checkbox" class="el-checkbox-stub" />'},
  ElSwitch: {template: '<input type="checkbox" class="el-switch-stub" />'},
  ElRadioGroup: passthrough('div', 'el-radio-group-stub'),
  ElRadio: {template: '<input type="radio" class="el-radio-stub" />'},
  ElDivider: {template: '<hr class="el-divider-stub" />'},
  ElLink: passthrough('a', 'el-link-stub'),
  ElDatePicker: {template: '<input class="el-date-picker-stub" />'},
  ElTimePicker: {template: '<input class="el-time-picker-stub" />'},
  ElResult: {
    props: ['title', 'subTitle', 'icon'],
    template:
      '<section class="el-result-stub" :data-icon="icon" :data-title="title" :data-sub-title="subTitle"><slot /><slot name="extra" /></section>',
  },
  ElAlert: passthrough('div', 'el-alert-stub'),
  ElSteps: passthrough('div', 'el-steps-stub'),
  ElStep: passthrough('div', 'el-step-stub'),
  ElBadge: passthrough('span', 'el-badge-stub'),
  ElMenu: passthrough('nav', 'el-menu-stub'),
  ElMenuItem: passthrough('a', 'el-menu-item-stub'),
  ElSubMenu: passthrough('div', 'el-sub-menu-stub'),
  ElDropdown: passthrough('div', 'el-dropdown-stub'),
  ElDropdownMenu: passthrough('div', 'el-dropdown-menu-stub'),
  ElDropdownItem: passthrough('div', 'el-dropdown-item-stub'),
  ElIcon: passthrough('span', 'el-icon-stub'),
  ElAvatar: passthrough('span', 'el-avatar-stub'),
  ElScrollbar: passthrough('div', 'el-scrollbar-stub'),
  ElBreadcrumb: passthrough('nav', 'el-breadcrumb-stub'),
  ElBreadcrumbItem: passthrough('span', 'el-breadcrumb-item-stub'),
  ElContainer: passthrough('div', 'el-container-stub'),
  ElHeader: passthrough('header', 'el-header-stub'),
  ElAside: passthrough('aside', 'el-aside-stub'),
  ElMain: passthrough('main', 'el-main-stub'),
  ElLoading: passthrough('div', 'el-loading-stub'),
  ElTreeSelect: passthrough('div', 'el-tree-select-stub'),
  ElTree: passthrough('div', 'el-tree-stub'),
  ElColorPicker: {template: '<input class="el-color-picker-stub" />'},
  ElTabs: passthrough('div', 'el-tabs-stub'),
  ElTabPane: passthrough('div', 'el-tab-pane-stub'),
  ElCollapse: passthrough('div', 'el-collapse-stub'),
  ElCollapseItem: passthrough('div', 'el-collapse-item-stub'),
  ElCheckboxGroup: passthrough('div', 'el-checkbox-group-stub'),
  ElTransfer: passthrough('div', 'el-transfer-stub'),
  ElUpload: passthrough('div', 'el-upload-stub'),
  ElPopover: passthrough('div', 'el-popover-stub'),
  ElImage: {template: '<img class="el-image-stub" />'},
  ElProgress: passthrough('div', 'el-progress-stub'),
  ElSlider: {template: '<input type="range" class="el-slider-stub" />'},
  ElTimeline: passthrough('ul', 'el-timeline-stub'),
  ElTimelineItem: {template: '<li class="el-timeline-item-stub"><slot /></li>'},
};
