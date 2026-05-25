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
          <el-step :title="$t('profile.edit.commandConfig')"></el-step>
          <el-step :title="$t('profile.edit.eventConfig')"></el-step>
          <el-step :title="$t('profile.edit.complete')"></el-step>
        </el-steps>
      </el-card>
    </div>

    <div class="edit-card-body">
      <el-card v-if="reactiveData.active === 0" shadow="hover">
        <el-divider content-position="left">{{ $t('profile.edit.profileConfig') }}</el-divider>
        <el-form ref="formDataRef" :model="reactiveData.profileFormData" :rules="formRule" label-position="top">
          <el-form-item :label="$t('profile.edit.profileName')" prop="name">
            <el-input
              v-model="reactiveData.profileFormData.profileName"
              :placeholder="$t('profile.edit.profileNamePlaceholder')"
              clearable
            />
          </el-form-item>
          <el-form-item :label="$t('common.enableFlag')" prop="enableFlag">
            <enable-flag-segmented v-model="reactiveData.profileFormData.enableFlag" />
          </el-form-item>
          <el-form-item :label="$t('profile.edit.description')" prop="remark">
            <el-input
              v-model="reactiveData.profileFormData.remark"
              :placeholder="$t('profile.edit.descriptionPlaceholder')"
              clearable
              maxlength="300"
              show-word-limit
              type="textarea"
            />
          </el-form-item>
          <el-form-item class="edit-form-button">
            <el-button :icon="Back" plain @click="done">{{ $t('common.return') }}</el-button>
            <el-button :icon="RefreshLeft" @click="profileReset">{{ $t('common.reset') }}</el-button>
            <el-button :icon="Right" plain type="primary" @click="next">{{ $t('common.next') }}</el-button>
          </el-form-item>
        </el-form>
      </el-card>
      <el-card v-if="reactiveData.active === 1" shadow="hover">
        <el-divider content-position="left">{{ $t('profile.edit.pointConfig') }}</el-divider>
        <point :embedded="'edit'" :profile-id="reactiveData.id"></point>
        <el-form-item class="edit-form-button">
          <el-button :icon="Back" plain @click="pre">{{ $t('common.previous') }}</el-button>
          <el-button :icon="Right" plain type="primary" @click="next">{{ $t('common.next') }}</el-button>
        </el-form-item>
      </el-card>
      <el-card v-if="reactiveData.active === 2" shadow="hover">
        <el-divider content-position="left">{{ $t('profile.edit.commandConfig') }}</el-divider>
        <command-list :embedded="'edit'" :profile-id="reactiveData.id"></command-list>
        <el-form-item class="edit-form-button">
          <el-button :icon="Back" plain @click="pre">{{ $t('common.previous') }}</el-button>
          <el-button :icon="Right" plain type="primary" @click="next">{{ $t('common.next') }}</el-button>
        </el-form-item>
      </el-card>
      <el-card v-if="reactiveData.active === 3" shadow="hover">
        <el-divider content-position="left">{{ $t('profile.edit.eventConfig') }}</el-divider>
        <event-list :embedded="'edit'" :profile-id="reactiveData.id"></event-list>
        <el-form-item class="edit-form-button">
          <el-button :icon="Back" plain @click="pre">{{ $t('common.previous') }}</el-button>
          <el-button :icon="Right" plain type="primary" @click="next">{{ $t('common.next') }}</el-button>
        </el-form-item>
      </el-card>
      <el-card v-if="reactiveData.active === 4" shadow="hover">
        <el-divider content-position="left">{{ $t('profile.edit.complete') }}</el-divider>
        <el-result
          :sub-title="$t('profile.edit.completeSubTitle')"
          :title="$t('profile.edit.completeTitle')"
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
