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
      <el-form ref="formDataRef" :inline="true" :model="reactiveData.formData" class="tool-card__body">
        <div class="tool-card-body-form">
          <el-form-item :label="$t('settings.user.nickName')" prop="nickName">
            <el-input
              v-model="reactiveData.formData.nickName"
              class="edit-form-default"
              clearable
              :placeholder="$t('settings.user.nickNamePlaceholder')"
              @keyup.enter="search"
            />
          </el-form-item>
          <el-form-item :label="$t('settings.user.userName')" prop="userName">
            <el-input
              v-model="reactiveData.formData.userName"
              class="edit-form-default"
              clearable
              :placeholder="$t('settings.user.userNamePlaceholder')"
              @keyup.enter="search"
            />
          </el-form-item>
          <el-form-item :label="$t('settings.user.phone')" prop="phone">
            <el-input
              v-model="reactiveData.formData.phone"
              class="edit-form-default"
              clearable
              :placeholder="$t('settings.user.phonePlaceholder')"
              @keyup.enter="search"
            />
          </el-form-item>
          <el-form-item :label="$t('settings.user.email')" prop="email">
            <el-input
              v-model="reactiveData.formData.email"
              class="edit-form-default"
              clearable
              :placeholder="$t('settings.user.emailPlaceholder')"
              @keyup.enter="search"
            />
          </el-form-item>
          <el-form-item :label="$t('common.enableFlag')" prop="enableFlag">
            <el-segmented
              v-model="reactiveData.formData.enableFlag"
              :options="[
                { label: $t('common.all'), value: '' },
                { label: $t('common.enable'), value: 0 },
                { label: $t('common.disable'), value: 1 },
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
          <el-button :icon="Plus" type="success" @click="add">{{ $t('common.add') }}</el-button>
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
          />
          <el-tooltip class="item" :content="$t('common.refresh')" effect="dark" placement="top">
            <el-button :icon="Refresh" circle @click="refresh" />
          </el-tooltip>
          <el-tooltip class="item" :content="$t('common.sort')" effect="dark" placement="top">
            <el-button :icon="Sort" circle @click="sort" />
          </el-tooltip>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script lang="ts" src="./index.ts" />

<style lang="scss" scoped>
  @use '@/components/card/styles/tool-card.scss';
</style>
