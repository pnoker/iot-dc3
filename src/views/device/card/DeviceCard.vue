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
  <div class="things-card">
    <el-card shadow="hover">
      <div class="things-card-content">
        <div
          :class="{
            'header-enable': 'ENABLE' === data.enableFlag,
            'header-disable': 'ENABLE' !== data.enableFlag,
          }"
          class="things-card__header"
        >
          <div class="things-card-header-icon">
            <img :alt="data.deviceName" :src="icon" />
          </div>
          <div class="things-card-header-name nowrap-name" @click="copyId(data.id, 'Device ID')">
            {{ data.deviceName }}
          </div>
          <div class="things-card-header-status" :title="$t('common.enableFlag')">
            <el-tag v-if="status === 'ONLINE'" effect="plain" type="success">{{ $t('status.online') }}</el-tag>
            <el-tag v-else-if="status === 'MAINTAIN'" effect="plain" type="warning">{{ $t('status.maintain') }}</el-tag>
            <el-tag v-else-if="status === 'FAULT'" effect="plain" type="danger">{{ $t('status.fault') }}</el-tag>
            <el-tag v-else-if="status === 'DISABLE'" effect="plain" type="info">{{ $t('status.disable') }}</el-tag>
            <el-tag v-else effect="plain" type="info">{{ $t('status.offline') }}</el-tag>
          </div>
        </div>
        <div class="things-card__body">
          <div class="things-card-body-content">
            <ul>
              <li class="nowrap-item">
                <el-icon>
                  <Promotion />
                </el-icon>
                {{ $t('device.card.driver') }}: {{ driver.driverName }}
              </li>
              <li class="nowrap-item">
                <el-icon>
                  <Edit />
                </el-icon>
                {{ $t('common.operationTime') }}: {{ timestamp(data.operateTime) }}
              </li>
              <li class="nowrap-item">
                <el-icon>
                  <Sunset />
                </el-icon>
                {{ $t('common.createTime') }}: {{ timestamp(data.createTime) }}
              </li>
            </ul>
          </div>
          <div class="things-card-body-content" :title="$t('device.card.remarkTitle')">
            <p class="nowrap-description">
              {{ data.remark ? data.remark : $t('common.noDescription') }}
            </p>
          </div>
        </div>
        <div v-if="!embedded" class="things-card__footer">
          <div class="things-card-footer-operation">
            <el-popconfirm
              :icon="SwitchButton"
              icon-color="#e6a23c"
              placement="top"
              :title="$t('device.card.confirmDisable')"
              @confirm="disableThing"
            >
              <template #reference>
                <el-button :disabled="'ENABLE' !== data.enableFlag" link type="primary">{{
                  $t('common.disable')
                }}</el-button>
              </template>
            </el-popconfirm>
            <el-popconfirm
              :icon="CircleCheck"
              icon-color="#67c23a"
              placement="top"
              :title="$t('device.card.confirmEnable')"
              @confirm="enableThing"
            >
              <template #reference>
                <el-button :disabled="'ENABLE' === data.enableFlag" link type="primary">{{
                  $t('common.enable')
                }}</el-button>
              </template>
            </el-popconfirm>
            <el-popconfirm
              :icon="CircleClose"
              icon-color="#f56c6c"
              placement="top"
              :title="$t('device.card.confirmDelete')"
              @confirm="deleteThing"
            >
              <template #reference>
                <el-button link type="primary">{{ $t('common.delete') }}</el-button>
              </template>
            </el-popconfirm>
            <el-button link type="primary" @click="edit">{{ $t('common.edit') }}</el-button>
            <el-button link type="primary" @click="detail">{{ $t('common.detail') }}</el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script lang="ts" src="./index.ts" />

<style lang="scss" scoped>
  @use '@/components/card/styles/things-card.scss';
</style>
