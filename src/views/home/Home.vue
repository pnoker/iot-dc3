<!--
  - Copyright 2016-present the original author or authors.
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
        <title-card
            class="home-app"
            
        >
            <el-row :gutter="2">
                <el-col :span="2">
                    <div class="statistic-card">
                        <el-statistic :value="statisticData.driverCount">
                            <template #title>
                                <div style="display: inline-flex; align-items: center">
                                    驱动数量
                                </div>
                            </template>
                        </el-statistic>
                    </div>
                </el-col>
                <el-col :span="2">
                    <div class="statistic-card">
                        <el-statistic :value="statisticData.profileCount">
                            <template #title>
                                <div style="display: inline-flex; align-items: center">
                                    模板数量
                                </div>
                            </template>
                        </el-statistic>
                    </div>
                </el-col>
                <el-col :span="2">
                    <div class="statistic-card">
                        <el-statistic :value="statisticData.pointCount">
                            <template #title>
                                <div style="display: inline-flex; align-items: center">
                                    点位数量
                                </div>
                            </template>
                        </el-statistic>
                    </div>
                </el-col>
                <el-col :span="2">
                    <div class="statistic-card">
                        <el-statistic :value="statisticData.deviceCount">
                            <template #title>
                                <div style="display: inline-flex; align-items: center">
                                    设备数量
                                </div>
                            </template>
                        </el-statistic>
                    </div>
                </el-col>
                <el-col :span="2">
                    <div class="statistic-card">
                        <el-statistic :value="statisticData.dataCount">
                            <template #title>
                                <div style="display: inline-flex; align-items: center">
                                    数据数量
                                </div>
                            </template>
                        </el-statistic>
                    </div>
                </el-col>
            </el-row>
        </title-card>
        <title-card
            class="home-dashboard"
            title="全国气象设备地图"
        >
            <div id="container"></div>
        </title-card>
        <title-card
            class="home-count"
            title="统计"
        >
            <div
                id="count-data"
                ref="countDataChartRef"
            ></div>
        </title-card>
    </div>
</template>

<script src="./index.ts" lang="ts" />

<style lang="scss">
@import '@/views/home/style.scss';
</style>
