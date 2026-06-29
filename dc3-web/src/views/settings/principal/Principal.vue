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
    <tool-card
      :form-model="filterForm"
      :page="reactiveData.page"
      @refresh="refresh"
      @reset="onReset"
      @search="onSearch"
      @sort="sort"
      @size-change="sizeChange"
      @current-change="currentChange"
    >
      <template #filters>
        <el-form-item :label="t('settings.principal.principalType')" prop="principalType">
          <el-select v-model="filterForm.principalType" class="edit-form-default" clearable>
            <el-option v-for="opt in principalTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('settings.principal.principalName')" prop="principalName">
          <el-input v-model="filterForm.principalName" class="edit-form-default" clearable />
        </el-form-item>
        <el-form-item :label="$t('common.enableFlag')" prop="enableFlag">
          <enable-flag-segmented v-model="filterForm.enableFlag" include-all />
        </el-form-item>
      </template>
    </tool-card>

    <blank-card>
      <el-table v-loading="reactiveData.loading" :data="reactiveData.listData" class="settings-table" stripe>
        <el-table-column :label="t('settings.principal.principalName')" min-width="200" prop="principalName" />
        <el-table-column :label="t('settings.principal.displayName')" min-width="160" prop="displayName" />
        <el-table-column :label="t('settings.principal.principalType')" min-width="150" prop="principalType" />
        <el-table-column :label="t('settings.principal.sourceType')" min-width="130" prop="sourceType" />
        <!-- @vue-generic {import('@/config/types').PrincipalRecord} -->
        <el-table-column :label="t('common.enable')" width="90">
          <template #default="{row}">
            <el-switch :model-value="isEnabledFlag(row.enableFlag)" @change="() => toggleEnable(row)" />
          </template>
        </el-table-column>
        <el-table-column
          :formatter="timestampColumn"
          :label="t('settings.principal.lastLoginTime')"
          prop="lastLoginTime"
          width="165"
        />
        <el-table-column :formatter="timestampColumn" :label="t('common.createTime')" prop="createTime" width="165" />
        <template #empty>
          <el-empty :description="t('settings.principal.empty')" />
        </template>
      </el-table>
    </blank-card>
  </div>
</template>

<script lang="ts" src="./index.ts"></script>
