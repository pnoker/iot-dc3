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
    v-loading="reactiveData.formLoading"
    :append-to-body="true"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
    class="things-dialog"
    draggable
    :title="$t('device.import.title')"
  >
    <el-form ref="formDataRef" :model="reactiveData.formData" :rules="formRule" label-position="top">
      <el-alert :closable="false" class="things-dialog-form-alert" show-icon type="warning">
        <p>{{ $t('device.import.instruction1') }}</p>
        <p>{{ $t('device.import.instruction2') }}</p>
      </el-alert>
      <el-form-item :label="$t('device.import.driver')" prop="driverId">
        <el-select
          v-model="reactiveData.formData.driverId"
          clearable
          filterable
          remote
          reserve-keyword
          :placeholder="$t('device.import.driverPlaceholder')"
          :remote-method="driverDictionary"
          :loading="reactiveData.driverLoading"
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
      <el-form-item :label="$t('device.add.profiles')" prop="profileIds">
        <el-select
          v-model="reactiveData.formData.profileIds"
          :multiple="true"
          clearable
          filterable
          remote
          reserve-keyword
          :placeholder="$t('device.add.profilePlaceholder')"
          :remote-method="profileDictionary"
          :loading="reactiveData.profileLoading"
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
        <el-button plain type="success" @click="reset">{{ $t('common.reset') }}</el-button>
        <el-button plain type="warning" @click="importTemplate">{{ $t('device.import.template') }}</el-button>
        <el-button type="primary" @click="importThing">{{ $t('common.confirm') }}</el-button>
      </slot>
    </div>
  </el-dialog>
</template>

<script lang="ts" src="./index.ts" />

<style lang="scss" scoped>
  @use '@/components/dialog/styles/things-dialog';
</style>
