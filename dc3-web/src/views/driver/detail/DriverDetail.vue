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

<script lang="ts" src="./index.ts" />

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
