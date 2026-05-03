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
    <resource-tool
      :page="reactiveData.page"
      @search="search"
      @reset="reset"
      @refresh="refresh"
      @sort="sort"
      @add="openAdd"
      @size-change="sizeChange"
      @current-change="currentChange"
    />

    <blank-card>
      <el-table
        v-loading="reactiveData.loading"
        :data="reactiveData.listData"
        row-key="id"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        default-expand-all
        stripe
        class="settings-table"
      >
        <el-table-column prop="resourceName" :label="t('settings.resource.resourceName')" min-width="240" />
        <el-table-column
          prop="resourceCode"
          :label="t('settings.resource.resourceCode')"
          min-width="200"
          show-overflow-tooltip
        />
        <el-table-column prop="resourceTypeFlag" :label="t('settings.resource.resourceType')" min-width="120" />
        <el-table-column prop="resourceScopeFlag" :label="t('settings.resource.resourceScope')" min-width="100" />
        <el-table-column :label="t('settings.resource.entity')" min-width="160">
          <template #default="{ row }">
            <el-button v-if="isEntityLinkable(row)" link type="primary" @click="goEntityDetail(row)">
              {{ formatEntityId(row) }}
            </el-button>
            <span v-else>{{ formatEntityId(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" :label="t('common.remark')" min-width="140" show-overflow-tooltip />
        <el-table-column :label="t('common.enable')" width="90">
          <template #default="{ row }">
            <el-tag :type="String(row.enableFlag) === 'ENABLE' || Number(row.enableFlag) === 0 ? 'success' : 'info'">
              {{
                String(row.enableFlag) === 'ENABLE' || Number(row.enableFlag) === 0
                  ? t('common.enable')
                  : t('common.disable')
              }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="creatorName" :label="t('common.creatorName')" min-width="110" show-overflow-tooltip>
          <template #default="{ row }">{{ row.creatorName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" :label="t('common.createTime')" :formatter="timestampColumn" width="180" />
        <el-table-column prop="operatorName" :label="t('common.operatorName')" min-width="110" show-overflow-tooltip>
          <template #default="{ row }">{{ row.operatorName || '-' }}</template>
        </el-table-column>
        <el-table-column
          prop="operateTime"
          :label="t('common.operationTime')"
          :formatter="timestampColumn"
          width="180"
        />
        <el-table-column :label="t('common.operation')" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">{{ t('common.detail') }}</el-button>
            <el-button link type="primary" :disabled="isGroupingNode(row)" @click="openEdit(row)">
              {{ t('common.edit') }}
            </el-button>
            <el-popconfirm
              v-if="!isGroupingNode(row)"
              :title="t('settings.resource.confirmDelete')"
              :confirm-button-text="t('common.confirm')"
              :cancel-button-text="t('common.cancel')"
              @confirm="remove(row.id)"
            >
              <template #reference>
                <el-button link type="danger">{{ t('common.delete') }}</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="t('settings.resource.empty')" />
        </template>
      </el-table>
    </blank-card>

    <resource-edit-form ref="editRef" :tree-data="reactiveData.listData" @add-thing="onAdd" @update-thing="onUpdate" />
  </div>
</template>

<script lang="ts" src="./index.ts"></script>

<style lang="scss" scoped>
  .settings-table {
    margin-top: 1px;
    border-radius: 4px;
  }
</style>
