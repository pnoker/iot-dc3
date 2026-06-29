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
            <el-switch :model-value="isEnabledFlag(row.enableFlag)" @change="() => toggleEnable(row)" />
          </template>
        </el-table-column>
        <el-table-column :formatter="timestampColumn" :label="t('common.createTime')" prop="createTime" width="165" />
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
          <el-empty :description="t('settings.serviceAccount.empty')" />
        </template>
      </el-table>
    </blank-card>

    <service-account-edit-form ref="editRef" @add-thing="onAdd" @update-thing="onUpdate" />
  </div>
</template>

<script lang="ts" src="./index.ts"></script>
