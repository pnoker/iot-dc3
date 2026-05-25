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
    <api-tool
      :page="reactiveData.page"
      @refresh="refresh"
      @reset="reset"
      @search="search"
      @sort="sort"
      @size-change="sizeChange"
      @current-change="currentChange"
    />

    <blank-card>
      <el-table v-loading="reactiveData.loading" :data="reactiveData.listData" class="settings-table" stripe>
        <el-table-column :label="t('settings.api.apiName')" min-width="160" prop="apiName" />
        <el-table-column :label="t('settings.api.apiCode')" min-width="200" prop="apiCode" show-overflow-tooltip />
        <el-table-column :label="t('settings.api.apiGroup')" min-width="160" prop="apiGroup" />
        <el-table-column :label="t('settings.api.serviceName')" min-width="160" prop="serviceName" />
        <el-table-column :label="t('settings.api.apiType')" min-width="100" prop="apiTypeFlag" />
        <el-table-column :label="t('common.enable')" width="90">
          <template #default="{ row }">
            <enable-tag :value="row.enableFlag" />
          </template>
        </el-table-column>
        <el-table-column :label="t('common.remark')" min-width="140" prop="remark" show-overflow-tooltip />
        <el-table-column :formatter="timestampColumn" :label="t('common.createTime')" prop="createTime" width="180" />
        <el-table-column
          :formatter="timestampColumn"
          :label="t('common.operationTime')"
          prop="operateTime"
          width="180"
        />
        <el-table-column :label="t('common.operation')" fixed="right" width="120">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">{{ t('common.detail') }}</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="t('settings.api.empty')" />
        </template>
      </el-table>
    </blank-card>
  </div>
</template>

<script lang="ts" src="./index.ts"></script>
