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
    <blank-card class="home-statistic">
      <el-row :gutter="16">
        <el-col :lg="6" :md="12" :sm="12" :xl="6" :xs="24">
          <el-card class="statistic-card statistic-device" shadow="hover">
            <div class="statistic-card-inner">
              <div class="statistic-icon">
                <el-icon :size="36"><Management /></el-icon>
              </div>
              <el-statistic :title="$t('home.deviceCount')" :value="reactiveData.deviceCount" />
            </div>
          </el-card>
        </el-col>
        <el-col :lg="6" :md="12" :sm="12" :xl="6" :xs="24">
          <el-card class="statistic-card statistic-point" shadow="hover">
            <div class="statistic-card-inner">
              <div class="statistic-icon">
                <el-icon :size="36"><CollectionTag /></el-icon>
              </div>
              <el-statistic :title="$t('home.pointCount')" :value="reactiveData.pointCount" />
            </div>
          </el-card>
        </el-col>
        <el-col :lg="6" :md="12" :sm="12" :xl="6" :xs="24">
          <el-card class="statistic-card statistic-profile" shadow="hover">
            <div class="statistic-card-inner">
              <div class="statistic-icon">
                <el-icon :size="36"><List /></el-icon>
              </div>
              <el-statistic :title="$t('home.profileCount')" :value="reactiveData.profileCount" />
            </div>
          </el-card>
        </el-col>
        <el-col :lg="6" :md="12" :sm="12" :xl="6" :xs="24">
          <el-card class="statistic-card statistic-driver" shadow="hover">
            <div class="statistic-card-inner">
              <div class="statistic-icon">
                <el-icon :size="36"><Promotion /></el-icon>
              </div>
              <el-statistic :title="$t('home.driverCount')" :value="reactiveData.driverCount" />
            </div>
          </el-card>
        </el-col>
      </el-row>
    </blank-card>
    <title-card class="home-count">
      <div id="count-data" ref="countDataChartRef"></div>
    </title-card>
  </div>
</template>

<script lang="ts" src="./index.ts" />

<style lang="scss" scoped>
  @use '@/views/home/style.scss';
</style>
