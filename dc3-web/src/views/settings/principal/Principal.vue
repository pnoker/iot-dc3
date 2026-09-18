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

    <responsive-record-list
      :columns="columns"
      :empty-text="t('settings.principal.empty')"
      :loading="reactiveData.loading"
      :rows="reactiveData.listData"
      :status="reactiveData.status"
      @retry="refresh"
    >
      <template #cell-enableFlag="{row}">
        <el-switch
          :aria-label="`${t('common.enable')}: ${row.principalName || row.id}`"
          :disabled="isToggling(row)"
          :loading="isToggling(row)"
          :model-value="isEnabledFlag(row.enableFlag)"
          @change="() => toggleEnable(row)"
        />
      </template>
    </responsive-record-list>
  </div>
</template>

<script lang="ts" src="./index.ts"></script>
