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
  <div class="edit-card">
    <div class="edit-card-header">
      <el-card shadow="hover">
        <el-steps :active="reactiveData.active" align-center>
          <el-step :title="$t('point.edit.pointConfig')"></el-step>
          <el-step :title="$t('point.edit.complete')"></el-step>
        </el-steps>
      </el-card>
    </div>

    <div class="edit-card-body">
      <el-card v-if="reactiveData.active === 0" shadow="hover">
        <el-divider content-position="left">{{ $t('point.edit.pointConfig') }}</el-divider>
        <el-form ref="formDataRef" :model="reactiveData.pointFormData" :rules="pointFormRule" label-position="top">
          <el-form-item :label="$t('point.edit.pointName')" prop="pointName">
            <el-input
              v-model="reactiveData.pointFormData.pointName"
              :placeholder="$t('point.edit.pointNamePlaceholder')"
              clearable
            />
          </el-form-item>
          <el-form-item :label="$t('point.edit.dataType')" prop="pointTypeFlag">
            <el-select
              v-model="reactiveData.pointFormData.pointTypeFlag"
              :placeholder="$t('point.edit.dataTypeRequired')"
              clearable
            >
              <el-option :label="$t('dataType.string')" value="STRING" />
              <el-option :label="$t('dataType.byte')" value="BYTE" />
              <el-option :label="$t('dataType.short')" value="SHORT" />
              <el-option :label="$t('dataType.int')" value="INT" />
              <el-option :label="$t('dataType.long')" value="LONG" />
              <el-option :label="$t('dataType.float')" value="FLOAT" />
              <el-option :label="$t('dataType.double')" value="DOUBLE" />
              <el-option :label="$t('dataType.boolean')" value="BOOLEAN" />
            </el-select>
          </el-form-item>
          <el-form-item :label="$t('point.edit.rwType')" prop="rwFlag">
            <el-select
              v-model="reactiveData.pointFormData.rwFlag"
              :placeholder="$t('point.edit.rwTypeRequired')"
              clearable
            >
              <el-option :label="$t('status.readOnly')" value="R" />
              <el-option :label="$t('status.writeOnly')" value="W" />
              <el-option :label="$t('status.readWrite')" value="RW" />
            </el-select>
          </el-form-item>
          <el-form-item :label="$t('common.enableFlag')" prop="enableFlag">
            <enable-flag-segmented v-model="reactiveData.pointFormData.enableFlag" />
          </el-form-item>
          <el-form-item :label="$t('point.edit.accuracy')" prop="valueDecimal">
            <el-input
              v-model="reactiveData.pointFormData.valueDecimal"
              :placeholder="$t('point.edit.accuracyPlaceholder')"
              clearable
            />
          </el-form-item>
          <el-form-item :label="$t('point.edit.unit')" prop="unit">
            <el-input
              v-model="reactiveData.pointFormData.unit"
              :placeholder="$t('point.edit.unitPlaceholder')"
              clearable
            />
          </el-form-item>
          <el-form-item :label="$t('point.edit.baseValue')" prop="baseValue">
            <el-input
              v-model="reactiveData.pointFormData.baseValue"
              :placeholder="$t('point.edit.baseValuePlaceholder')"
              clearable
            />
          </el-form-item>
          <el-form-item :label="$t('point.edit.ratio')" prop="multiple">
            <el-input
              v-model="reactiveData.pointFormData.multiple"
              :placeholder="$t('point.edit.ratioPlaceholder')"
              clearable
            />
          </el-form-item>
          <el-form-item :label="$t('point.edit.description')" prop="remark">
            <el-input
              v-model="reactiveData.pointFormData.remark"
              :placeholder="$t('point.edit.descriptionPlaceholder')"
              clearable
              maxlength="300"
              show-word-limit
              type="textarea"
            />
          </el-form-item>
          <el-form-item class="edit-form-button">
            <el-button :icon="Back" plain type="success" @click="done">{{ $t('common.return') }}</el-button>
            <el-button :icon="RefreshLeft" @click="pointReset">{{ $t('common.reset') }}</el-button>
            <el-button :icon="Right" plain type="warning" @click="next">{{ $t('common.next') }}</el-button>
          </el-form-item>
        </el-form>
      </el-card>
      <el-card v-if="reactiveData.active === 1" shadow="hover">
        <el-divider content-position="left">{{ $t('point.edit.complete') }}</el-divider>
        <el-result
          :sub-title="$t('point.edit.completeSubTitle')"
          :title="$t('point.edit.completeTitle')"
          icon="success"
        >
          <template #extra>
            <el-button plain type="primary" @click="done">{{ $t('common.return') }}</el-button>
          </template>
        </el-result>
      </el-card>
    </div>
  </div>
</template>

<script lang="ts" src="./index.ts" />

<style lang="scss" scoped>
  @use '@/styles/edit-card.scss';
</style>
