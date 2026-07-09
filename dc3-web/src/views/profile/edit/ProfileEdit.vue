<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  -
  - This program is free software: you can redistribute it and/or modify
  - it under the terms of the GNU Affero General Public License as
  - published by the Free Software Foundation, either version 3 of the
  - License, or (at your option) any later version.
  -
  - This program is distributed in the hope that it will be useful,
  - but WITHOUT ANY WARRANTY; without even the implied warranty of
  - MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  - GNU Affero General Public License for more details.
  -
  - You should have received a copy of the GNU Affero General Public License
  - along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
                <enable-flag-segmented v-model="reactiveData.profileFormData.enableFlag"/>
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

<script lang="ts" src="./index.ts"/>

<style lang="scss" scoped>
@use '@/styles/edit-card.scss';
</style>
