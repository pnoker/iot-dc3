<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  -
  - This program is free software: you can redistribute it and/or modify
  - it under the terms of the GNU Affero General Public License as
  - published by the Free Software Foundation, either version 3 of the
  - License, or (at your option) any later version.
  -
  - This program is distributed in the hope that it will be useful,
  - but WITHOUT ANY WARRANTY; without even the implied warranty of
  - MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  - GNU Affero General Public License for more details.
  -
  - You should have received a copy of the GNU Affero General Public License
  - along with this program.  If not, see <https://www.gnu.org/licenses/>.
  -->

<template>
  <el-dialog
    v-model="reactiveData.formVisible"
    :append-to-body="true"
    :before-close="requestClose"
    :close-on-click-modal="false"
    :close-on-press-escape="!reactiveData.submitting"
    :show-close="!reactiveData.submitting"
    :title="$t('profile.add.title')"
    class="things-dialog"
    destroy-on-close
    draggable
    @closed="onClosed"
  >
    <el-alert
      v-if="reactiveData.saveError"
      :closable="false"
      :title="$t('common.saveFailed')"
      class="things-dialog-form-alert"
      show-icon
      type="error"
    />
    <el-form
      ref="formDataRef"
      v-loading="reactiveData.submitting"
      :aria-busy="reactiveData.submitting"
      :model="reactiveData.formData"
      :rules="formRule"
      label-position="top"
    >
      <el-form-item :label="$t('profile.add.profileName')" prop="profileName">
        <el-input
          v-model="reactiveData.formData.profileName"
          :disabled="reactiveData.submitting"
          :placeholder="$t('profile.add.profileNamePlaceholder')"
          clearable
          maxlength="32"
          show-word-limit
        ></el-input>
      </el-form-item>
      <el-form-item :label="$t('profile.add.description')" prop="remark">
        <el-input
          v-model="reactiveData.formData.remark"
          :disabled="reactiveData.submitting"
          :placeholder="$t('profile.add.descriptionPlaceholder')"
          clearable
          maxlength="300"
          show-word-limit
          type="textarea"
        ></el-input>
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="things-dialog-footer">
        <slot name="footer">
          <el-button :disabled="reactiveData.submitting" @click="cancel">{{ $t('common.cancel') }}</el-button>
          <el-button :disabled="reactiveData.submitting" plain @click="reset">{{ $t('common.reset') }}</el-button>
          <el-button :loading="reactiveData.submitting" type="primary" @click="addThing">
            {{ $t('common.confirm') }}
          </el-button>
        </slot>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import {computed, onBeforeUnmount, reactive, ref, unref} from 'vue';
import type {FormInstance, FormRules} from 'element-plus';
import {ElMessageBox} from 'element-plus';
import {useI18n} from 'vue-i18n';

import {nameRules, remarkRules} from '@/utils/formRuleUtil';

interface ProfileAddFormData {
  profileName?: string;
  remark?: string;
}

const emit = defineEmits<{
  (e: 'add-thing', formData: ProfileAddFormData, done: (successful?: boolean) => void): void;
}>();

const {t} = useI18n();
const formDataRef = ref<FormInstance>();

const emptyForm = (): ProfileAddFormData => ({profileName: '', remark: ''});

const formRule = reactive<FormRules>({
  profileName: nameRules(t, t('common.entityProfile')),
  remark: remarkRules(t),
});

const reactiveData = reactive({
  formData: emptyForm(),
  formVisible: false,
  submitting: false,
  saveError: false,
});
const initialForm = ref<ProfileAddFormData>(emptyForm());
let formSession = 0;
const isDirty = computed(
  () => reactiveData.formVisible && JSON.stringify(reactiveData.formData) !== JSON.stringify(initialForm.value)
);

const show = () => {
  formSession += 1;
  reactiveData.formData = emptyForm();
  initialForm.value = emptyForm();
  reactiveData.submitting = false;
  reactiveData.saveError = false;
  reactiveData.formVisible = true;
};

const cancel = () => {
  void requestClose();
};

const reset = () => {
  const form = unref(formDataRef);
  reactiveData.formData = {...initialForm.value};
  reactiveData.saveError = false;
  form?.clearValidate();
};

const onClosed = () => {
  formSession += 1;
  formDataRef.value?.clearValidate();
};

onBeforeUnmount(() => {
  formSession += 1;
});

const requestClose = async (done?: () => void) => {
  if (reactiveData.submitting) return;
  const session = formSession;
  if (!isDirty.value) {
    if (done) done();
    else reactiveData.formVisible = false;
    return;
  }
  try {
    await ElMessageBox.confirm(t('common.discardConfirm'), t('common.confirm'), {
      type: 'warning',
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
    });
    if (session !== formSession || !reactiveData.formVisible) return;
    if (done) done();
    else reactiveData.formVisible = false;
  } catch {
    // Keep the draft open when the user cancels the confirmation.
  }
};

const addThing = async () => {
  if (reactiveData.submitting) return;
  const form = unref(formDataRef);
  if (!form) {
    return;
  }

  const session = formSession;
  try {
    await form.validate();
    if (session !== formSession || !reactiveData.formVisible) return;
    reactiveData.submitting = true;
    reactiveData.saveError = false;
    try {
      emit('add-thing', {...reactiveData.formData}, (successful = true) => {
        if (session !== formSession) return;
        reactiveData.submitting = false;
        if (!successful) {
          reactiveData.saveError = true;
          return;
        }
        initialForm.value = {...reactiveData.formData};
        reactiveData.formVisible = false;
      });
    } catch {
      reactiveData.submitting = false;
      reactiveData.saveError = true;
    }
  } catch {
    // validation errors are displayed by Element Plus
  }
};

defineExpose({
  show,
  cancel,
  reset,
  addThing,
});
</script>
