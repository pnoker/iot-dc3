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
        <device-tool
            :embedded="embedded"
            :page="reactiveData.page"
            @refresh="refresh"
            @reset="reset"
            @search="search"
            @sort="sort"
            @show-add="showAdd"
            @show-import="showImport"
            @size-change="sizeChange"
            @current-change="currentChange"
        />

        <blank-card>
            <el-row>
                <skeleton-card :footer="true" :loading="reactiveData.loading">
                    <el-col v-if="hasData">
                        <el-empty description="暂无设备数据!" />
                    </el-col>
                    <el-col v-for="data in reactiveData.listData" :key="data.id" :lg="6" :md="8" :sm="12" :xl="4" :xs="24">
                        <device-card
                            :data="data"
                            :driver="reactiveData.driverTable[data.driverId]"
                            :embedded="embedded != ''"
                            :status="reactiveData.statusTable[data.id]"
                            @disable-thing="disableThing"
                            @enable-thing="enableThing"
                            @delete-thing="deleteThing"
                        />
                    </el-col>
                </skeleton-card>
            </el-row>
            <!--            <el-row>
                            <el-col v-for="data in 12" :key="data" :lg="6" :md="8" :sm="12" :xl="4" :xs="24">
                                <skeleton-card :footer="true" :loading="reactiveData.loading" />
                            </el-col>
                            <el-col v-if="hasData">
                                <el-empty description="暂无设备数据!" />
                            </el-col>
                            <el-col v-for="data in reactiveData.listData" :key="data.id" :lg="6" :md="8" :sm="12" :xl="4" :xs="24">
                                <device-card
                                    :data="data"
                                    :driver="reactiveData.driverTable[data.driverId]"
                                    :embedded="embedded != ''"
                                    :status="reactiveData.statusTable[data.id]"
                                    @disable-thing="disableThing"
                                    @enable-thing="enableThing"
                                    @delete-thing="deleteThing"
                                />
                            </el-col>
                        </el-row>-->
        </blank-card>

        <device-add-form ref="deviceAddFormRef" @add-thing="addThing" />
        <device-import-form ref="deviceImportFormRef" @import-template="importTemplate" @import-thing="importThing" />
    </div>
</template>

<script lang="ts" src="./index.ts" />

<style lang="scss" scoped></style>
