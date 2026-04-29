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
    <el-form ref="formDataRef" :model="reactiveData.formData" :rules="formRule">
      <el-alert :closable="false" class="things-dialog-form-alert" show-icon type="warning">
        <p>{{ $t('device.import.instruction1') }}</p>
        <p>{{ $t('device.import.instruction2') }}</p>
      </el-alert>
      <el-form-item class="things-dialog-form-item" :label="$t('device.import.driver')" prop="driverId">
        <el-select
          v-model="reactiveData.formData.driverId"
          class="edit-form-special"
          clearable
          :placeholder="$t('device.import.driverPlaceholder')"
          @visible-change="driverDictionaryVisible"
        >
          <div class="tool-select">
            <el-form-item class="tool-select-input">
              <el-input
                v-model="reactiveData.driverQuery"
                clearable
                :placeholder="$t('device.import.driverPlaceholder')"
                @input="driverDictionary"
              />
            </el-form-item>
            <el-pagination
              :current-page="+reactiveData.driverPage.current"
              :hide-on-single-page="true"
              :page-size="+reactiveData.driverPage.size"
              :pager-count="5"
              :total="+reactiveData.driverPage.total"
              background
              class="tool-select-pagination"
              layout="prev, pager, next"
              @current-change="driverCurrentChange"
            ></el-pagination>
          </div>
          <el-option
            v-for="dictionary in reactiveData.driverDictionary"
            :key="dictionary.value"
            :label="dictionary.label"
            :value="dictionary.value"
          ></el-option>
        </el-select>
      </el-form-item>
      <el-form-item class="things-dialog-form-item" :label="$t('device.add.profiles')" prop="profileIds">
        <el-select
          v-model="reactiveData.formData.profileIds"
          :multiple="true"
          class="edit-form-special"
          clearable
          :placeholder="$t('device.add.profilePlaceholder')"
          @visible-change="profileDictionaryVisible"
        >
          <div class="tool-select">
            <el-form-item class="tool-select-input">
              <el-input
                v-model="reactiveData.profileQuery"
                clearable
                :placeholder="$t('device.add.profilePlaceholder')"
                @input="profileDictionary"
              />
            </el-form-item>
            <el-pagination
              :current-page="+reactiveData.profilePage.current"
              :hide-on-single-page="true"
              :page-size="+reactiveData.profilePage.size"
              :pager-count="5"
              :total="+reactiveData.profilePage.total"
              background
              class="tool-select-pagination"
              layout="prev, pager, next"
              @current-change="profileCurrentChange"
            ></el-pagination>
          </div>
          <el-option
            v-for="dictionary in reactiveData.profileDictionary"
            :key="dictionary.value"
            :label="dictionary.label"
            :value="dictionary.value"
          ></el-option>
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
  @use '@/components/card/styles/tool-card';
</style>
