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
      @add="openAdd"
      @refresh="refresh"
      @reset="reset"
      @search="search"
      @sort="sort"
      @size-change="sizeChange"
      @current-change="currentChange"
    />

    <blank-card>
      <el-table
        v-loading="reactiveData.loading"
        :data="reactiveData.listData"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        class="settings-table"
        default-expand-all
        row-key="id"
        stripe
      >
        <el-table-column :label="t('settings.resource.resourceName')" min-width="240" prop="resourceName" />
        <el-table-column
          :label="t('settings.resource.resourceCode')"
          min-width="200"
          prop="resourceCode"
          show-overflow-tooltip
        />
        <el-table-column :label="t('settings.resource.resourceType')" min-width="120" prop="resourceTypeFlag" />
        <el-table-column :label="t('settings.resource.resourceScope')" min-width="100" prop="resourceScopeFlag" />
        <el-table-column :label="t('settings.resource.entity')" min-width="160">
          <template #default="{ row }">
            <el-button v-if="isEntityLinkable(row)" link type="primary" @click="goEntityDetail(row)">
              {{ formatEntityId(row) }}
            </el-button>
            <span v-else>{{ formatEntityId(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.remark')" min-width="140" prop="remark" show-overflow-tooltip />
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
        <el-table-column :label="t('common.creatorName')" min-width="110" prop="creatorName" show-overflow-tooltip>
          <template #default="{ row }">{{ row.creatorName || '-' }}</template>
        </el-table-column>
        <el-table-column :formatter="timestampColumn" :label="t('common.createTime')" prop="createTime" width="180" />
        <el-table-column :label="t('common.operatorName')" min-width="110" prop="operatorName" show-overflow-tooltip>
          <template #default="{ row }">{{ row.operatorName || '-' }}</template>
        </el-table-column>
        <el-table-column
          :formatter="timestampColumn"
          :label="t('common.operationTime')"
          prop="operateTime"
          width="180"
        />
        <el-table-column :label="t('common.operation')" fixed="right" width="240">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">{{ t('common.detail') }}</el-button>
            <el-button :disabled="isGroupingNode(row)" link type="primary" @click="openEdit(row)">
              {{ t('common.edit') }}
            </el-button>
            <el-popconfirm
              v-if="!isGroupingNode(row)"
              :cancel-button-text="t('common.cancel')"
              :confirm-button-text="t('common.confirm')"
              :title="t('settings.resource.confirmDelete')"
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
