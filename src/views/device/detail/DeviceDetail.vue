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
        <base-card>
            <el-tabs v-model="reactiveData.active" @tab-click="changeActive">
                <el-tab-pane label="设备信息" name="detail">
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
                                关联模板: {{ profileLength }} 个
                            </li>
                            <li>
                                <el-icon>
                                    <CollectionTag />
                                </el-icon>
                                关联位号: {{ pointLength }} 个
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
                <el-tab-pane label="关联模板" name="profile">
                    <profile ref="profileViewRef" :embedded="'device'" :device-id="reactiveData.id"></profile>
                </el-tab-pane>
                <el-tab-pane label="关联位号" name="point">
                    <point ref="pointViewRef" :embedded="'device'" :device-id="reactiveData.id"></point>
                </el-tab-pane>
                <el-tab-pane label="设备数据" name="pointValue">
                    <point-value ref="pointValueViewRef" :embedded="'device'" :device-id="reactiveData.id"></point-value>
                </el-tab-pane>
                <!-- <el-tab-pane label="设备模型" name="deviceModel">
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
                </el-tab-pane> -->
            </el-tabs>
        </base-card>
    </div>
</template>

<script src="./index.ts" lang="ts" />

<style lang="scss">
@import '@/components/card/styles/things-card.scss';
</style>
