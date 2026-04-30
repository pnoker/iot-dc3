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
  <div class="tool-card">
    <el-card shadow="never">
      <el-form
        ref="formDataRef"
        :inline="true"
        :model="reactiveData.formData"
        :rules="formRule"
        class="tool-card__body"
      >
        <div class="tool-card-body-form">
          <el-form-item :label="$t('device.tool.deviceName')" prop="deviceName">
            <el-input
              v-model="reactiveData.formData.deviceName"
              class="edit-form-default"
              clearable
              :placeholder="$t('device.tool.deviceNamePlaceholder')"
              @keyup.enter="search"
            />
          </el-form-item>
          <el-form-item v-if="embedded != 'driver'" :label="$t('device.tool.driver')" prop="driverId">
            <el-select
              v-model="reactiveData.formData.driverId"
              class="edit-form-special"
              clearable
              filterable
              remote
              reserve-keyword
              :placeholder="$t('device.tool.driverPlaceholder')"
              :remote-method="driverDictionary"
              :loading="reactiveData.driverLoading"
              @visible-change="driverDictionaryVisible"
            >
              <el-option
                v-for="dictionary in reactiveData.driverDictionary"
                :key="dictionary.value"
                :label="dictionary.label"
                :value="dictionary.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item :label="$t('common.enableFlag')" prop="enableFlag">
            <el-segmented
              v-model="reactiveData.formData.enableFlag"
              :options="[
                { label: $t('common.all'), value: '' },
                { label: $t('common.enable'), value: 'ENABLE' },
                { label: $t('common.disable'), value: 'DISABLE' },
              ]"
            />
          </el-form-item>
        </div>
        <el-form-item class="tool-card-body-button">
          <el-button :icon="Search" type="primary" @click="search">{{ $t('common.search') }}</el-button>
          <el-button :icon="RefreshRight" @click="reset">{{ $t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
      <div class="tool-card__footer">
        <div class="tool-card-footer-button">
          <el-button v-if="embedded == ''" :icon="Plus" type="success" @click="showAdd">{{
            $t('common.add')
          }}</el-button>
          <el-button v-if="embedded == ''" :icon="Upload" type="primary" @click="showImport">{{
            $t('common.import')
          }}</el-button>
        </div>
        <div class="tool-card-footer-page">
          <el-pagination
            :current-page="+page.current"
            :page-size="+page.size"
            :page-sizes="[6, 12, 24, 36, 48, 96]"
            :total="+page.total"
            background
            layout="total, prev, pager, next, sizes"
            @size-change="sizeChange"
            @current-change="currentChange"
          ></el-pagination>
          <el-tooltip class="item" :content="$t('common.refresh')" effect="dark" placement="top">
            <el-button :icon="Refresh" circle @click="refresh"></el-button>
          </el-tooltip>
          <el-tooltip class="item" :content="$t('common.sort')" effect="dark" placement="top">
            <el-button :icon="Sort" circle @click="sort"></el-button>
          </el-tooltip>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script lang="ts" src="./index.ts" />

<style lang="scss" scoped>
  @use '@/components/card/styles/tool-card';
</style>
