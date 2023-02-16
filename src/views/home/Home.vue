<!--
  - Copyright 2022 Pnoker All Rights Reserved
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
        <title-card class="home-app" title="导航">
            <div class="app-item">
                <application-card v-for="i in 12" :key="i" :name="'应用 ' + i"></application-card>
            </div>
        </title-card>
        <title-card class="home-dashboard" title="看板">
            <div class="dashboard-item">
                <dashboard-card v-for="i in 6" :key="i" :name="'数据看板 ' + i"></dashboard-card>
            </div>
        </title-card>
        <title-card class="home-count" title="统计">
            <div id="count-data" ref="countDataChartRef"></div>
        </title-card>
    </div>
</template>

<script src="./index.ts" lang="ts" />

<style lang="scss">
@import '@/views/home/style.scss';
</style>
