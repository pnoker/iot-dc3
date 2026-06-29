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
    :title="$t('device.add.title')"
    class="things-dialog"
    draggable
  >
    <el-form ref="formDataRef" :model="reactiveData.formData" :rules="formRule" label-position="top">
      <el-form-item :label="$t('device.add.deviceName')" prop="deviceName">
        <el-input
          v-model="reactiveData.formData.deviceName"
          :placeholder="$t('device.add.deviceNamePlaceholder')"
          clearable
          maxlength="32"
          show-word-limit
        ></el-input>
      </el-form-item>
      <el-form-item :label="$t('device.add.driver')" prop="driverId">
        <el-select
          v-model="reactiveData.formData.driverId"
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
          :placeholder="$t('device.add.descriptionPlaceholder')"
          clearable
          maxlength="300"
          show-word-limit
          type="textarea"
        ></el-input>
      </el-form-item>
    </el-form>
    <div class="things-dialog-footer">
      <slot name="footer">
        <el-button @click="cancel">{{ $t('common.cancel') }}</el-button>
        <el-button plain @click="reset">{{ $t('common.reset') }}</el-button>
        <el-button type="primary" @click="addThing">{{ $t('common.confirm') }}</el-button>
      </slot>
    </div>
  </el-dialog>
</template>

<script lang="ts" setup>
  import {reactive, ref, unref} from 'vue';
  import type {FormInstance, FormRules} from 'element-plus';
  import {useI18n} from 'vue-i18n';

  import type {Dictionary} from '@/config/types';

  import {successMessage} from '@/utils/notificationUtil';
  import {nameRules, remarkRules} from '@/utils/formRuleUtil';
  import {listDriverDictionary, listProfileDictionary} from '@/api/dictionary';

  interface DeviceAddFormData {
    deviceName: string;
    driverId: string;
    profileId: string;
    remark: string;
  }

  interface DictionaryPage {
    records: Dictionary[];
  }

  type DictionaryResponse = R<DictionaryPage>;

  const emit = defineEmits<{
    (e: 'add', formData: DeviceAddFormData, done: () => void): void;
  }>();

  const {t} = useI18n();
  const formDataRef = ref<FormInstance>();

  const reactiveData = reactive({
    formData: {
      deviceName: '',
      driverId: '',
      profileId: '',
      remark: '',
    } as DeviceAddFormData,
    formVisible: false,
    driverDictionary: [] as Dictionary[],
    driverLoading: false,
    profileDictionary: [] as Dictionary[],
    profileLoading: false,
  });

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
  };

  const cancel = () => {
    reactiveData.formVisible = false;
  };

  const reset = () => {
    const form = unref(formDataRef);
    form?.resetFields();
  };

  const addThing = async () => {
    const form = unref(formDataRef);
    if (!form) {
      return;
    }

    try {
      await form.validate();
      emit('add', {...reactiveData.formData}, () => {
        cancel();
        reset();
        successMessage();
      });
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
