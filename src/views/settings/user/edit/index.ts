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
import { AUTH_NAME_PATTERN, EMAIL_PATTERN, NAME_PATTERN, PHONE_PATTERN } from '@/utils/formRuleUtil';

type FormMode = 'add' | 'edit';

const createEmptyForm = () => ({
  id: '' as string,
  userName: '',
  nickName: '',
  phone: '',
  email: '',
  enableFlag: 0,
});

export default defineComponent({
  name: 'UserEditForm',
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
      userName: [
        { required: true, whitespace: true, message: t('settings.user.userNamePlaceholder'), trigger: 'blur' },
        { min: 2, max: 32, message: t('common.authNameLength'), trigger: 'blur' },
        { pattern: AUTH_NAME_PATTERN, message: t('common.authNameFormat'), trigger: 'blur' },
      ],
      nickName: [
        { required: true, whitespace: true, message: t('settings.user.nickNamePlaceholder'), trigger: 'blur' },
        { min: 2, max: 32, message: t('common.nameLength'), trigger: 'blur' },
        { pattern: NAME_PATTERN, message: t('common.nameFormat'), trigger: 'blur' },
      ],
      phone: [{ pattern: PHONE_PATTERN, message: t('settings.user.phoneFormat'), trigger: 'blur' }],
      email: [{ pattern: EMAIL_PATTERN, message: t('settings.user.emailFormat'), trigger: 'blur' }],
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

    const showEdit = (row: any) => {
      reactiveData.mode = 'edit';
      const initial = {
        ...createEmptyForm(),
        ...row,
        enableFlag: Number(row.enableFlag ?? 0),
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
      if (reactiveData.mode === 'add') {
        emit('add-thing', { ...reactiveData.form }, done);
      } else {
        emit('update-thing', { ...reactiveData.form }, done);
      }
    };

    return {
      t,
      formRef,
      reactiveData,
      rules,
      reset,
      show,
      showEdit,
      submit,
    };
  },
});
