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
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
    :title="$t('device.import.title')"
    class="things-dialog"
    draggable
  >
    <el-form
      ref="formDataRef"
      v-loading="reactiveData.formLoading"
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
          :http-request="uploadRequest"
          :limit="1"
          :on-change="handleChange"
          :on-exceed="handleExceed"
          :on-remove="handleRemove"
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
    <div class="things-dialog-footer">
      <slot name="footer">
        <el-button :disabled="reactiveData.formLoading" @click="cancel">{{ $t('common.cancel') }}</el-button>
        <el-button :disabled="reactiveData.formLoading" plain @click="reset">{{ $t('common.reset') }}</el-button>
        <el-button :disabled="reactiveData.formLoading" plain type="warning" @click="importTemplate">{{ $t('device.import.template') }}</el-button>
        <el-button :disabled="reactiveData.formLoading" type="primary" @click="importThing">{{ $t('common.confirm') }}</el-button>
      </slot>
    </div>
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
  UploadRequestOptions,
} from 'element-plus';
import {genFileId} from 'element-plus';
import {reactive, ref, unref} from 'vue';
import {useI18n} from 'vue-i18n';

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
  importStatus: null as OperationUiStatus | null,
  driverDictionary: [] as Dictionary[],
  driverLoading: false,
  profileDictionary: [] as Dictionary[],
  profileLoading: false,
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
  reactiveData.driverLoading = true;
  try {
    const res = await listDriverDictionary<DictionaryResponse>({
      offset: 0,
      limit: 50,
      label: query,
    });
    reactiveData.driverDictionary = res.items ?? [];
  } catch {
    // nothing to do
  } finally {
    reactiveData.driverLoading = false;
  }
};

const driverDictionaryVisible = (visible: boolean) => {
  if (visible) {
    void driverDictionary();
  }
};

const profileDictionary = async (query = '') => {
  reactiveData.profileLoading = true;
  try {
    const res = await listProfileDictionary<DictionaryResponse>({
      offset: 0,
      limit: 50,
      label: query,
    });
    reactiveData.profileDictionary = res.items ?? [];
  } catch {
    // nothing to do
  } finally {
    reactiveData.profileLoading = false;
  }
};

const profileDictionaryVisible = (visible: boolean) => {
  if (visible) {
    void profileDictionary();
  }
};

const show = () => {
  reactiveData.formVisible = true;
  reactiveData.formLoading = false;
  reactiveData.importStatus = null;
};

const cancel = () => {
  reactiveData.formVisible = false;
  reactiveData.formLoading = false;
};

const reset = () => {
  const form = unref(formDataRef);
  form?.resetFields();
  formUploadRef.value?.clearFiles();
  reactiveData.formData.file = undefined;
  reactiveData.importStatus = null;
  idempotencyKey.value = '';
};

const importTemplate = async () => {
  const form = unref(formDataRef);
  if (!form) {
    return;
  }

  try {
    await form.validateField(['driverId', 'profileId']);
    reactiveData.formLoading = true;
    emit('import-template', {...reactiveData.formData}, (successful) => {
      reactiveData.formLoading = false;
      if (successful) successMessage(t('device.import.templateSuccess'));
    });
  } catch {
    // validation errors are displayed by Element Plus
  }
};

const uploadRequest = (param: UploadRequestOptions): Promise<unknown> => {
  emit('import', reactiveData.formData, param.file as File, idempotencyKey.value, (status) => {
    reactiveData.importStatus = status;
    if (status === 'SUCCEEDED') {
      reactiveData.formLoading = false;
      cancel();
      reset();
      successMessage(t('device.import.importSuccess'));
    } else if (status === 'FAILED' || status === 'CANCELLED' || status === 'EXPIRED') {
      reactiveData.formLoading = false;
      idempotencyKey.value = '';
    } else if (status === 'REQUEST_ERROR') {
      reactiveData.formLoading = false;
    }
  });
  return Promise.resolve();
};

const importThing = async () => {
  const form = unref(formDataRef);
  if (!form) {
    return;
  }

  try {
    await form.validate();
    reactiveData.formLoading = true;
    reactiveData.importStatus = 'PENDING';
    if (!idempotencyKey.value) idempotencyKey.value = crypto.randomUUID();
    formUploadRef.value?.submit();
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
