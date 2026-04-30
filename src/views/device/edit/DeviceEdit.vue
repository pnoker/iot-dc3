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
        <el-steps :active="reactiveData.active" align-center finish-status="success">
          <el-step :title="$t('device.edit.deviceConfig')"></el-step>
          <el-step :title="$t('device.edit.driverConfig')"></el-step>
          <el-step :title="$t('device.edit.pointConfig')"></el-step>
          <el-step :title="$t('device.edit.complete')"></el-step>
        </el-steps>
      </el-card>
    </div>

    <div class="edit-card-body">
      <el-card v-if="reactiveData.active === 0" shadow="hover">
        <el-divider content-position="left">{{ $t('device.edit.deviceConfig') }}</el-divider>
        <el-form ref="deviceFormRef" label-position="top" :model="reactiveData.deviceFormData" :rules="deviceFormRule">
          <el-form-item :label="$t('device.edit.deviceName')" prop="deviceName">
            <el-input
              v-model="reactiveData.deviceFormData.deviceName"
              clearable
              :placeholder="$t('device.edit.deviceNamePlaceholder')"
            />
          </el-form-item>
          <el-form-item :label="$t('device.edit.driver')" prop="driverId">
            <el-select
              v-model="reactiveData.deviceFormData.driverId"
              clearable
              filterable
              remote
              reserve-keyword
              :placeholder="$t('device.edit.driverPlaceholder')"
              :remote-method="driverDictionary"
              :loading="reactiveData.driverLoading"
              @change="changeAttribute"
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
          <el-form-item :label="$t('common.enableFlag')" prop="enableFlag">
            <el-switch
              v-model="reactiveData.deviceFormData.enableFlag"
              active-value="ENABLE"
              inactive-value="DISABLE"
              :active-text="$t('common.enable')"
              :inactive-text="$t('common.disable')"
            />
          </el-form-item>
          <el-form-item :label="$t('device.edit.profiles')" prop="profileIds">
            <el-select
              v-model="reactiveData.deviceFormData.profileIds"
              :multiple="true"
              clearable
              filterable
              remote
              reserve-keyword
              :placeholder="$t('device.edit.driverPlaceholder')"
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
          <el-form-item :label="$t('device.edit.description')" prop="remark">
            <el-input
              v-model="reactiveData.deviceFormData.remark"
              clearable
              maxlength="300"
              :placeholder="$t('device.edit.descriptionPlaceholder')"
              show-word-limit
              type="textarea"
            />
          </el-form-item>
          <el-form-item class="edit-form-button">
            <el-button :icon="Back" plain type="success" @click="done">{{ $t('common.return') }}</el-button>
            <el-button :icon="RefreshLeft" @click="deviceReset">{{ $t('common.reset') }}</el-button>
            <el-button :icon="Right" plain type="warning" @click="next">{{ $t('common.next') }}</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card
        v-if="reactiveData.active === 1 && reactiveData.driverAttributes && reactiveData.driverAttributes.length > 0"
        shadow="hover"
      >
        <el-divider content-position="left">{{ $t('device.edit.driverConfig') }}</el-divider>
        <el-alert
          :closable="false"
          :description="$t('device.edit.driverConfigTip')"
          :title="$t('device.edit.driverConfig')"
          type="success"
        />
        <el-form
          v-if="reactiveData.driverFormData.length > 0"
          ref="driverFormRef"
          label-position="top"
          :model="reactiveData.driverFormData"
        >
          <el-form-item
            v-for="attribute in reactiveData.driverAttributes"
            :key="attribute.id"
            :label="attribute.attributeName"
            :prop="attribute.attributeCode"
          >
            <el-input
              v-if="reactiveData.driverFormData[attribute.attributeCode]"
              :key="reactiveData.driverFormData[attribute.attributeCode].id"
              v-model="reactiveData.driverFormData[attribute.attributeCode].configValue"
              :placeholder="'Enter ' + attribute.attributeName"
              clearable
              @keyup.enter="driverUpdate"
            />
          </el-form-item>
          <el-form-item class="edit-form-button">
            <el-button :icon="Back" plain type="success" @click="pre">{{ $t('common.previous') }}</el-button>
            <el-button :icon="RefreshLeft" @click="driverInfoReset">{{ $t('common.reset') }}</el-button>
            <el-button :icon="Right" plain type="warning" @click="next">{{ $t('common.next') }}</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card v-if="reactiveData.active === 2" shadow="hover">
        <el-divider content-position="left">{{ $t('device.edit.pointConfig') }}</el-divider>
        <el-alert
          :closable="false"
          :description="$t('device.edit.pointConfigTip')"
          :title="$t('device.edit.pointConfig')"
          type="success"
        />
        <el-form
          v-if="reactiveData.pointFormData.length > 0"
          ref="pointFormRef"
          label-position="top"
          :model="reactiveData.pointFormData"
        >
          <el-form-item :label="$t('device.edit.pointName')" prop="pointName">
            <el-input v-model="reactiveData.pointFormData.pointName" disabled />
          </el-form-item>
          <el-form-item
            v-for="attribute in reactiveData.pointAttributes"
            :key="attribute.id"
            :label="attribute.attributeName"
            :prop="attribute.attributeCode"
          >
            <el-input
              v-if="reactiveData.pointFormData[attribute.attributeCode]"
              :key="reactiveData.pointFormData[attribute.attributeCode].id"
              v-model="reactiveData.pointFormData[attribute.attributeCode].configValue"
              :placeholder="'Enter ' + attribute.attributeName"
              clearable
              @keyup.enter="pointUpdate"
            />
            <el-input v-else disabled />
          </el-form-item>
        </el-form>
        <el-form-item class="edit-form-button">
          <el-button :icon="Back" plain type="success" @click="pre">{{ $t('common.previous') }}</el-button>
          <el-button :disabled="!hasPointFormData" :icon="Edit" type="primary" @click="pointUpdate">
            {{ $t('common.edit') }}
          </el-button>
          <el-button :disabled="!hasPointFormData" :icon="RefreshLeft" @click="pointInfoReset">
            {{ $t('common.reset') }}
          </el-button>
          <el-button :icon="Check" plain type="warning" @click="next">{{ $t('common.next') }}</el-button>
        </el-form-item>
        <el-row>
          <el-col v-for="data in 12" :key="data" :lg="8" :md="12" :sm="12" :xl="6" :xs="24">
            <skeleton-card :footer="false" :loading="reactiveData.loading"></skeleton-card>
          </el-col>
          <el-col v-for="data in reactiveData.pointInfoData" :key="data.id" :lg="8" :md="12" :sm="12" :xl="6" :xs="24">
            <point-info-card
              :attributes="reactiveData.pointAttributes"
              :data="data"
              @select="selectPoint"
            ></point-info-card>
          </el-col>
        </el-row>
      </el-card>
      <el-card v-if="reactiveData.active === 3" shadow="hover">
        <el-divider content-position="left">{{ $t('device.edit.complete') }}</el-divider>
        <el-result
          icon="success"
          :sub-title="$t('device.edit.completeSubTitle')"
          :title="$t('device.edit.completeTitle')"
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
  @use '@/components/card/styles/edit-card';
  @use '@/components/card/styles/tool-card';

  // label-position="top" 模式下 label 应该左对齐、宽度自适应,覆盖 edit-card.scss 中 100px 固定宽度的规则
  :deep(.el-form--label-top .el-form-item__label) {
    width: auto;
    text-align: left;

    &::after {
      display: none;
    }
  }
</style>
