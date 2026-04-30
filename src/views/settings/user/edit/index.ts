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
  emits: ['add-thing', 'update-thing'],
  setup(_, { emit }) {
    const { t } = useI18n();

    const formRef = ref<FormInstance>();

    const reactiveData = reactive({
      visible: false,
      mode: 'add' as FormMode,
      submitting: false,
      form: createEmptyForm(),
    });

    const rules: FormRules = {
      userName: [{ required: true, message: t('settings.user.userNamePlaceholder'), trigger: 'blur' }],
      nickName: [{ required: true, message: t('settings.user.nickNamePlaceholder'), trigger: 'blur' }],
    };

    const reset = () => {
      reactiveData.form = createEmptyForm();
      reactiveData.submitting = false;
      formRef.value?.clearValidate();
    };

    const show = () => {
      reactiveData.mode = 'add';
      reactiveData.form = createEmptyForm();
      reactiveData.visible = true;
    };

    const showEdit = (row: any) => {
      reactiveData.mode = 'edit';
      reactiveData.form = {
        ...createEmptyForm(),
        ...row,
        enableFlag: Number(row.enableFlag ?? 0),
      };
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
