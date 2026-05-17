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
    <menu-tool :page="reactiveData.page" @add="openAdd" @refresh="refresh" @reset="reset" @search="search" />

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
        <el-table-column :label="t('settings.menu.menuName')" min-width="220" prop="menuName" />
        <el-table-column :label="t('settings.menu.menuCode')" min-width="180" prop="menuCode" show-overflow-tooltip />
        <el-table-column :label="t('settings.menu.menuType')" min-width="100" prop="menuTypeFlag" />
        <el-table-column :label="t('settings.menu.menuLevel')" min-width="90" prop="menuLevel" />
        <el-table-column :label="t('settings.menu.menuIndex')" min-width="80" prop="menuIndex" />
        <el-table-column :label="t('settings.menu.menuUrl')" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.menuExt?.content?.url || '-' }}
          </template>
        </el-table-column>
        <el-table-column :label="t('settings.menu.menuIcon')" width="90">
          <template #default="{ row }">
            <span v-if="row.menuExt?.content?.icon" class="menu-icon-cell">
              <el-icon :size="18">
                <component :is="resolveIcon(row.menuExt.content.icon) || MenuIcon" />
              </el-icon>
              <span class="menu-icon-cell__name">{{ row.menuExt.content.icon }}</span>
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
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
        <el-table-column :formatter="timestampColumn" :label="t('common.createTime')" prop="createTime" width="165" />
        <el-table-column :label="t('common.operation')" fixed="right" width="200">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">{{ t('common.detail') }}</el-button>
            <el-button link type="primary" @click="openEdit(row)">{{ t('common.edit') }}</el-button>
            <el-popconfirm
              :cancel-button-text="t('common.cancel')"
              :confirm-button-text="t('common.confirm')"
              :title="t('settings.menu.confirmDelete')"
              @confirm="remove(row.id)"
            >
              <template #reference>
                <el-button link type="danger">{{ t('common.delete') }}</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="t('settings.menu.empty')" />
        </template>
      </el-table>
    </blank-card>

    <menu-edit-form ref="editRef" :tree-data="reactiveData.listData" @add-thing="onAdd" @update-thing="onUpdate" />
  </div>
</template>

<script lang="ts" src="./index.ts"></script>

<style lang="scss" scoped>
  .settings-table {
    margin-top: 1px;
    border-radius: 4px;
  }

  .menu-icon-cell {
    display: inline-flex;
    align-items: center;
    gap: 6px;

    .menu-icon-cell__name {
      font-size: 12px;
      color: #606266;
    }
  }
</style>
