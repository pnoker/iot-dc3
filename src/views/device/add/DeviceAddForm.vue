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
    class="things-dialog"
    draggable
    :title="$t('device.add.title')"
  >
    <el-form ref="formDataRef" :model="reactiveData.formData" :rules="formRule" label-position="top">
      <el-form-item :label="$t('device.add.deviceName')" prop="deviceName">
        <el-input
          v-model="reactiveData.formData.deviceName"
          clearable
          :placeholder="$t('device.add.deviceNamePlaceholder')"
        ></el-input>
      </el-form-item>
      <el-form-item :label="$t('device.add.driver')" prop="driverId">
        <el-select
          v-model="reactiveData.formData.driverId"
          clearable
          filterable
          remote
          reserve-keyword
          :placeholder="$t('device.add.driverPlaceholder')"
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
      <el-form-item :label="$t('device.add.description')" prop="remark">
        <el-input
          v-model="reactiveData.formData.remark"
          clearable
          maxlength="300"
          :placeholder="$t('device.add.descriptionPlaceholder')"
          show-word-limit
          type="textarea"
        ></el-input>
      </el-form-item>
    </el-form>
    <div class="things-dialog-footer">
      <slot name="footer">
        <el-button @click="cancel">{{ $t('common.cancel') }}</el-button>
        <el-button plain type="success" @click="reset">{{ $t('common.reset') }}</el-button>
        <el-button type="primary" @click="addThing">{{ $t('common.confirm') }}</el-button>
      </slot>
    </div>
  </el-dialog>
</template>

<script lang="ts" src="./index.ts" />

<style lang="scss" scoped>
  @use '@/styles/things-dialog.scss';
</style>
