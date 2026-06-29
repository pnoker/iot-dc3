<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -      https://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
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
      <el-upload
        ref="formUploadRef"
        :auto-upload="false"
        :http-request="uploadRequest"
        :limit="1"
        :on-exceed="handleExceed"
        accept=".xlsx"
        class="things-dialog-upload"
        drag
      >
        <el-icon class="el-upload__icon">
          <UploadFilled />
        </el-icon>
        <div class="el-upload__text" v-html="$t('device.import.upload')"></div>
      </el-upload>
    </el-form>
    <div class="things-dialog-footer">
      <slot name="footer">
        <el-button @click="cancel">{{ $t('common.cancel') }}</el-button>
        <el-button plain @click="reset">{{ $t('common.reset') }}</el-button>
        <el-button plain type="warning" @click="importTemplate">{{ $t('device.import.template') }}</el-button>
        <el-button type="primary" @click="importThing">{{ $t('common.confirm') }}</el-button>
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
  import {successMessage} from '@/utils/notificationUtil';

  interface DictionaryPage {
    records: Dictionary[];
  }

  interface DeviceImportFormData {
    driverId: string;
    profileId: string;
    file?: UploadRawFile;
  }

  type DictionaryResponse = R<DictionaryPage>;

  const emit = defineEmits<{
    (e: 'import-template', formData: DeviceImportFormData, done: () => void): void;
    (e: 'import', formData: DeviceImportFormData, done: () => void): void;
  }>();

  const {t} = useI18n();
  const formDataRef = ref<FormInstance>();
  const formUploadRef = ref<UploadInstance>();

  const reactiveData = reactive({
    formData: {
      driverId: '',
      profileId: '',
    } as DeviceImportFormData,
    formVisible: false,
    formLoading: false,
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
  });

  const driverDictionary = async (query = '') => {
    reactiveData.driverLoading = true;
    try {
      const res = await listDriverDictionary<DictionaryResponse>({
        page: {size: 50, current: 1},
        label: query,
      });
      reactiveData.driverDictionary = res.data.records ?? [];
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
        page: {size: 50, current: 1},
        label: query,
      });
      reactiveData.profileDictionary = res.data.records ?? [];
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
  };

  const cancel = () => {
    reactiveData.formVisible = false;
    reactiveData.formLoading = false;
  };

  const reset = () => {
    const form = unref(formDataRef);
    form?.resetFields();
    formUploadRef.value?.clearFiles();
  };

  const importTemplate = async () => {
    const form = unref(formDataRef);
    if (!form) {
      return;
    }

    try {
      await form.validate();
      emit('import-template', {...reactiveData.formData}, () => {
        successMessage(t('device.import.templateSuccess'));
      });
    } catch {
      // validation errors are displayed by Element Plus
    }
  };

  const uploadRequest = (param: UploadRequestOptions): Promise<unknown> => {
    emit(
      'import',
      {
        ...reactiveData.formData,
        file: param.file as UploadRawFile,
      },
      () => {
        cancel();
        reset();
        successMessage(t('device.import.importSuccess'));
      }
    );
    return Promise.resolve();
  };

  const importThing = async () => {
    const form = unref(formDataRef);
    if (!form) {
      return;
    }

    try {
      await form.validate();
      formUploadRef.value?.submit();
      reactiveData.formLoading = true;
    } catch {
      // validation errors are displayed by Element Plus
    }
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
