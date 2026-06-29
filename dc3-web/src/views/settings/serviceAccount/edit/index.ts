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

import {defineComponent, reactive, ref, watch} from 'vue';
import type {FormInstance, FormRules} from 'element-plus';
import {useI18n} from 'vue-i18n';

import {listPrincipalByIds} from '@/api/principal';
import EnableFlagSegmented from '@/components/segmented/EnableFlagSegmented.vue';
import {NAME_PATTERN} from '@/utils/formRuleUtil';

type FormMode = 'add' | 'edit';

const createEmptyForm = (ownerPrincipalId = '') => ({
  id: '' as string,
  serviceAccountName: '',
  ownerPrincipalId,
  purpose: '',
  expireTime: '',
  enableFlag: 'ENABLE',
});

const normalizeEnableFlag = (value: unknown) => {
  if (value === 1 || value === '1' || value === 'DISABLE') return 'DISABLE';
  return 'ENABLE';
};

const submitPayload = (form: ReturnType<typeof createEmptyForm>) => {
  const payload: Record<string, unknown> = {...form};
  if (!payload.id) delete payload.id;
  if (!payload.purpose) delete payload.purpose;
  if (!payload.expireTime) delete payload.expireTime;
  return payload;
};

export default defineComponent({
  name: 'ServiceAccountEditForm',
  components: {EnableFlagSegmented},
  emits: ['add-thing', 'update-thing'],
  setup(_, {emit}) {
    const {t} = useI18n();

    const formRef = ref<FormInstance>();

    const reactiveData = reactive({
      visible: false,
      mode: 'add' as FormMode,
      submitting: false,
      form: createEmptyForm(),
      originalForm: createEmptyForm(),
    });

    // Owner is fixed (the acting user); show its display name instead of the raw id.
    const ownerName = ref('');
    watch(
      () => reactiveData.form.ownerPrincipalId,
      async (id) => {
        const key = String(id ?? '');
        if (!key || key === '0') {
          ownerName.value = '';
          return;
        }
        try {
          const res: any = await listPrincipalByIds([key]);
          const p = (res?.data || [])[0];
          ownerName.value = p ? p.displayName || p.principalName || key : key;
        } catch {
          ownerName.value = key;
        }
      },
      {immediate: true}
    );

    const rules: FormRules = {
      serviceAccountName: [
        {
          required: true,
          whitespace: true,
          message: t('settings.serviceAccount.serviceAccountNamePlaceholder'),
          trigger: 'blur',
        },
        {min: 2, max: 32, message: t('common.nameLength'), trigger: 'blur'},
        {pattern: NAME_PATTERN, message: t('common.nameFormat'), trigger: 'blur'},
      ],
    };

    const reset = () => {
      reactiveData.form =
        reactiveData.mode === 'edit'
          ? {...reactiveData.originalForm}
          : createEmptyForm(reactiveData.form.ownerPrincipalId);
      reactiveData.submitting = false;
      formRef.value?.clearValidate();
    };

    const show = (ownerPrincipalId = '') => {
      reactiveData.mode = 'add';
      reactiveData.originalForm = createEmptyForm(ownerPrincipalId);
      reactiveData.form = createEmptyForm(ownerPrincipalId);
      reactiveData.visible = true;
    };

    const showEdit = (row: any) => {
      reactiveData.mode = 'edit';
      const initial = {
        ...createEmptyForm(),
        ...row,
        enableFlag: normalizeEnableFlag(row.enableFlag),
      };
      reactiveData.originalForm = {...initial};
      reactiveData.form = {...initial};
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
        emit('add-thing', submitPayload(reactiveData.form), done);
      } else {
        emit('update-thing', submitPayload(reactiveData.form), done);
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
      ownerName,
    };
  },
});
