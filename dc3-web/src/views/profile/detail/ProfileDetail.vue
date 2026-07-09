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

<script lang="ts" src="./index.ts"/>
