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
            <img :alt="data.pointName" :src="icon" />
          </div>
          <div class="things-card-header-name nowrap-name" @click="copyId(data.id, $t('point.card.profile'))">
            {{ data.pointName }}
          </div>
          <div class="things-card-header-status" :title="$t('common.name')"></div>
        </div>
        <div class="things-card__body">
          <div class="things-card-body-content">
            <ul class="things-body-content-item-column-2">
              <li class="nowrap-item">
                <span
                  ><el-icon><List /></el-icon> {{ $t('point.card.profile') }}: </span
                >{{ profile.profileName }}
              </li>
              <li class="nowrap-item">
                <span
                  ><el-icon><Location /></el-icon> {{ $t('point.card.ratio') }}: </span
                >{{ data.multiple }}
              </li>
              <li class="nowrap-item">
                <span
                  ><el-icon><Location /></el-icon> {{ $t('point.card.accuracy') }}: </span
                >{{ data.valueDecimal }}
              </li>
              <li class="nowrap-item">
                <span
                  ><el-icon><Location /></el-icon> {{ $t('point.card.unit') }}: </span
                >{{ data.unit }}
              </li>
              <li class="nowrap-item">
                <span
                  ><el-icon><Edit /></el-icon> {{ $t('common.operationTime') }}: </span
                >{{ timestamp(data.operateTime) }}
              </li>
            </ul>
            <ul class="things-body-content-item-column-2">
              <li class="nowrap-item">
                <span
                  ><el-icon><Location /></el-icon> {{ $t('point.card.dataType') }}: </span
                >{{ pointTypeFlag }}
              </li>
              <li class="nowrap-item">
                <span
                  ><el-icon><Location /></el-icon> {{ $t('point.card.baseValue') }}: </span
                >{{ data.baseValue }}
              </li>
              <li class="nowrap-item">
                <span
                  ><el-icon><Location /></el-icon> {{ $t('pointValue.card.processedValue') }}: </span
                >Y = {{ data.multiple }}X + {{ data.baseValue }}, {{ data.valueDecimal }}
              </li>
              <li class="nowrap-item">
                <span
                  ><el-icon><Location /></el-icon> {{ $t('point.card.rw') }}: </span
                >{{ rwFlag }}
              </li>
              <li class="nowrap-item">
                <span
                  ><el-icon><Sunset /></el-icon> {{ $t('common.createTime') }}: </span
                >{{ timestamp(data.createTime) }}
              </li>
            </ul>
          </div>
          <div class="things-card-body-content" :title="$t('point.add.description')">
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
              :title="$t('point.card.confirmDisable')"
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
              :title="$t('point.card.confirmEnable')"
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
              :title="$t('point.card.confirmDelete')"
              @confirm="deleteThing"
            >
              <template #reference>
                <el-button link type="primary">{{ $t('common.delete') }}</el-button>
              </template>
            </el-popconfirm>
            <el-button link type="primary" @click="edit">{{ $t('common.edit') }}</el-button>
            <el-button disabled link type="primary" @click="detail">{{ $t('common.detail') }}</el-button>
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
