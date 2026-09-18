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
    :close-on-press-escape="!reactiveData.formLoading"
    :show-close="!reactiveData.formLoading"
    :title="$t('device.import.title')"
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
        {{ $t('device.import.driver') }} {{ $t('common.retry') }}
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
      v-loading="reactiveData.formLoading"
      :aria-busy="reactiveData.formLoading"
      :model="reactiveData.formData"
      :rules="formRule"
      label-position="top"
    >
      <el-alert :closable="false" class="things-dialog-form-alert" show-icon type="warning">
        <p>{{ $t('device.import.instruction1') }}</p>
        <p>{{ $t('device.import.instruction2') }}</p>
      </el-alert>
      <el-form-item :label="$t('device.import.driver')" prop="driverId">
        <el-select
          v-model="reactiveData.formData.driverId"
          :disabled="reactiveData.formLoading"
          :loading="reactiveData.driverLoading"
          :placeholder="$t('device.import.driverPlaceholder')"
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
          :disabled="reactiveData.formLoading"
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
      <el-form-item :label="$t('device.import.file')" prop="file">
        <el-upload
          ref="formUploadRef"
          :auto-upload="false"
          :limit="1"
          :on-change="handleChange"
          :on-exceed="handleExceed"
          :on-remove="handleRemove"
          :disabled="reactiveData.formLoading"
          accept=".xlsx"
          class="things-dialog-upload"
          drag
        >
          <el-icon class="el-upload__icon">
            <UploadFilled/>
          </el-icon>
          <div class="el-upload__text" v-html="$t('device.import.upload')"></div>
        </el-upload>
      </el-form-item>
      <el-alert
        v-if="reactiveData.importStatus"
        :closable="false"
        :title="$t(`device.import.status.${reactiveData.importStatus.toLowerCase()}`)"
        :type="reactiveData.importStatus === 'FAILED' || reactiveData.importStatus === 'REQUEST_ERROR' || reactiveData.importStatus === 'EXPIRED' || reactiveData.importStatus === 'CANCELLED' ? 'error' : 'info'"
        show-icon
      />
    </el-form>
    <template #footer>
      <div class="things-dialog-footer">
        <slot name="footer">
          <el-button :disabled="reactiveData.formLoading" @click="cancel">{{ $t('common.cancel') }}</el-button>
          <el-button :disabled="reactiveData.formLoading" plain @click="reset">{{ $t('common.reset') }}</el-button>
          <el-button :disabled="reactiveData.formLoading" plain type="warning" @click="importTemplate">{{ $t('device.import.template') }}</el-button>
          <el-button :loading="reactiveData.formLoading" type="primary" @click="importThing">
            {{ $t('common.confirm') }}
          </el-button>
        </slot>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import {UploadFilled} from '@element-plus/icons-vue';
import type {
  FormInstance,
  FormRules,
  UploadInstance,
  UploadProps,
  UploadRawFile,
} from 'element-plus';
import {genFileId} from 'element-plus';
import {computed, onBeforeUnmount, reactive, ref, unref} from 'vue';
import {useI18n} from 'vue-i18n';
import {ElMessageBox} from 'element-plus';

import type {Dictionary} from '@/config/types';

import {listDriverDictionary, listProfileDictionary} from '@/api/dictionary';
import type {OperationUiStatus} from '@/config/types/operation';
import {successMessage} from '@/utils/notificationUtil';

interface DictionaryPage {
  items: Dictionary[];
}

interface DeviceImportFormData {
  driverId: string;
  profileId: string;
  file?: UploadRawFile;
}

type DictionaryResponse = DictionaryPage;

const emit = defineEmits<{
  (e: 'import-template', formData: DeviceImportFormData, done: (successful: boolean) => void): void;
  (e: 'import', formData: DeviceImportFormData, file: File, idempotencyKey: string,
    report: (status: OperationUiStatus) => void): void;
}>();

const {t} = useI18n();
const formDataRef = ref<FormInstance>();
const formUploadRef = ref<UploadInstance>();
const idempotencyKey = ref('');

const reactiveData = reactive({
  formData: {
    driverId: '',
    profileId: '',
  } as DeviceImportFormData,
  formVisible: false,
  formLoading: false,
  saveError: false,
  importStatus: null as OperationUiStatus | null,
  driverDictionary: [] as Dictionary[],
  driverLoading: false,
  driverError: false,
  profileDictionary: [] as Dictionary[],
  profileLoading: false,
  profileError: false,
});
let formSession = 0;
let driverRequest = 0;
let profileRequest = 0;
let importSession = 0;

const initialForm = ref<DeviceImportFormData>({driverId: '', profileId: ''});
const fileSignature = (file?: UploadRawFile) => (file ? `${file.name}:${file.size}:${file.lastModified}` : '');
const isDirty = computed(() => {
  if (!reactiveData.formVisible) return false;
  return (
    reactiveData.formData.driverId !== initialForm.value.driverId ||
    reactiveData.formData.profileId !== initialForm.value.profileId ||
    fileSignature(reactiveData.formData.file) !== fileSignature(initialForm.value.file)
  );
});

const formRule = reactive<FormRules>({
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
  file: [
    {
      required: true,
      message: () => t('device.import.fileRequired'),
      trigger: 'change',
    },
  ],
});

const driverDictionary = async (query = '') => {
  if (reactiveData.formLoading) return;
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
  if (reactiveData.formLoading) return;
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
  importSession += 1;
  driverRequest += 1;
  profileRequest += 1;
  reactiveData.formData = {driverId: '', profileId: ''};
  initialForm.value = {driverId: '', profileId: ''};
  reactiveData.driverDictionary = [];
  reactiveData.profileDictionary = [];
  reactiveData.driverError = false;
  reactiveData.profileError = false;
  reactiveData.saveError = false;
  reactiveData.formVisible = true;
  reactiveData.formLoading = false;
  reactiveData.importStatus = null;
  idempotencyKey.value = '';
  void driverDictionary();
  void profileDictionary();
};

const cancel = () => {
  void requestClose();
};

const reset = () => {
  const form = unref(formDataRef);
  reactiveData.formData = {...initialForm.value};
  form?.clearValidate();
  formUploadRef.value?.clearFiles();
  reactiveData.formData.file = initialForm.value.file;
  reactiveData.importStatus = null;
  reactiveData.saveError = false;
  idempotencyKey.value = '';
};

const onClosed = () => {
  formSession += 1;
  importSession += 1;
  driverRequest += 1;
  profileRequest += 1;
  reactiveData.driverLoading = false;
  reactiveData.profileLoading = false;
  formDataRef.value?.clearValidate();
};

onBeforeUnmount(() => {
  formSession += 1;
  importSession += 1;
  driverRequest += 1;
  profileRequest += 1;
});

const requestClose = async (done?: () => void) => {
  if (reactiveData.formLoading) return;
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

const importTemplate = async () => {
  if (reactiveData.formLoading) return;
  const form = unref(formDataRef);
  if (!form) {
    return;
  }

  const formSessionId = formSession;
  try {
    await form.validateField(['driverId', 'profileId']);
    if (formSessionId !== formSession || !reactiveData.formVisible) return;
    reactiveData.formLoading = true;
    reactiveData.saveError = false;
    const session = formSessionId;
    emit('import-template', {...reactiveData.formData}, (successful) => {
      if (session !== formSession) return;
      reactiveData.formLoading = false;
      if (successful) successMessage(t('device.import.templateSuccess'));
      else reactiveData.saveError = true;
    });
  } catch {
    // validation errors are displayed by Element Plus
  }
};

const reportImportStatus = (status: OperationUiStatus) => {
  reactiveData.importStatus = status;
  if (status === 'SUCCEEDED') {
    reactiveData.formLoading = false;
    initialForm.value = {...reactiveData.formData};
    importSession += 1;
    reactiveData.formVisible = false;
    successMessage(t('device.import.importSuccess'));
  } else if (status === 'FAILED' || status === 'CANCELLED' || status === 'EXPIRED') {
    reactiveData.formLoading = false;
    idempotencyKey.value = '';
  } else if (status === 'REQUEST_ERROR') {
    reactiveData.formLoading = false;
    reactiveData.saveError = true;
  }
};

const startImport = (file: File) => {
  const session = ++importSession;
  emit('import', reactiveData.formData, file, idempotencyKey.value, (status) => {
    if (session !== importSession) return;
    reportImportStatus(status);
  });
};

const importThing = async () => {
  if (reactiveData.formLoading) return;
  const form = unref(formDataRef);
  if (!form) {
    return;
  }

  const formSessionId = formSession;
  try {
    await form.validate();
    if (formSessionId !== formSession || !reactiveData.formVisible) return;
    const file = reactiveData.formData.file;
    if (!file) {
      await form.validateField('file');
      return;
    }
    reactiveData.formLoading = true;
    reactiveData.saveError = false;
    reactiveData.importStatus = 'PENDING';
    if (!idempotencyKey.value) idempotencyKey.value = crypto.randomUUID();
    startImport(file);
  } catch {
    // validation errors are displayed by Element Plus
  }
};

const handleChange: UploadProps['onChange'] = (file) => {
  if (file.status !== 'ready') return;
  reactiveData.formData.file = file.raw;
  reactiveData.importStatus = null;
  idempotencyKey.value = '';
  void formDataRef.value?.validateField('file');
};

const handleRemove: UploadProps['onRemove'] = () => {
  reactiveData.formData.file = undefined;
  reactiveData.importStatus = null;
  idempotencyKey.value = '';
};

const handleExceed: UploadProps['onExceed'] = (files) => {
  formUploadRef.value?.clearFiles();
  const file = files[0] as UploadRawFile;
  file.uid = genFileId();
  formUploadRef.value?.handleStart(file);
};

defineExpose({
  show,
  cancel,
  reset,
  importTemplate,
  importThing,
});
</script>
