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
    <base-card>
      <el-alert
        v-if="reactiveData.status === 'error'"
        :closable="false"
        :title="$t('common.loadFailed')"
        class="detail-page-alert"
        show-icon
        type="error"
      >
        <el-button :loading="reactiveData.loading" link type="danger" @click="reload">
          {{ $t('common.retry') }}
        </el-button>
      </el-alert>
      <el-empty
        v-if="reactiveData.status === 'error' && !reactiveData.data.id"
        :description="$t('common.loadFailed')"
      />
      <el-skeleton v-else-if="!reactiveData.data.id" :rows="6" animated />
      <el-tabs
        v-else
        v-model="reactiveData.active"
        v-loading="reactiveData.loading"
        @tab-click="changeActive"
      >
        <el-tab-pane :label="$t('point.detail.pointInfo')" name="detail">
          <detail-card>
            <el-descriptions :column="isMobile ? 1 : 2" border>
              <el-descriptions-item :label="$t('point.detail.pointName')"
              >{{ reactiveData.data.pointName }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('point.detail.relatedDeviceCount')">
                {{ reactiveData.listDeviceData.length || 0 }} {{ $t('common.count', {count: ''}) }}:
                {{ deviceName() }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.operationTime')"
              >{{ timestamp(reactiveData.data.operateTime) }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.createTime')"
              >{{ timestamp(reactiveData.data.createTime) }}
              </el-descriptions-item>
            </el-descriptions>
          </detail-card>
        </el-tab-pane>
        <el-tab-pane :label="$t('point.detail.relatedDevices')" name="device">
          <el-alert
            v-if="reactiveData.relationStatus === 'error'"
            :closable="false"
            :title="$t('common.loadFailed')"
            class="detail-page-alert"
            show-icon
            type="error"
          >
            <el-button :loading="reactiveData.relationLoading" link type="danger" @click="device">
              {{ $t('common.retry') }}
            </el-button>
          </el-alert>
          <el-empty
            v-if="reactiveData.relationStatus === 'error' && reactiveData.listDeviceData.length === 0"
            :description="$t('common.loadFailed')"
          />
          <el-empty
            v-else-if="reactiveData.relationStatus === 'success' && reactiveData.listDeviceData.length === 0"
            :description="$t('common.empty')"
          />
          <el-row v-else v-loading="reactiveData.relationLoading" :aria-busy="reactiveData.relationLoading">
            <el-col
              v-for="data in reactiveData.listDeviceData"
              :key="data.id"
              :lg="6"
              :md="8"
              :sm="12"
              :xl="4"
              :xs="24"
            >
              <device-card
                :data="data"
                :driver="reactiveData.driverTable[data.driverId]"
                :embedded="true"
                :status="reactiveData.statusTable[data.id]"
              ></device-card>
            </el-col>
          </el-row>
        </el-tab-pane>
      </el-tabs>
    </base-card>
  </div>
</template>

<script lang="ts" src="./index.ts"/>

<style lang="scss" scoped>
.detail-page-alert {
  margin-bottom: var(--dc3-space-3);

  :deep(.el-alert__content) {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: var(--dc3-space-2);
  }
}
</style>
