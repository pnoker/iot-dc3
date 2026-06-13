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
    <tool-card
      :form-model="filterForm"
      :page="reactiveData.page"
      @refresh="refresh"
      @reset="onReset"
      @search="onSearch"
      @sort="sort"
      @size-change="sizeChange"
      @current-change="currentChange"
    >
      <template #filters>
        <el-form-item :label="t('settings.tenantMembership.principalId')" prop="principalId">
          <el-input v-model="filterForm.principalId" class="edit-form-default" clearable />
        </el-form-item>
        <el-form-item :label="t('settings.tenantMembership.membershipStatus')" prop="membershipStatus">
          <el-select v-model="filterForm.membershipStatus" class="edit-form-default" clearable>
            <el-option v-for="opt in membershipStatusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
      </template>
      <template #actions>
        <el-button :icon="Plus" type="success" @click="openAdd">{{ t('common.add') }}</el-button>
      </template>
    </tool-card>

    <blank-card>
      <el-table v-loading="reactiveData.loading" :data="reactiveData.listData" class="settings-table" stripe>
        <el-table-column :label="t('settings.tenantMembership.principalId')" min-width="140" prop="principalId" />
        <el-table-column :label="t('settings.tenantMembership.principalType')" min-width="150" prop="principalType" />
        <el-table-column
          :label="t('settings.tenantMembership.membershipStatus')"
          min-width="150"
          prop="membershipStatus"
        />
        <el-table-column
          :formatter="timestampColumn"
          :label="t('settings.tenantMembership.joinedTime')"
          prop="joinedTime"
          width="165"
        />
        <el-table-column :formatter="timestampColumn" :label="t('common.createTime')" prop="createTime" width="165" />
        <el-table-column :label="t('common.operation')" fixed="right" width="120">
          <template #default="{ row }">
            <el-popconfirm
              :cancel-button-text="t('common.cancel')"
              :confirm-button-text="t('common.confirm')"
              :title="t('settings.tenantMembership.confirmDelete')"
              @confirm="remove(row.id)"
            >
              <template #reference>
                <el-button link type="danger">{{ t('common.delete') }}</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="t('settings.tenantMembership.empty')" />
        </template>
      </el-table>
    </blank-card>

    <el-dialog v-model="dialog.visible" :title="t('settings.tenantMembership.addTitle')" width="520px">
      <el-form :model="dialog.form" label-position="top">
        <el-form-item :label="t('settings.tenantMembership.principalId')">
          <el-input v-model="dialog.form.principalId" />
        </el-form-item>
        <el-form-item :label="t('settings.tenantMembership.principalType')">
          <el-segmented v-model="dialog.form.principalType" :options="principalTypeOptions" />
        </el-form-item>
        <el-form-item :label="t('settings.tenantMembership.membershipStatus')">
          <el-select v-model="dialog.form.membershipStatus" style="width: 100%">
            <el-option v-for="opt in membershipStatusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">{{ t('common.cancel') }}</el-button>
        <el-button :loading="dialog.submitting" type="primary" @click="submit">
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" src="./index.ts"></script>
