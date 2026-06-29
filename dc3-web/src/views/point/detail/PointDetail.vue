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
        <el-tab-pane :label="$t('point.detail.pointInfo')" name="detail">
          <detail-card>
            <el-descriptions :column="2" border>
              <el-descriptions-item :label="$t('point.detail.pointName')"
                >{{ reactiveData.data.pointName }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('point.detail.relatedDeviceCount')">
                {{ reactiveData.listDeviceData.length || 0 }} {{ $t('common.count', {count: ''}) }}:
                {{ deviceName() }}
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
        <el-tab-pane :label="$t('point.detail.relatedDevices')" name="device">
          <el-row>
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

<script lang="ts" src="./index.ts" />
