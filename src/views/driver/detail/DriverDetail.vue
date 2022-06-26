<!--
  - Copyright (c) 2022. Pnoker. All Rights Reserved.
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -     http://www.apache.org/licenses/LICENSE-2.0
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->

<template>
    <div>
        <driver-tool
            :page="reactiveData.page"
            @search="search"
            @reset="reset"
            @refresh="refresh"
            @sort="sort"
            @size-change="sizeChange"
            @current-change="currentChange"
        ></driver-tool>
        <el-row :gutter="3">
            <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
                <blank-card>
                    <el-scrollbar>
                        <el-row>
                            <el-col :key="data.id" v-for="data in 12">
                                <skeleton-card :loading="reactiveData.driverLoading"></skeleton-card>
                            </el-col>
                            <el-col>
                                <el-empty v-if="reactiveData.listDriverData.length<1" description="暂无驱动数据！"></el-empty>
                            </el-col>
                            <el-col :key="data.id" v-for="data in reactiveData.listDriverData">
                                <driver-card
                                    :data="data"
                                    :statusTable="reactiveData.driverStatusTable"
                                    :footer="true"
                                    @select-change="selectChange"
                                ></driver-card>
                            </el-col>
                        </el-row>
                    </el-scrollbar>
                </blank-card>
            </el-col>
            <el-col :xs="24" :sm="12" :md="16" :lg="18" :xl="20">
                <base-card>
                    <el-tabs v-model="reactiveData.active" @tab-click="changeActive">
                        <el-tab-pane label="驱动详情" name="detail">
                            <detail-card>
                                <ul>
                                    <li><i class="el-icon-data-line"></i> 驱动名称: {{ reactiveData.data.name }}</li>
                                    <li><i class="el-icon-collection-tag"></i> 关联设备 [{{ reactiveData.listDeviceData.length || 0 }} 个]: {{ deviceName() }}</li>
                                    <li class="nowrap-item"><span><i class="el-icon-connection"/> 端口: </span>{{ reactiveData.data.port }}</li>
                                    <li class="nowrap-item"><span><i class="el-icon-monitor"/> 主机: </span>{{ reactiveData.data.host }}</li>
                                    <li class="nowrap-item"><span><i class="el-icon-s-promotion"/> 驱动服务: </span>{{ reactiveData.data.serviceName }}</li>
                                    <li><i class="el-icon-edit-outline"></i> 修改日期: {{ timestamp(reactiveData.data.createTime) }}</li>
                                    <li><i class="el-icon-sunset"></i> 创建日期: {{ timestamp(reactiveData.data.updateTime) }}</li>
                                </ul>
                            </detail-card>
                        </el-tab-pane>
                        <el-tab-pane label="关联设备" name="device">
                            <el-row>
                                <el-col :xs="24" :sm="24" :md="12" :lg="8" :xl="6" :key="data.id" v-for="data in 12">
                                    <skeleton-card :loading="reactiveData.deviceLoading" :footer="true"></skeleton-card>
                                </el-col>
                                <el-col>
                                    <el-empty v-if="reactiveData.listDeviceData.length<1" description="暂无关联的设备数据！"></el-empty>
                                </el-col>
                                <el-col :xs="24" :sm="24" :md="12" :lg="8" :xl="6" :key="data.id" v-for="data in reactiveData.listDeviceData">
                                    <device-card
                                        :data="data"
                                        :driverTable="reactiveData.driverTable"
                                        :profileTable="reactiveData.profileTable"
                                        :statusTable="reactiveData.deviceStatusTable"
                                        :embedded="true"
                                    ></device-card>
                                </el-col>
                            </el-row>
                        </el-tab-pane>
                        <el-tab-pane label="驱动模型" name="model">
                            <el-empty description="暂无驱动模型数据！"></el-empty>
                        </el-tab-pane>
                        <el-tab-pane label="驱动事件" name="event">
                            <el-empty description="暂无驱动事件数据！"></el-empty>
                        </el-tab-pane>
                    </el-tabs>
                </base-card>
            </el-col>
        </el-row>
    </div>
</template>

<script lang="ts">
import DriverDetail from "@/views/driver/detail"

export default DriverDetail
</script>

<style lang="less">
</style>
