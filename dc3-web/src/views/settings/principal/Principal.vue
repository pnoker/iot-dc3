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
            <el-option v-for="opt in principalTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value"/>
          </el-select>
        </el-form-item>
        <el-form-item :label="t('settings.principal.principalName')" prop="principalName">
          <el-input v-model="filterForm.principalName" class="edit-form-default" clearable/>
        </el-form-item>
        <el-form-item :label="$t('common.enableFlag')" prop="enableFlag">
          <enable-flag-segmented v-model="filterForm.enableFlag" include-all/>
        </el-form-item>
      </template>
    </tool-card>

    <blank-card>
      <el-table v-loading="reactiveData.loading" :data="reactiveData.listData" class="settings-table" stripe>
        <el-table-column :label="t('settings.principal.principalName')" min-width="200" prop="principalName"/>
        <el-table-column :label="t('settings.principal.displayName')" min-width="160" prop="displayName"/>
        <el-table-column :label="t('settings.principal.principalType')" min-width="150" prop="principalType"/>
        <el-table-column :label="t('settings.principal.sourceType')" min-width="130" prop="sourceType"/>
        <!-- @vue-generic {import('@/config/types').PrincipalRecord} -->
        <el-table-column :label="t('common.enable')" width="90">
          <template #default="{row}">
            <el-switch :model-value="isEnabledFlag(row.enableFlag)" @change="() => toggleEnable(row)"/>
          </template>
        </el-table-column>
        <el-table-column
          :formatter="timestampColumn"
          :label="t('settings.principal.lastLoginTime')"
          prop="lastLoginTime"
          width="165"
        />
        <el-table-column :formatter="timestampColumn" :label="t('common.createTime')" prop="createTime" width="165"/>
        <template #empty>
          <el-empty :description="t('settings.principal.empty')"/>
        </template>
      </el-table>
    </blank-card>
  </div>
</template>

<script lang="ts" src="./index.ts"></script>
