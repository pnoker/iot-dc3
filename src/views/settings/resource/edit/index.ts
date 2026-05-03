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

import { computed, defineComponent, reactive, ref } from 'vue';
import type { PropType } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { useI18n } from 'vue-i18n';

type FormMode = 'add' | 'edit';

const createEmptyForm = () => ({
  id: '' as string,
  parentResourceId: 0 as number | string,
  resourceName: '',
  resourceCode: '',
  resourceTypeFlag: '' as string,
  entityId: '' as string | number,
  enableFlag: 'ENABLE' as string,
  remark: '',
});

export default defineComponent({
  name: 'ResourceEditForm',
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
      resourceName: [{ required: true, message: t('settings.resource.resourceNamePlaceholder'), trigger: 'blur' }],
      parentResourceId: [
        { required: true, message: t('settings.resource.parentResourceIdPlaceholder'), trigger: 'change' },
      ],
      entityId: [{ required: true, message: t('settings.resource.entityIdPlaceholder'), trigger: 'blur' }],
    };

    // Synthesize a virtual "Root" so top-level resources remain pickable —
    // same pattern as MenuEditForm / RoleEditForm.
    const parentTreeOptions = computed(() => [
      { id: 0, resourceName: t('settings.resource.rootResource'), children: props.treeData || [] },
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
        parentResourceId: row?.parentResourceId ?? 0,
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
      if (!payload.parentResourceId) {
        payload.parentResourceId = 0;
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
