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
    <service-account-tool
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
        <el-table-column
          :label="t('settings.serviceAccount.serviceAccountName')"
          min-width="160"
          prop="serviceAccountName"
        />
        <el-table-column
          :label="t('settings.serviceAccount.purpose')"
          min-width="180"
          prop="purpose"
          show-overflow-tooltip
        />
        <!-- @vue-generic {import('@/config/types/auth').ServiceAccountRecord} -->
        <el-table-column :label="t('settings.serviceAccount.ownerPrincipalId')" min-width="140">
          <template #default="{row}">{{ ownerNameFor(row) }}</template>
        </el-table-column>
        <el-table-column
          :formatter="timestampColumn"
          :label="t('settings.serviceAccount.expireTime')"
          prop="expireTime"
          width="165"
        />
        <el-table-column
          :formatter="timestampColumn"
          :label="t('settings.serviceAccount.lastUsedTime')"
          prop="lastUsedTime"
          width="165"
        />
        <!-- @vue-generic {import('@/config/types/auth').ServiceAccountRecord} -->
        <el-table-column :label="t('common.enable')" width="90">
          <template #default="{row}">
            <el-switch :model-value="isEnabledFlag(row.enableFlag)" @change="() => toggleEnable(row)"/>
          </template>
        </el-table-column>
        <el-table-column :formatter="timestampColumn" :label="t('common.createTime')" prop="createTime" width="165"/>
        <!-- @vue-generic {import('@/config/types/auth').ServiceAccountRecord} -->
        <el-table-column :label="t('common.operation')" fixed="right" width="170">
          <template #default="{row}">
            <el-button link type="primary" @click="openEdit(row)">{{ t('common.edit') }}</el-button>
            <el-popconfirm
              :cancel-button-text="t('common.cancel')"
              :confirm-button-text="t('common.confirm')"
              :title="t('settings.serviceAccount.confirmDelete')"
              @confirm="remove(row.id)"
            >
              <template #reference>
                <el-button link type="danger">{{ t('common.delete') }}</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="t('settings.serviceAccount.empty')"/>
        </template>
      </el-table>
    </blank-card>

    <service-account-edit-form ref="editRef" @add-thing="onAdd" @update-thing="onUpdate"/>
  </div>
</template>

<script lang="ts" src="./index.ts"></script>
