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
    <group-tool
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
      <el-table v-loading="reactiveData.loading" :data="reactiveData.listData" class="settings-table" stripe>
        <el-table-column :label="t('settings.group.groupName')" min-width="160" prop="groupName" />
        <el-table-column
          :label="t('settings.group.groupCode')"
          min-width="160"
          prop="groupCode"
          show-overflow-tooltip
        />
        <el-table-column :label="t('settings.common.entityType')" prop="groupTypeFlag" width="120" />
        <el-table-column :label="t('settings.group.parentGroupId')" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            {{ parentName(row.parentGroupId) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('settings.group.groupLevel')" prop="groupLevel" width="90" />
        <el-table-column :label="t('settings.group.groupIndex')" prop="groupIndex" width="90" />
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
        <el-table-column :label="t('common.remark')" min-width="160" prop="remark" show-overflow-tooltip />
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
        <el-table-column :label="t('common.operation')" fixed="right" width="180">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">{{ t('common.edit') }}</el-button>
            <el-popconfirm
              :cancel-button-text="t('common.cancel')"
              :confirm-button-text="t('common.confirm')"
              :title="t('settings.group.confirmDelete')"
              @confirm="remove(row.id)"
            >
              <template #reference>
                <el-button link type="danger">{{ t('common.delete') }}</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="t('settings.group.empty')" />
        </template>
      </el-table>
    </blank-card>

    <group-edit-form ref="editRef" :tree-data="reactiveData.groupOptions" @add-thing="onAdd" @update-thing="onUpdate" />
  </div>
</template>

<script lang="ts" src="./index.ts"></script>

<style lang="scss" scoped>
  .settings-table {
    margin-top: 1px;
    border-radius: 4px;
  }
</style>
