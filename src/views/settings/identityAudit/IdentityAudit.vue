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
    <blank-card>
      <div class="identity-audit-filter">
        <el-input
          v-model="reactiveData.filter.principalId"
          :placeholder="t('settings.identityAudit.principalId')"
          clearable
        />
        <el-select v-model="reactiveData.filter.action" :placeholder="t('settings.identityAudit.action')" clearable>
          <el-option v-for="opt in actionOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
        <el-input
          v-model="reactiveData.filter.resourceType"
          :placeholder="t('settings.identityAudit.resourceType')"
          clearable
        />
        <el-select v-model="reactiveData.filter.status" :placeholder="t('settings.identityAudit.status')" clearable>
          <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
        <el-button :icon="Search" type="primary" @click="load">{{ t('common.search') }}</el-button>
        <el-button :icon="Refresh" @click="reset">{{ t('common.reset') }}</el-button>
      </div>
      <el-table v-loading="reactiveData.loading" :data="reactiveData.list" stripe>
        <el-table-column :label="t('settings.identityAudit.createTime')" min-width="165">
          <template #default="{ row }">{{ timestampLabel(row.createTime) }}</template>
        </el-table-column>
        <el-table-column :label="t('settings.identityAudit.principalId')" min-width="120" prop="principalId" />
        <el-table-column :label="t('settings.identityAudit.principalType')" min-width="130" prop="principalType" />
        <el-table-column :label="t('settings.identityAudit.action')" min-width="110" prop="action" />
        <el-table-column :label="t('settings.identityAudit.resourceType')" min-width="150" prop="resourceType" />
        <el-table-column :label="t('settings.identityAudit.resourceId')" min-width="120" prop="resourceId" />
        <el-table-column
          :label="t('settings.identityAudit.resourceName')"
          min-width="150"
          prop="resourceName"
          show-overflow-tooltip
        />
        <el-table-column :label="t('settings.identityAudit.status')" min-width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('settings.identityAudit.errorCode')" min-width="120" prop="errorCode" />
        <template #empty>
          <el-empty :description="t('settings.identityAudit.empty')" />
        </template>
      </el-table>
    </blank-card>
  </div>
</template>

<script lang="ts" src="./index.ts"></script>

<style lang="scss" scoped>
  .identity-audit-filter {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 10px;
    margin-bottom: 12px;
  }

  .identity-audit-filter .el-input {
    max-width: 200px;
  }

  .identity-audit-filter .el-select {
    width: 150px;
  }
</style>
