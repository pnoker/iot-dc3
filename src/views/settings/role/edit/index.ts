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

import type { PropType } from 'vue';
import { computed, defineComponent, reactive, ref } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { useI18n } from 'vue-i18n';

import EnableFlagSegmented from '@/components/segmented/EnableFlagSegmented.vue';
import { authNameRules, remarkRules } from '@/utils/formRuleUtil';

type FormMode = 'add' | 'edit';

const createEmptyForm = () => ({
  id: '' as string,
  parentRoleId: 0 as number | string,
  roleName: '',
  roleCode: '',
  enableFlag: 'ENABLE' as string,
  remark: '',
});

export default defineComponent({
  name: 'RoleEditForm',
  components: { EnableFlagSegmented },
  props: {
    treeData: {
      type: Array as PropType<any[]>,
      default: () => [],
    },
  },
  emits: ['add-thing', 'update-thing'],
  setup(props, { emit }) {
    const { t } = useI18n();

    const formRef = ref<FormInstance>();

    const reactiveData = reactive({
      visible: false,
      mode: 'add' as FormMode,
      submitting: false,
      form: createEmptyForm(),
      originalForm: createEmptyForm(),
    });

    const rules: FormRules = {
      roleName: authNameRules(t, t('common.entityRole')),
      parentRoleId: [{ required: true, message: t('settings.role.parentRoleIdPlaceholder'), trigger: 'change' }],
      remark: remarkRules(t),
    };

    // Synthesize a virtual "Root" row so top-level roles are reachable
    // through the same tree UI — same pattern as MenuEditForm.
    const parentTreeOptions = computed(() => [
      { id: 0, roleName: t('settings.role.rootRole'), children: props.treeData || [] },
    ]);

    const reset = () => {
      reactiveData.form = reactiveData.mode === 'edit' ? { ...reactiveData.originalForm } : createEmptyForm();
      reactiveData.submitting = false;
      formRef.value?.clearValidate();
    };

    const show = () => {
      reactiveData.mode = 'add';
      reactiveData.originalForm = createEmptyForm();
      reactiveData.form = createEmptyForm();
      reactiveData.visible = true;
    };

    const showEdit = (row: any) => {
      reactiveData.mode = 'edit';
      const initial = {
        ...createEmptyForm(),
        ...row,
        parentRoleId: row?.parentRoleId ?? 0,
      };
      reactiveData.originalForm = { ...initial };
      reactiveData.form = { ...initial };
      reactiveData.visible = true;
    };

    const done = () => {
      reactiveData.submitting = false;
      reactiveData.visible = false;
    };

    const submit = async () => {
      const valid = await formRef.value?.validate().catch(() => false);
      if (!valid) return;
      reactiveData.submitting = true;
      const payload = { ...reactiveData.form };
      if (!payload.parentRoleId) {
        payload.parentRoleId = 0;
      }
      if (reactiveData.mode === 'add') {
        emit('add-thing', payload, done);
      } else {
        emit('update-thing', payload, done);
      }
    };

    return {
      t,
      formRef,
      reactiveData,
      rules,
      parentTreeOptions,
      reset,
      show,
      showEdit,
      submit,
    };
  },
});
