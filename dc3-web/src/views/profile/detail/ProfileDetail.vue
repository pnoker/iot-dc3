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
        <el-button :loading="reactiveData.loading" link type="danger" @click="profile">
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
        v-loading="reactiveData.loading"
        :model-value="String(reactiveData.active ?? 'detail')"
        @tab-click="changeActive"
      >
        <el-tab-pane :label="$t('profile.detail.profileInfo')" name="detail">
          <detail-card>
            <el-descriptions :column="isMobile ? 1 : 2" border>
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
              >{{ timestamp(reactiveData.data.operateTime || '') }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.createTime')"
              >{{ timestamp(reactiveData.data.createTime || '') }}
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
