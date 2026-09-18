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

import {computed, defineComponent, onBeforeUnmount, reactive, ref, watch} from 'vue';
import type {FormInstance, FormRules} from 'element-plus';
import {ElMessageBox} from 'element-plus';
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
      saveError: false,
      form: createEmptyForm(),
      originalForm: createEmptyForm(),
    });

    // Owner is fixed (the acting user); show its display name instead of the raw id.
    const ownerName = ref('');
    let ownerRequestId = 0;
    watch(
      () => reactiveData.form.ownerPrincipalId,
      async (id) => {
        const requestId = ++ownerRequestId;
        const key = String(id ?? '');
        if (!key || key === '0') {
          if (requestId === ownerRequestId) ownerName.value = '';
          return;
        }
        try {
          const res: any = await listPrincipalByIds([key]);
          const p = (res || [])[0];
          if (requestId === ownerRequestId) ownerName.value = p ? p.displayName || p.principalName || key : key;
        } catch {
          if (requestId === ownerRequestId) ownerName.value = key;
        }
      },
      {immediate: true}
    );

    let formSession = 0;
    const formDirty = computed(
      () => reactiveData.visible && JSON.stringify(reactiveData.form) !== JSON.stringify(reactiveData.originalForm)
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
        {...reactiveData.originalForm};
      reactiveData.submitting = false;
      reactiveData.saveError = false;
      formRef.value?.clearValidate();
    };

    const show = (ownerPrincipalId = '') => {
      formSession += 1;
      reactiveData.mode = 'add';
      reactiveData.originalForm = createEmptyForm(ownerPrincipalId);
      reactiveData.form = createEmptyForm(ownerPrincipalId);
      reactiveData.submitting = false;
      reactiveData.saveError = false;
      reactiveData.visible = true;
    };

    const showEdit = (row: any) => {
      formSession += 1;
      reactiveData.mode = 'edit';
      const initial = {
        ...createEmptyForm(),
        ...row,
        enableFlag: normalizeEnableFlag(row.enableFlag),
      };
      reactiveData.originalForm = {...initial};
      reactiveData.form = {...initial};
      reactiveData.submitting = false;
      reactiveData.saveError = false;
      reactiveData.visible = true;
    };

    const done = (close = true, session = formSession) => {
      if (session !== formSession) return;
      reactiveData.submitting = false;
      if (close) {
        reactiveData.originalForm = {...reactiveData.form};
        reactiveData.visible = false;
      } else {
        reactiveData.saveError = true;
      }
    };

    const onClosed = () => {
      formSession += 1;
      reactiveData.submitting = false;
      formRef.value?.clearValidate();
    };

    onBeforeUnmount(() => {
      formSession += 1;
      ownerRequestId += 1;
    });

    const requestClose = async (doneCallback?: () => void) => {
      if (reactiveData.submitting) return;
      const session = formSession;
      if (!formDirty.value) {
        if (doneCallback) doneCallback();
        else reactiveData.visible = false;
        return;
      }
      try {
        await ElMessageBox.confirm(t('common.discardConfirm'), t('common.confirm'), {
          type: 'warning',
          confirmButtonText: t('common.confirm'),
          cancelButtonText: t('common.cancel'),
        });
        if (session !== formSession || !reactiveData.visible) return;
        if (doneCallback) doneCallback();
        else reactiveData.visible = false;
      } catch {
        // Keep the draft open when the user cancels the confirmation.
      }
    };

    const submit = async () => {
      if (reactiveData.submitting) return;
      const session = formSession;
      if (!reactiveData.visible) return;
      const valid = await formRef.value?.validate().catch(() => false);
      if (session !== formSession || !reactiveData.visible) return;
      if (!valid) return;
      reactiveData.submitting = true;
      reactiveData.saveError = false;
      const finish = (close = true) => done(close, session);
      try {
        if (reactiveData.mode === 'add') {
          emit('add-thing', submitPayload(reactiveData.form), finish);
        } else {
          emit('update-thing', submitPayload(reactiveData.form), finish);
        }
      } catch {
        reactiveData.submitting = false;
        reactiveData.saveError = true;
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
      requestClose,
      onClosed,
    };
  },
});
