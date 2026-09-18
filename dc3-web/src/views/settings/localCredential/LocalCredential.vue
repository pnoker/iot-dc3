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

    <responsive-record-list
      :columns="columns"
      :empty-text="t('settings.localCredential.empty')"
      :loading="reactiveData.loading"
      :rows="reactiveData.listData"
      :status="reactiveData.status"
      operation-width="180"
      @retry="refresh"
    >
      <template #actions="{row}">
        <el-button :disabled="isDeleting(row)" link type="primary" @click="openReset(row)">
          {{ t('settings.localCredential.resetPassword') }}
        </el-button>
        <el-popconfirm
          :cancel-button-text="t('common.cancel')"
          :confirm-button-text="t('common.confirm')"
          :disabled="isDeleting(row)"
          :title="t('common.confirmDelete', {name: t('common.entityCredential')})"
          @confirm="remove(row.id)"
        >
          <template #reference>
            <el-button :disabled="isDeleting(row)" :loading="isDeleting(row)" link type="danger">
              {{ t('common.delete') }}
            </el-button>
          </template>
        </el-popconfirm>
      </template>
    </responsive-record-list>

    <el-dialog
      v-model="addDialog.visible"
      :aria-busy="addDialog.submitting || addDialog.optionsLoading"
      :before-close="requestCloseAdd"
      :close-on-click-modal="false"
      :close-on-press-escape="!addDialog.submitting"
      :show-close="!addDialog.submitting"
      :title="t('settings.localCredential.addTitle')"
      class="things-dialog"
      destroy-on-close
      width="520px"
    >
      <el-alert
        v-if="addDialog.optionsError"
        :closable="false"
        :title="t('common.optionLoadFailed')"
        class="credential-dialog__alert"
        show-icon
        type="error"
      >
        <el-button
          :disabled="addDialog.submitting"
          :loading="addDialog.optionsLoading"
          link
          type="danger"
          @click="loadPrincipalOptions(true)"
        >
          {{ t('common.retry') }}
        </el-button>
      </el-alert>
      <el-alert
        v-if="addDialog.saveError"
        :closable="false"
        :title="t('common.saveFailed')"
        class="credential-dialog__alert"
        show-icon
        type="error"
      />
      <el-form ref="addFormRef" :model="addDialog.form" :rules="addRules" label-position="top">
        <el-form-item :label="t('settings.localCredential.loginName')" prop="loginName">
          <el-input v-model="addDialog.form.loginName" :disabled="addDialog.submitting" clearable/>
        </el-form-item>
        <el-form-item :label="t('settings.localCredential.principalId')" prop="principalId">
          <el-select
            v-model="addDialog.form.principalId"
            :disabled="addDialog.submitting"
            :loading="addDialog.optionsLoading"
            filterable
            style="width: 100%"
          >
            <el-option v-for="opt in principalOptions" :key="opt.value" :label="opt.label" :value="opt.value"/>
          </el-select>
        </el-form-item>
        <el-form-item :label="t('settings.localCredential.password')" prop="password">
          <el-input
            v-model="addDialog.form.password"
            :disabled="addDialog.submitting"
            autocomplete="new-password"
            show-password
            type="password"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="addDialog.submitting" @click="requestCloseAdd()">{{ t('common.cancel') }}</el-button>
        <el-button :loading="addDialog.submitting" type="primary" @click="submitAdd">
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="resetDialog.visible"
      :aria-busy="resetDialog.submitting"
      :before-close="requestCloseReset"
      :close-on-click-modal="false"
      :close-on-press-escape="!resetDialog.submitting"
      :show-close="!resetDialog.submitting"
      :title="t('settings.localCredential.resetTitle')"
      class="things-dialog"
      destroy-on-close
      width="480px"
    >
      <el-alert
        v-if="resetDialog.saveError"
        :closable="false"
        :title="t('common.saveFailed')"
        class="credential-dialog__alert"
        show-icon
        type="error"
      />
      <el-form ref="resetFormRef" :model="resetDialog" :rules="resetRules" label-position="top">
        <el-form-item :label="t('settings.localCredential.loginName')">
          <el-input :model-value="resetDialog.loginName" disabled/>
        </el-form-item>
        <el-form-item :label="t('settings.localCredential.newPassword')" prop="password">
          <el-input
            v-model="resetDialog.password"
            :disabled="resetDialog.submitting"
            autocomplete="new-password"
            show-password
            type="password"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="resetDialog.submitting" @click="requestCloseReset()">{{ t('common.cancel') }}</el-button>
        <el-button :loading="resetDialog.submitting" type="primary" @click="submitReset">
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" src="./index.ts"></script>

<style lang="scss" scoped>
.credential-dialog__alert {
  margin-bottom: var(--dc3-space-3);
}
</style>
