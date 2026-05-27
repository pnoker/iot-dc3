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
  <div>
    <base-card>
      <el-tabs v-model="reactiveData.active" @tab-click="changeActive">
        <el-tab-pane :label="$t('profile.edit.profileConfig')" name="profileConfig">
          <div class="config-toolbar">
            <div class="config-toolbar__actions">
              <el-button :icon="RefreshLeft" size="small" @click="profileReset">{{ $t('common.reset') }}</el-button>
              <el-button :icon="Check" plain size="small" type="primary" @click="profileSave">
                {{ $t('common.save') }}
              </el-button>
            </div>
          </div>
          <el-form ref="formDataRef" :model="reactiveData.profileFormData" :rules="formRule" label-position="top">
            <el-form-item :label="$t('profile.edit.profileName')" prop="profileName">
              <el-input
                v-model="reactiveData.profileFormData.profileName"
                :placeholder="$t('profile.edit.profileNamePlaceholder')"
                clearable
                maxlength="32"
                show-word-limit
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
          </el-form>
        </el-tab-pane>
        <el-tab-pane :label="$t('profile.edit.pointConfig')" name="pointConfig">
          <point :embedded="'edit'" :profile-id="reactiveData.id"></point>
        </el-tab-pane>
        <el-tab-pane :label="$t('profile.edit.commandConfig')" name="commandConfig">
          <command-list :embedded="'edit'" :profile-id="reactiveData.id"></command-list>
        </el-tab-pane>
        <el-tab-pane :label="$t('profile.edit.eventConfig')" name="eventConfig">
          <event-list :embedded="'edit'" :profile-id="reactiveData.id"></event-list>
        </el-tab-pane>
      </el-tabs>
    </base-card>
  </div>
</template>

<script lang="ts" src="./index.ts" />

<style lang="scss" scoped>
  @use '@/styles/edit-card.scss';

  .config-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 10px;
    margin: 10px 0;
  }

  .config-toolbar__actions {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    justify-content: flex-end;
    gap: 8px;
    margin-left: auto;
  }
</style>
