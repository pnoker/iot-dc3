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
        <point-value-tool
            :embedded="embedded"
            :page="reactiveData.page"
            @search="search"
            @reset="reset"
            @refresh="refresh"
            @size-change="sizeChange"
            @current-change="currentChange"
        ></point-value-tool>

        <blank-card :embedded="embedded != ''">
            <el-row>
                <el-col v-for="data in 12" :key="data" :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
                    <skeleton-card :loading="reactiveData.loading"></skeleton-card>
                </el-col>
                <el-col v-if="hasData">
                    <el-empty :description="embedded == 'device' ? '暂无设备数据' : '暂无数据，请选择设备和位号！'"></el-empty>
                </el-col>
                <el-col v-for="data in reactiveData.listData" :key="data.id" :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
                    <point-value-card
                        :embedded="embedded"
                        :data="data"
                        :device="reactiveData.deviceTable[data.deviceId]"
                        :point="reactiveData.pointTable[data.pointId]"
                        :unit="reactiveData.unitTable[data.pointId]"
                        :history-data="reactiveData.historyData[data.pointId]"
                    ></point-value-card>
                </el-col>
            </el-row>
        </blank-card>
    </div>
</template>

<script src="./index.ts" lang="ts" />

<style lang="scss"></style>
