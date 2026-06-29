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
      <el-tabs :model-value="String(reactiveData.active ?? 'detail')" @tab-click="changeActive">
        <el-tab-pane :label="$t('profile.detail.profileInfo')" name="detail">
          <detail-card>
            <el-descriptions :column="2" border>
              <el-descriptions-item :label="$t('profile.detail.profileName')"
                >{{ reactiveData.data.profileName }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('profile.detail.pointCount')"
                >{{ pointLength }} {{ $t('common.count', {count: ''}) }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('profile.detail.commandCount')"
                >{{ commandLength }} {{ $t('common.count', {count: ''}) }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('profile.detail.eventCount')"
                >{{ eventLength }} {{ $t('common.count', {count: ''}) }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('profile.detail.deviceCount')"
                >{{ deviceLength }} {{ $t('common.count', {count: ''}) }}
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
        <el-tab-pane :label="$t('profile.detail.relatedPoints')" name="point">
          <point ref="pointViewRef" :embedded="'profile'" :profile-id="reactiveData.id"></point>
        </el-tab-pane>
        <el-tab-pane :label="$t('profile.detail.relatedCommands')" name="command">
          <command-list ref="commandViewRef" :embedded="'profile'" :profile-id="reactiveData.id"></command-list>
        </el-tab-pane>
        <el-tab-pane :label="$t('profile.detail.relatedEvents')" name="event">
          <event-list ref="eventViewRef" :embedded="'profile'" :profile-id="reactiveData.id"></event-list>
        </el-tab-pane>
        <el-tab-pane :label="$t('profile.detail.relatedDevices')" name="device">
          <device ref="deviceViewRef" :embedded="'profile'" :profile-id="reactiveData.id"></device>
        </el-tab-pane>
      </el-tabs>
    </base-card>
  </div>
</template>

<script lang="ts" src="./index.ts" />
