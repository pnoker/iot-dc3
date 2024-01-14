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
    <div class="things-card">
        <el-card shadow="hover">
            <div class="things-card-content">
                <div
                    class="things-card__header"
                    :class="{
                        'header-enable': data.interval < 200,
                        'header-disable': data.interval >= 200,
                    }"
                >
                    <div class="things-card-header-icon">
                        <img :src="icon" :alt="data.pointName" />
                    </div>
                    <div class="things-card-header-name nowrap-name" @click="copyId(data.pointId, '位号值ID')">
                        {{ point.pointName }}
                    </div>
                    <div class="things-card-header-status" title="读写标识">
                        <el-tag v-if="data.rwFlag === 'R'" type="warning" effect="plain">只读</el-tag>
                        <el-tag v-else-if="data.rwFlagrw === 'W'" type="info" effect="plain">只写</el-tag>
                        <el-tag v-else-if="data.rwFlag === 'RW'" type="success" effect="plain">读写</el-tag>
                    </div>
                </div>
                <div class="things-card__body">
                    <div class="things-card-body-content">
                        <ul class="things-card-body-content-value">
                            <li class="nowrap-item value" title="处理值，点击复制" @click="copyValue(data)">{{ data.value }} {{ unit }}</li>
                            <li class="nowrap-item" title="计算值">
                                {{ data.calculateValue || '-' }}
                            </li>
                            <li class="nowrap-item">
                                <el-icon><Sunrise /></el-icon> 原始值: {{ data.rawValue }}
                            </li>
                            <li class="nowrap-item value-point" v-if="embedded == ''">
                                <el-icon> <Management /> </el-icon> 所属设备: {{ device.deviceName }}
                            </li>
                            <li class="nowrap-item">
                                <el-icon><Timer /></el-icon> 数据延时: {{ data.interval }} ms
                            </li>
                            <li class="nowrap-item">
                                <el-icon><Edit /></el-icon> 采集日期: {{ timestamp(data.originTime) }}
                            </li>
                            <li class="nowrap-item">
                                <el-icon><Sunset /></el-icon> 保存日期: {{ timestamp(data.createTime) }}
                            </li>
                        </ul>
                    </div>
                    <div class="things-card-body-content-time" v-if="embedded != ''">
                        <div :id="data.pointId"></div>
                    </div>
                </div>
                <div class="things-card__footer" v-if="embedded == ''">
                    <div class="things-card-footer-operation">
                        <el-popconfirm title="是否确定删除该数据？该数据下的配置将会被全部删除，且该操作不可恢复！" placement="top" :icon="CircleClose" icon-color="#f56c6c">
                            <template #reference>
                                <el-button type="primary" link>删除</el-button>
                            </template>
                        </el-popconfirm>
                        <el-button type="primary" link>编辑</el-button>
                        <el-button type="primary" link>详情</el-button>
                    </div>
                </div>
            </div>
        </el-card>
    </div>
</template>

<script src="./index.ts" lang="ts" />

<style lang="scss">
@import '@/components/card/styles/things-card';
</style>
