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
    <user-tool
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
        <el-table-column :label="t('settings.user.nickName')" min-width="120" prop="nickName" />
        <el-table-column :label="t('settings.user.userName')" min-width="140" prop="userName" />
        <el-table-column :label="t('settings.user.phone')" min-width="140" prop="phone" />
        <el-table-column :label="t('settings.user.email')" min-width="180" prop="email" show-overflow-tooltip />
        <el-table-column :label="t('common.enable')" width="90">
          <template #default="{ row }">
            <el-tag :type="Number(row.enableFlag) === 0 ? 'success' : 'info'">
              {{ Number(row.enableFlag) === 0 ? t('common.enable') : t('common.disable') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.createTime')" prop="createTime" :formatter="timestampColumn" width="180" />
        <el-table-column
          :label="t('common.operationTime')"
          prop="operateTime"
          :formatter="timestampColumn"
          width="180"
        />
        <el-table-column :label="t('common.operation')" fixed="right" width="320">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">{{ t('common.detail') }}</el-button>
            <el-button link type="primary" @click="openEdit(row)">{{ t('common.edit') }}</el-button>
            <el-button link type="primary" @click="openAssignRoles(row)">
              {{ t('settings.user.assignRoles') }}
            </el-button>
            <el-popconfirm
              :cancel-button-text="t('common.cancel')"
              :confirm-button-text="t('common.confirm')"
              :title="t('settings.user.confirmDelete')"
              @confirm="remove(row.id)"
            >
              <template #reference>
                <el-button link type="danger">{{ t('common.delete') }}</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="t('settings.user.empty')" />
        </template>
      </el-table>
    </blank-card>

    <user-edit-form ref="editRef" @add-thing="onAdd" @update-thing="onUpdate" />
    <user-assign-roles ref="assignRef" @save="onAssignRoles" />
  </div>
</template>

<script lang="ts" src="./index.ts"></script>

<style lang="scss" scoped>
  .settings-table {
    border-radius: 4px;
  }
</style>
