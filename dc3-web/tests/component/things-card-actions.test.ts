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

import {mount} from '@vue/test-utils';
import {describe, expect, it} from 'vitest';

import i18n from '@/config/i18n';
import ThingsCardActions from '@/components/card/actions/ThingsCardActions.vue';

import {createElButtonStub, layoutStubs} from '../setup/stubs/element-plus'; // Popconfirm stub that surfaces the `confirm` event so we can simulate

// Popconfirm stub that surfaces the `confirm` event so we can simulate
// a user accepting the dialog. The real ElPopconfirm renders the
// reference inline; we add a button after it that fires `confirm` on click.
const ElPopconfirmStub = {
  name: 'ElPopconfirm',
  emits: ['confirm'],
  props: ['title', 'icon', 'iconColor', 'placement'],
  template: `
    <span class="el-popconfirm-stub" :data-title="title">
      <slot name="reference"/>
      <button type="button" class="popconfirm-confirm" @click="$emit('confirm')"/>
    </span>
  `,
};

function mountActions(props: Record<string, unknown> = {}) {
  return mount(ThingsCardActions, {
    props: {
      enabled: true,
      disableTitle: 'Disable this device?',
      enableTitle: 'Enable this device?',
      deleteTitle: 'Delete this device?',
      ...props,
    },
    global: {
      plugins: [i18n],
      stubs: {
        ...layoutStubs,
        ElPopconfirm: ElPopconfirmStub,
        ElButton: createElButtonStub(),
      },
    },
  });
}

describe('ThingsCardActions', () => {
  it('emits disable / enable / delete when their popconfirms confirm', async () => {
    const wrapper = mountActions();
    const confirmButtons = wrapper.findAll('.popconfirm-confirm');

    // Order matches the template: disable → enable → delete.
    expect(confirmButtons).toHaveLength(3);
    await confirmButtons[0].trigger('click');
    await confirmButtons[1].trigger('click');
    await confirmButtons[2].trigger('click');

    expect(wrapper.emitted('disable')).toHaveLength(1);
    expect(wrapper.emitted('enable')).toHaveLength(1);
    expect(wrapper.emitted('delete')).toHaveLength(1);
  });

  it('emits edit and detail directly without confirmation', async () => {
    const wrapper = mountActions();
    const directButtons = wrapper.findAll('button.el-button-stub').filter((btn) => !btn.attributes('disabled'));

    // The last two non-popconfirm buttons in the cluster: Edit, then Detail.
    const edit = directButtons[directButtons.length - 2];
    const detail = directButtons[directButtons.length - 1];

    await edit.trigger('click');
    await detail.trigger('click');

    expect(wrapper.emitted('edit')).toHaveLength(1);
    expect(wrapper.emitted('detail')).toHaveLength(1);
  });

  it('disables the detail button when detailDisabled is true', () => {
    const wrapper = mountActions({detailDisabled: true});
    const buttons = wrapper.findAll('button.el-button-stub');
    const detail = buttons[buttons.length - 1];
    expect(detail.attributes('disabled')).toBeDefined();
  });

  it('disables disable when already disabled, and enable when already enabled', () => {
    const enabled = mountActions({enabled: true});
    const enabledButtons = enabled.findAll('button.el-button-stub');
    // Buttons in template order: disable(0), enable(1), delete(2), edit(3), detail(4)
    expect(enabledButtons[0].attributes('disabled')).toBeUndefined();
    expect(enabledButtons[1].attributes('disabled')).toBeDefined();

    const disabled = mountActions({enabled: false});
    const disabledButtons = disabled.findAll('button.el-button-stub');
    expect(disabledButtons[0].attributes('disabled')).toBeDefined();
    expect(disabledButtons[1].attributes('disabled')).toBeUndefined();
  });
});
