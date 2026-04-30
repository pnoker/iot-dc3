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
          <el-step :title="$t('profile.edit.profileConfig')"></el-step>
          <el-step :title="$t('profile.edit.pointConfig')"></el-step>
          <el-step :title="$t('profile.edit.complete')"></el-step>
        </el-steps>
      </el-card>
    </div>

    <div class="edit-card-body">
      <el-card v-if="reactiveData.active === 0" shadow="hover">
        <el-divider content-position="left">{{ $t('profile.edit.profileConfig') }}</el-divider>
        <el-form ref="formDataRef" label-position="top" :model="reactiveData.profileFormData" :rules="formRule">
          <el-form-item :label="$t('profile.edit.profileName')" prop="name">
            <el-input
              v-model="reactiveData.profileFormData.profileName"
              clearable
              :placeholder="$t('profile.edit.profileNamePlaceholder')"
            />
          </el-form-item>
          <el-form-item :label="$t('common.enableFlag')" prop="enableFlag">
            <el-switch
              v-model="reactiveData.profileFormData.enableFlag"
              active-value="ENABLE"
              inactive-value="DISABLE"
              :active-text="$t('common.enable')"
              :inactive-text="$t('common.disable')"
            />
          </el-form-item>
          <el-form-item :label="$t('profile.edit.description')" prop="remark">
            <el-input
              v-model="reactiveData.profileFormData.remark"
              clearable
              maxlength="300"
              :placeholder="$t('profile.edit.descriptionPlaceholder')"
              show-word-limit
              type="textarea"
            />
          </el-form-item>
          <el-form-item class="edit-form-button">
            <el-button :icon="Back" plain type="success" @click="done">{{ $t('common.return') }}</el-button>
            <el-button :icon="RefreshLeft" @click="profileReset">{{ $t('common.reset') }}</el-button>
            <el-button :icon="Right" plain type="warning" @click="next">{{ $t('common.next') }}</el-button>
          </el-form-item>
        </el-form>
      </el-card>
      <el-card v-if="reactiveData.active === 1" shadow="hover">
        <el-divider content-position="left">{{ $t('profile.edit.pointConfig') }}</el-divider>
        <point
          :embedded="'edit'"
          :pre="true"
          :profile-id="reactiveData.id"
          @pre-handle="pre"
          @next-handle="next"
        ></point>
      </el-card>
      <el-card v-if="reactiveData.active === 2" shadow="hover">
        <el-divider content-position="left">{{ $t('profile.edit.complete') }}</el-divider>
        <el-result
          icon="success"
          :sub-title="$t('profile.edit.completeSubTitle')"
          :title="$t('profile.edit.completeTitle')"
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
  @use '@/components/card/styles/edit-card.scss';

  // label-position="top" 模式下 label 应该左对齐、宽度自适应,覆盖 edit-card.scss 中 100px 固定宽度的规则
  :deep(.el-form--label-top .el-form-item__label) {
    width: auto;
    text-align: left;

    &::after {
      display: none;
    }
  }
</style>
