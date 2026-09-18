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

import {flushPromises, mount} from '@vue/test-utils';
import {createPinia, setActivePinia} from 'pinia';
import {defineComponent, h, ref} from 'vue';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import RegisterClientDialog from '@/views/settings/mcp/components/RegisterClientDialog.vue';
import i18n from '@/config/i18n';

import {createElButtonStub, createElFormStub, layoutStubs} from '../setup/stubs/element-plus';

const mcpMocks = vi.hoisted(() => ({
  registerMcpClient: vi.fn(),
  listServiceAccount: vi.fn(),
  setCopyContent: vi.fn(),
  failMessage: vi.fn(),
  successMessage: vi.fn(),
}));

vi.mock('@/api/mcp', () => ({registerMcpClient: mcpMocks.registerMcpClient}));
vi.mock('@/api/serviceAccount', () => ({listServiceAccount: mcpMocks.listServiceAccount}));
vi.mock('@/utils/clipboardUtil', () => ({setCopyContent: mcpMocks.setCopyContent}));
vi.mock('@/utils/notificationUtil', () => ({
  failMessage: mcpMocks.failMessage,
  successMessage: mcpMocks.successMessage,
}));

const Host = defineComponent({
  name: 'RegisterClientDialogHost',
  components: {RegisterClientDialog},
  setup() {
    const dialog = ref<{open: () => void}>();
    const openDialog = () => dialog.value?.open();
    return {dialog, openDialog};
  },
  render() {
    return h('div', [
      h('button', {class: 'open-dialog', type: 'button', onClick: this.openDialog}, 'Open'),
      h(RegisterClientDialog, {ref: 'dialog'}),
    ]);
  },
});

const mountDialog = () => {
  const {ElForm} = createElFormStub();
  return mount(Host, {
    global: {
      plugins: [i18n],
      directives: {loading: () => undefined},
      stubs: {
        ...layoutStubs,
        ElButton: createElButtonStub(),
        ElForm,
      },
    },
  });
};

describe('RegisterClientDialog', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    mcpMocks.registerMcpClient.mockReset();
    mcpMocks.listServiceAccount.mockReset();
    mcpMocks.setCopyContent.mockReset();
    mcpMocks.failMessage.mockReset();
    mcpMocks.successMessage.mockReset();
    mcpMocks.listServiceAccount.mockResolvedValue({items: []});
    mcpMocks.setCopyContent.mockResolvedValue(true);
  });

  it('shows a one-time secret result with copy actions after registration', async () => {
    mcpMocks.registerMcpClient.mockResolvedValue({client_id: 'client-1', client_secret: 'secret-1'});
    const wrapper = mountDialog();

    await wrapper.find('.open-dialog').trigger('click');
    await flushPromises();
    await wrapper.get('button.el-button-stub.is-primary').trigger('click');
    await flushPromises();

    expect(wrapper.text()).toContain('This secret is shown only once');
    expect(wrapper.findAll('input').map((input) => input.element.getAttribute('value'))).toContain('secret-1');
    expect(wrapper.findAll('button.el-button-stub').filter((button) => button.text() === 'Save')).toHaveLength(0);
    expect(wrapper.findAll('button.el-button-stub').filter((button) => button.text() === 'Copy')).toHaveLength(2);

    const copyButtons = wrapper.findAll('button.el-button-stub').filter((button) => button.text() === 'Copy');
    await copyButtons[1].trigger('click');
    expect(mcpMocks.setCopyContent).toHaveBeenCalledWith('secret-1', true, 'Client Secret');
  });
});
