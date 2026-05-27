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

import { defineComponent, reactive, ref } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { useI18n } from 'vue-i18n';

import EnableFlagSegmented from '@/components/segmented/EnableFlagSegmented.vue';
import { ENTITY_TYPE_OPTIONS } from '@/config/constant/enums';
import type { LabelRecord } from '@/config/types/manager';
import { nameRules, remarkRules } from '@/utils/formRuleUtil';

type FormMode = 'add' | 'edit';

const createEmptyForm = () => ({
  id: '' as string,
  entityTypeFlag: 'DEVICE' as string,
  labelName: '',
  labelCode: '',
  labelColor: '#F4F4F5',
  enableFlag: 'ENABLE' as string,
  remark: '',
});

export default defineComponent({
  name: 'LabelEditForm',
  components: { EnableFlagSegmented },
  emits: ['add-thing', 'update-thing'],
  setup(_, { emit }) {
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
      entityTypeFlag: [{ required: true, message: t('settings.common.entityTypePlaceholder'), trigger: 'change' }],
      labelName: nameRules(t, t('common.entityLabel')),
      remark: remarkRules(t),
    };

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

    const showEdit = (row: LabelRecord) => {
      reactiveData.mode = 'edit';
      const initial = {
        ...createEmptyForm(),
        ...row,
        labelColor: row?.labelColor || '#F4F4F5',
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
      ENTITY_TYPE_OPTIONS,
      reset,
      show,
      showEdit,
      submit,
    };
  },
});
