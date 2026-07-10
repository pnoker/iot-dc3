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
        <el-form-item :label="t('settings.localCredential.loginName')" prop="loginName">
          <el-input v-model="filterForm.loginName" class="edit-form-default" clearable/>
        </el-form-item>
      </template>
      <template #actions>
        <el-button :icon="Plus" type="success" @click="openAdd">{{ t('common.add') }}</el-button>
      </template>
    </tool-card>

    <blank-card>
      <el-table v-loading="reactiveData.loading" :data="reactiveData.listData" class="settings-table" stripe>
        <el-table-column :label="t('settings.localCredential.loginName')" min-width="160" prop="loginName"/>
        <!-- @vue-generic {import('@/config/types').LocalCredentialRecord} -->
        <el-table-column :label="t('settings.localCredential.principalId')" min-width="140">
          <template #default="{row}">{{ principalNameFor(row) }}</template>
        </el-table-column>
        <el-table-column :label="t('settings.localCredential.credentialType')" min-width="130" prop="credentialType"/>
        <el-table-column :label="t('common.enable')" width="90">
          <template #default="{row}">
            <enable-tag :value="row.enableFlag"/>
          </template>
        </el-table-column>
        <el-table-column
          :formatter="timestampColumn"
          :label="t('settings.localCredential.passwordUpdatedTime')"
          prop="passwordUpdatedTime"
          width="165"
        />
        <el-table-column :label="t('settings.localCredential.failedAttempts')" min-width="120" prop="failedAttempts"/>
        <!-- @vue-generic {import('@/config/types').LocalCredentialRecord} -->
        <el-table-column :label="t('common.operation')" fixed="right" width="180">
          <template #default="{row}">
            <el-button link type="primary" @click="openReset(row)">
              {{ t('settings.localCredential.resetPassword') }}
            </el-button>
            <el-popconfirm
              :cancel-button-text="t('common.cancel')"
              :confirm-button-text="t('common.confirm')"
              :title="t('settings.localCredential.confirmDelete')"
              @confirm="remove(row.id)"
            >
              <template #reference>
                <el-button link type="danger">{{ t('common.delete') }}</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="t('settings.localCredential.empty')"/>
        </template>
      </el-table>
    </blank-card>

    <el-dialog v-model="addDialog.visible" :title="t('settings.localCredential.addTitle')" width="520px">
      <el-form :model="addDialog.form" label-position="top">
        <el-form-item :label="t('settings.localCredential.loginName')">
          <el-input v-model="addDialog.form.loginName"/>
        </el-form-item>
        <el-form-item :label="t('settings.localCredential.principalId')">
          <el-select v-model="addDialog.form.principalId" filterable style="width: 100%">
            <el-option v-for="opt in principalOptions" :key="opt.value" :label="opt.label" :value="opt.value"/>
          </el-select>
        </el-form-item>
        <el-form-item :label="t('settings.localCredential.password')">
          <el-input v-model="addDialog.form.password" show-password type="password"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialog.visible = false">{{ t('common.cancel') }}</el-button>
        <el-button :loading="addDialog.submitting" type="primary" @click="submitAdd">
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetDialog.visible" :title="t('settings.localCredential.resetTitle')" width="480px">
      <el-form label-position="top">
        <el-form-item :label="t('settings.localCredential.loginName')">
          <el-input :model-value="resetDialog.loginName" disabled/>
        </el-form-item>
        <el-form-item :label="t('settings.localCredential.newPassword')">
          <el-input v-model="resetDialog.password" show-password type="password"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetDialog.visible = false">{{ t('common.cancel') }}</el-button>
        <el-button :loading="resetDialog.submitting" type="primary" @click="submitReset">
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" src="./index.ts"></script>
