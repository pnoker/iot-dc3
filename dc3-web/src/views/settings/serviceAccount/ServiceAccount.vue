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

    <responsive-record-list
      :columns="columns"
      :empty-text="t('settings.serviceAccount.empty')"
      :loading="reactiveData.loading"
      :rows="reactiveData.listData"
      :status="reactiveData.status"
      operation-width="170"
      @retry="refresh"
    >
      <template #cell-enableFlag="{row}">
        <el-switch
          :aria-label="`${t('common.enable')}: ${row.serviceAccountName || row.id}`"
          :disabled="isToggling(row)"
          :loading="isToggling(row)"
          :model-value="isEnabledFlag(row.enableFlag)"
          @change="() => toggleEnable(row)"
        />
      </template>
      <template #actions="{row}">
        <el-button :disabled="isRowBusy(row)" link type="primary" @click="openEdit(row)">{{ t('common.edit') }}</el-button>
        <el-popconfirm
          :cancel-button-text="t('common.cancel')"
          :confirm-button-text="t('common.confirm')"
          :disabled="isRowBusy(row)"
          :title="t('common.confirmDelete', {name: t('common.entityServiceAccount')})"
          @confirm="remove(row.id)"
        >
          <template #reference>
            <el-button :disabled="isRowBusy(row)" :loading="isDeleting(row)" link type="danger">
              {{ t('common.delete') }}
            </el-button>
          </template>
        </el-popconfirm>
      </template>
    </responsive-record-list>

    <service-account-edit-form ref="editRef" @add-thing="onAdd" @update-thing="onUpdate"/>
  </div>
</template>

<script lang="ts" src="./index.ts"></script>
