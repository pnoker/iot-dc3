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
        <base-card>
            <el-tabs v-model="reactiveData.active" @tab-click="changeActive">
                <el-tab-pane label="设备详情" name="detail">
                    <detail-card>
                        <ul>
                            <li>
                                <el-icon>
                                    <Management />
                                </el-icon>
                                设备名称: {{ reactiveData.data.name }}
                            </li>
                            <li>
                                <el-icon>
                                    <List />
                                </el-icon>
                                包含模板: {{ reactiveData.listProfileData.length }} 个
                            </li>
                            <li>
                                <el-icon>
                                    <CollectionTag />
                                </el-icon>
                                包含位号: {{ reactiveData.listPointData.length }} 个
                            </li>
                            <li>
                                <el-icon>
                                    <Promotion />
                                </el-icon>
                                所属驱动: {{ reactiveData.driver.name }}
                            </li>
                            <li>
                                <el-icon>
                                    <Edit />
                                </el-icon>
                                修改日期: {{ timestamp(reactiveData.data.createTime) }}
                            </li>
                            <li>
                                <el-icon>
                                    <Sunset />
                                </el-icon>
                                创建日期: {{ timestamp(reactiveData.data.updateTime) }}
                            </li>
                        </ul>
                    </detail-card>
                </el-tab-pane>
                <el-tab-pane label="包含模板" name="profile">
                    <el-row>
                        <el-col v-for="data in 12" :key="data" :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
                            <skeleton-card :loading="reactiveData.profileLoading" :footer="true"></skeleton-card>
                        </el-col>
                        <el-col v-if="hasProfileData">
                            <el-empty description="暂无包含的模板数据！"></el-empty>
                        </el-col>
                        <el-col
                            v-for="data in reactiveData.listProfileData"
                            :key="data.id"
                            :xs="24"
                            :sm="12"
                            :md="8"
                            :lg="6"
                            :xl="4"
                        >
                            <profile-card
                                :data="data"
                                :point-table="reactiveData.pointTable"
                                :embedded="true"
                            ></profile-card>
                        </el-col>
                    </el-row>
                </el-tab-pane>
                <el-tab-pane label="包含位号" name="point">
                    <el-row>
                        <el-col v-for="data in 12" :key="data" :xs="24" :sm="12" :md="12" :lg="8" :xl="6">
                            <skeleton-card :loading="reactiveData.pointLoading" :footer="true"></skeleton-card>
                        </el-col>
                        <el-col v-if="hasPointData">
                            <el-empty description="暂无包含的位号数据！"></el-empty>
                        </el-col>
                        <el-col
                            v-for="data in reactiveData.listPointData"
                            :key="data.id"
                            :xs="24"
                            :sm="12"
                            :md="12"
                            :lg="8"
                            :xl="6"
                        >
                            <point-card
                                :data="data"
                                :profile="reactiveData.profileTable[data.profileId]"
                                :embedded="true"
                            ></point-card>
                        </el-col>
                    </el-row>
                </el-tab-pane>
                <el-tab-pane label="设备数据" name="pointValue">
                    <el-row>
                        <el-col v-for="data in 12" :key="data" :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
                            <skeleton-card :loading="reactiveData.pointValueLoading" :footer="true"></skeleton-card>
                        </el-col>
                        <el-col v-if="hasPointValueData">
                            <el-empty description="暂无设备位号数据！"></el-empty>
                        </el-col>
                        <el-col
                            v-for="data in reactiveData.listPointValueData"
                            :key="data.id"
                            :xs="24"
                            :sm="12"
                            :md="8"
                            :lg="6"
                            :xl="4"
                        >
                            <point-value-card
                                :data="data"
                                :unit="reactiveData.unitTable[data.pointId]"
                                :device="data"
                                :point="reactiveData.pointTable[data.pointId]"
                                :history-data="reactiveData.listPointValueHistoryData"
                                :embedded="true"
                                @show-detail="showPointValueDetail"
                            ></point-value-card>
                        </el-col>
                    </el-row>
                </el-tab-pane>
                <el-tab-pane label="设备模型" name="deviceModel">
                    <el-empty description="暂无设备模型数据！"></el-empty>
                </el-tab-pane>
                <el-tab-pane label="设备指令" name="deviceCommand">
                    <el-empty description="暂无设备指令数据！"></el-empty>
                </el-tab-pane>
                <el-tab-pane label="设备事件" name="deviceEvent">
                    <el-timeline>
                        <el-timeline-item timestamp="2021/7/30" placement="top">
                            <el-card>
                                <h4>设备数据上报</h4>
                                <p>该设备于 2021/7/30 20:46 开始上报数据</p>
                            </el-card>
                        </el-timeline-item>
                        <el-timeline-item timestamp="2021/7/30" placement="top">
                            <el-card>
                                <h4>设备上线</h4>
                                <p>该设备于 2021/7/30 20:46 上线</p>
                            </el-card>
                        </el-timeline-item>
                        <el-timeline-item timestamp="2021/7/30" placement="top">
                            <el-card>
                                <h4>设备注册</h4>
                                <p>该设备于 2021/7/30 20:46 注册成功</p>
                            </el-card>
                        </el-timeline-item>
                    </el-timeline>
                </el-tab-pane>
            </el-tabs>
        </base-card>

        <point-value-detail ref="pointValueDetailRef" :detail-data="reactiveData.pointValueDetailData">
        </point-value-detail>
    </div>
</template>

<script src="./index.ts" lang="ts" />

<style lang="less">
@import '~@/components/card/styles/things-card.less';
</style>
