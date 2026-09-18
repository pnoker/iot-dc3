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
    :title="$t('device.add.title')"
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
    <el-alert
      v-if="reactiveData.driverError || reactiveData.profileError"
      :closable="false"
      :title="$t('common.loadFailed')"
      class="things-dialog-form-alert"
      show-icon
      type="error"
    >
      <el-button
        v-if="reactiveData.driverError"
        :loading="reactiveData.driverLoading"
        link
        type="danger"
        @click="driverDictionary()"
      >
        {{ $t('device.add.driver') }} {{ $t('common.retry') }}
      </el-button>
      <el-button
        v-if="reactiveData.profileError"
        :loading="reactiveData.profileLoading"
        link
        type="danger"
        @click="profileDictionary()"
      >
        {{ $t('device.add.profile') }} {{ $t('common.retry') }}
      </el-button>
    </el-alert>
    <el-form
      ref="formDataRef"
      v-loading="reactiveData.submitting"
      :aria-busy="reactiveData.submitting"
      :model="reactiveData.formData"
      :rules="formRule"
      label-position="top"
    >
      <el-form-item :label="$t('device.add.deviceName')" prop="deviceName">
        <el-input
          v-model="reactiveData.formData.deviceName"
          :disabled="reactiveData.submitting"
          :placeholder="$t('device.add.deviceNamePlaceholder')"
          clearable
          maxlength="32"
          show-word-limit
        ></el-input>
      </el-form-item>
      <el-form-item :label="$t('device.add.driver')" prop="driverId">
        <el-select
          v-model="reactiveData.formData.driverId"
          :disabled="reactiveData.submitting"
          :loading="reactiveData.driverLoading"
          :placeholder="$t('device.add.driverPlaceholder')"
          :remote-method="driverDictionary"
          clearable
          filterable
          remote
          reserve-keyword
          @visible-change="driverDictionaryVisible"
        >
          <el-option
            v-for="dictionary in reactiveData.driverDictionary"
            :key="dictionary.value"
            :label="dictionary.label"
            :value="dictionary.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('device.add.profile')" prop="profileId">
        <el-select
          v-model="reactiveData.formData.profileId"
          :disabled="reactiveData.submitting"
          :loading="reactiveData.profileLoading"
          :placeholder="$t('device.add.profilePlaceholder')"
          :remote-method="profileDictionary"
          clearable
          filterable
          remote
          reserve-keyword
          @visible-change="profileDictionaryVisible"
        >
          <el-option
            v-for="dictionary in reactiveData.profileDictionary"
            :key="dictionary.value"
            :label="dictionary.label"
            :value="dictionary.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('device.add.description')" prop="remark">
        <el-input
          v-model="reactiveData.formData.remark"
          :disabled="reactiveData.submitting"
          :placeholder="$t('device.add.descriptionPlaceholder')"
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

import type {Dictionary} from '@/config/types';

import {nameRules, remarkRules} from '@/utils/formRuleUtil';
import {listDriverDictionary, listProfileDictionary} from '@/api/dictionary';

interface DeviceAddFormData {
  deviceName: string;
  driverId: string;
  profileId: string;
  remark: string;
}

interface DictionaryPage {
  items: Dictionary[];
}

type DictionaryResponse = DictionaryPage;

const emit = defineEmits<{
  (e: 'add', formData: DeviceAddFormData, done: (successful?: boolean) => void): void;
}>();

const {t} = useI18n();
const formDataRef = ref<FormInstance>();

const emptyForm = (): DeviceAddFormData => ({
  deviceName: '',
  driverId: '',
  profileId: '',
  remark: '',
});

const reactiveData = reactive({
  formData: emptyForm(),
  formVisible: false,
  submitting: false,
  saveError: false,
  driverDictionary: [] as Dictionary[],
  driverLoading: false,
  driverError: false,
  profileDictionary: [] as Dictionary[],
  profileLoading: false,
  profileError: false,
});
const initialForm = ref<DeviceAddFormData>(emptyForm());
let formSession = 0;
let driverRequest = 0;
let profileRequest = 0;

const isDirty = computed(
  () => reactiveData.formVisible && JSON.stringify(reactiveData.formData) !== JSON.stringify(initialForm.value)
);

const formRule = reactive<FormRules>({
  deviceName: nameRules(t, t('common.entityDevice')),
  driverId: [
    {
      required: true,
      message: () => t('device.add.driverRequired'),
      trigger: 'change',
    },
  ],
  profileId: [
    {
      required: true,
      message: () => t('device.add.profileRequired'),
      trigger: 'change',
    },
  ],
  remark: remarkRules(t),
});

const driverDictionary = async (query = '') => {
  if (reactiveData.submitting) return;
  const requestId = ++driverRequest;
  reactiveData.driverLoading = true;
  reactiveData.driverError = false;
  try {
    const res = await listDriverDictionary<DictionaryResponse>({
      offset: 0,
      limit: 50,
      label: query,
    });
    if (requestId === driverRequest && reactiveData.formVisible) {
      reactiveData.driverDictionary = res.items ?? [];
    }
  } catch {
    if (requestId === driverRequest && reactiveData.formVisible) reactiveData.driverError = true;
  } finally {
    if (requestId === driverRequest) reactiveData.driverLoading = false;
  }
};

const driverDictionaryVisible = (visible: boolean) => {
  if (visible) {
    void driverDictionary();
  }
};

const profileDictionary = async (query = '') => {
  if (reactiveData.submitting) return;
  const requestId = ++profileRequest;
  reactiveData.profileLoading = true;
  reactiveData.profileError = false;
  try {
    const res = await listProfileDictionary<DictionaryResponse>({
      offset: 0,
      limit: 50,
      label: query,
    });
    if (requestId === profileRequest && reactiveData.formVisible) {
      reactiveData.profileDictionary = res.items ?? [];
    }
  } catch {
    if (requestId === profileRequest && reactiveData.formVisible) reactiveData.profileError = true;
  } finally {
    if (requestId === profileRequest) reactiveData.profileLoading = false;
  }
};

const profileDictionaryVisible = (visible: boolean) => {
  if (visible) {
    void profileDictionary();
  }
};

const show = () => {
  formSession += 1;
  driverRequest += 1;
  profileRequest += 1;
  reactiveData.formData = emptyForm();
  initialForm.value = emptyForm();
  reactiveData.driverDictionary = [];
  reactiveData.profileDictionary = [];
  reactiveData.driverError = false;
  reactiveData.profileError = false;
  reactiveData.saveError = false;
  reactiveData.submitting = false;
  reactiveData.formVisible = true;
  void driverDictionary();
  void profileDictionary();
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
  driverRequest += 1;
  profileRequest += 1;
  reactiveData.driverLoading = false;
  reactiveData.profileLoading = false;
  formDataRef.value?.clearValidate();
};

onBeforeUnmount(() => {
  formSession += 1;
  driverRequest += 1;
  profileRequest += 1;
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
      emit('add', {...reactiveData.formData}, (successful = true) => {
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
