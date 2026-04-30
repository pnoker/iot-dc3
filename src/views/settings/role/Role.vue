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
  <div class="settings-page">
    <div class="settings-toolbar">
      <el-form :inline="true" class="settings-search">
        <el-form-item>
          <el-input
            v-model="reactiveData.query.roleName"
            clearable
            :placeholder="t('settings.role.roleName')"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="reactiveData.query.roleCode"
            clearable
            :placeholder="t('settings.role.roleCode')"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">{{ t('common.search') }}</el-button>
          <el-button @click="reset">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
      <div class="settings-actions">
        <el-button type="primary" @click="openAdd">+ {{ t('common.add') }}</el-button>
      </div>
    </div>

    <el-table v-loading="reactiveData.loading" :data="reactiveData.listData" stripe class="settings-table">
      <el-table-column prop="roleName" :label="t('settings.role.roleName')" min-width="160" />
      <el-table-column prop="roleCode" :label="t('settings.role.roleCode')" min-width="160" />
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
      <el-table-column prop="remark" :label="t('common.remark')" min-width="200" show-overflow-tooltip />
      <el-table-column prop="createTime" :label="t('common.createTime')" width="180" />
      <el-table-column :label="t('common.operation')" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">{{ t('common.edit') }}</el-button>
          <el-button link type="primary" @click="openAssignResources(row)">
            {{ t('settings.role.assignResources') }}
          </el-button>
          <el-popconfirm
            :title="t('settings.role.confirmDelete')"
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
        <el-empty :description="t('settings.role.empty')" />
      </template>
    </el-table>

    <el-pagination
      v-model:current-page="reactiveData.page.current"
      v-model:page-size="reactiveData.page.size"
      :total="reactiveData.page.total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      class="settings-pagination"
      @current-change="currentChange"
      @size-change="sizeChange"
    />

    <role-edit-form ref="editRef" @add-thing="onAdd" @update-thing="onUpdate" />
    <role-assign-resources ref="assignRef" @save="onAssignResources" />
  </div>
</template>

<script lang="ts" src="./index.ts"></script>

<style lang="scss" scoped>
  .settings-page {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  .settings-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
  }
  .settings-search :deep(.el-form-item) {
    margin-bottom: 0;
  }
  .settings-table {
    border-radius: 6px;
  }
  .settings-pagination {
    justify-content: flex-end;
  }
</style>
