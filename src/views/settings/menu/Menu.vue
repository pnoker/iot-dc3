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
    <menu-tool :page="reactiveData.page" @search="search" @reset="reset" @refresh="refresh" @add="openAdd" />

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
        <el-table-column prop="menuName" :label="t('settings.menu.menuName')" min-width="240" />
        <el-table-column prop="menuCode" :label="t('settings.menu.menuCode')" min-width="200" show-overflow-tooltip />
        <el-table-column prop="menuTypeFlag" :label="t('settings.menu.menuType')" min-width="100" />
        <el-table-column prop="menuLevel" :label="t('settings.menu.menuLevel')" min-width="90" />
        <el-table-column prop="menuIndex" :label="t('settings.menu.menuIndex')" min-width="80" />
        <el-table-column :label="t('settings.menu.menuUrl')" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.menuExt?.content?.url || '-' }}
          </template>
        </el-table-column>
        <el-table-column :label="t('settings.menu.menuIcon')" width="110">
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
        <el-table-column :label="t('common.operation')" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">{{ t('common.detail') }}</el-button>
            <el-button link type="primary" @click="openEdit(row)">{{ t('common.edit') }}</el-button>
            <el-popconfirm
              :title="t('settings.menu.confirmDelete')"
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
