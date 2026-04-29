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
  <div class="things-card" @click="select">
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
            <img :alt="data.driverName" :src="icon" />
          </div>
          <div class="things-card-header-name nowrap-name" @click="copyId(data.id, 'Driver ID')">
            {{ data.driverName }}
          </div>
          <div class="things-card-header-status" title="Status">
            <el-tag v-if="status === 'ONLINE'" effect="plain" type="success">{{ $t('status.online') }}</el-tag>
            <el-tag v-else-if="status === 'MAINTAIN'" effect="plain" type="warning">{{ $t('status.maintain') }}</el-tag>
            <el-tag v-else-if="status === 'FAULT'" effect="plain" type="danger">{{ $t('status.fault') }}</el-tag>
            <el-tag v-else-if="status === 'DISABLE'" effect="plain" type="info">{{ $t('status.disable') }}</el-tag>
            <el-tag v-else-if="status === 'REGISTERING'" effect="plain" type="info">{{
              $t('status.registering')
            }}</el-tag>
            <el-tag v-else effect="plain" type="info">{{ $t('status.offline') }}</el-tag>
          </div>
        </div>
        <div class="things-card__body">
          <div class="things-card-body-content">
            <ul>
              <li class="nowrap-item">
                <el-icon>
                  <Monitor />
                </el-icon>
                {{ $t('driver.card.host') }}: {{ data.serviceHost }}
              </li>
              <li class="nowrap-item">
                <el-icon>
                  <Promotion />
                </el-icon>
                {{ $t('driver.card.driverService') }}: {{ data.serviceName }}
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
          <div :title="data.remark ? data.remark : $t('driver.card.remarkTitle')" class="things-card-body-content">
            <p class="nowrap-description">
              {{ data.remark ? data.remark : $t('common.noDescription') }}
            </p>
          </div>
        </div>
        <div v-if="!footer" class="things-card__footer">
          <div class="things-card-footer-operation">
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
  @use '@/views/driver/card/style.scss';
</style>
