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
      <el-tabs v-model="reactiveData.active" @tab-click="changeActive">
        <el-tab-pane :label="$t('driver.detail.driverInfo')" name="detail">
          <detail-card>
            <el-descriptions :column="2" border>
              <el-descriptions-item :label="$t('driver.detail.driverName')"
              >{{ reactiveData.data.driverName }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('driver.detail.relatedDeviceCount')"
              >{{ deviceLength }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('driver.card.host')"
              >{{ reactiveData.data.serviceHost }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('driver.card.driverService')"
              >{{ reactiveData.data.serviceName }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.operationTime')"
              >{{ timestamp(reactiveData.data.createTime) }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.createTime')"
              >{{ timestamp(reactiveData.data.createTime) }}
              </el-descriptions-item>
            </el-descriptions>
          </detail-card>
        </el-tab-pane>
        <el-tab-pane :label="$t('driver.detail.relatedDevices')" name="device">
          <device ref="deviceViewRef" :driver-id="reactiveData.id" :embedded="'driver'"></device>
        </el-tab-pane>
      </el-tabs>
    </base-card>
  </div>
</template>

<script lang="ts" src="./index.ts"/>

<style lang="scss" scoped>
.detail-content {
  margin-left: 0 !important;
  margin-right: 0 !important;

  .detail-content-left {
    .el-scrollbar {
      height: calc(100vh - 273px);
    }
  }
}
</style>
