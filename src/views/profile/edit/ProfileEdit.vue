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
          <info-card
            :form-model="reactiveData.profileFormData"
            :rules="formRule"
            @reset="profileReset"
            @save="profileSave"
          >
            <template #fields>
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
              <el-form-item :label="$t('profile.edit.description')" class="info-card-item-full" prop="remark">
                <el-input
                  v-model="reactiveData.profileFormData.remark"
                  :placeholder="$t('profile.edit.descriptionPlaceholder')"
                  clearable
                  maxlength="300"
                  show-word-limit
                  type="textarea"
                />
              </el-form-item>
            </template>
          </info-card>
        </el-tab-pane>
        <el-tab-pane :label="$t('profile.detail.relatedPoints')" name="pointConfig">
          <point :embedded="'edit'" :profile-id="reactiveData.id"></point>
        </el-tab-pane>
        <el-tab-pane :label="$t('profile.detail.relatedCommands')" name="commandConfig">
          <command-list :embedded="'edit'" :profile-id="reactiveData.id"></command-list>
        </el-tab-pane>
        <el-tab-pane :label="$t('profile.detail.relatedEvents')" name="eventConfig">
          <event-list :embedded="'edit'" :profile-id="reactiveData.id"></event-list>
        </el-tab-pane>
      </el-tabs>
    </base-card>
  </div>
</template>

<script lang="ts" src="./index.ts" />

<style lang="scss" scoped>
  @use '@/styles/edit-card.scss';
</style>
