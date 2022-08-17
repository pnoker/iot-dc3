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
        <device-tool
            :embedded="embedded"
            :page="reactiveData.page"
            @search="search"
            @reset="reset"
            @show-add="showAdd"
            @refresh="refresh"
            @sort="sort"
            @size-change="sizeChange"
            @current-change="currentChange"
        />

        <blank-card :embedded="embedded != ''">
            <el-row>
                <el-col v-for="data in 12" :key="data" :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
                    <skeleton-card :loading="reactiveData.loading" />
                </el-col>
                <el-col v-if="hasData">
                    <el-empty description="暂无设备数据！" />
                </el-col>
                <el-col v-for="data in reactiveData.listData" :key="data.id" :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
                    <device-card
                        :embedded="embedded != ''"
                        :data="data"
                        :driver="reactiveData.driverTable[data.driverId]"
                        :status-table="reactiveData.statusTable"
                        @delete-thing="deleteThing"
                    />
                </el-col>
            </el-row>
        </blank-card>

        <device-add-form ref="deviceAddFormRef" @add-thing="addThing" />
    </div>
</template>

<script src="./index.ts" lang="ts" />

<style lang="less"></style>
