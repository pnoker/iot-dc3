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
                    :class="{
                        'header-enable': 'ENABLE' === data.enableFlag,
                        'header-disable': 'ENABLE' !== data.enableFlag
                    }"
                    class="things-card__header"
                >
                    <div class="things-card-header-icon">
                        <img :alt="data.deviceName" :src="icon" />
                    </div>
                    <div class="things-card-header-name nowrap-name" @click="copyId(data.id, '设备ID')">
                        {{ data.deviceName }}
                    </div>
                    <div class="things-card-header-status" title="状态">
                        <el-tag v-if="status === 'ONLINE'" effect="plain" type="success">在线</el-tag>
                        <el-tag v-else-if="status === 'MAINTAIN'" effect="plain" type="warning">维护</el-tag>
                        <el-tag v-else-if="status === 'FAULT'" effect="plain" type="danger">故障</el-tag>
                        <el-tag v-else-if="status === 'DISABLE'" effect="plain" type="info">停用</el-tag>
                        <el-tag v-else effect="plain" type="info">离线</el-tag>
                    </div>
                </div>
                <div class="things-card__body">
                    <div class="things-card-body-content">
                        <ul>
                            <li class="nowrap-item">
                                <el-icon>
                                    <Promotion />
                                </el-icon>
                                所属驱动: {{ driver.driverName }}
                            </li>
                            <li class="nowrap-item">
                                <el-icon>
                                    <Edit />
                                </el-icon>
                                修改日期: {{ timestamp(data.operateTime) }}
                            </li>
                            <li class="nowrap-item">
                                <el-icon>
                                    <Sunset />
                                </el-icon>
                                创建日期: {{ timestamp(data.createTime) }}
                            </li>
                        </ul>
                    </div>
                    <div class="things-card-body-content" title="设备描述信息">
                        <p class="nowrap-description">
                            {{ data.remark ? data.remark : '无描述信息' }}
                        </p>
                    </div>
                </div>
                <div v-if="!embedded" class="things-card__footer">
                    <div class="things-card-footer-operation">
                        <el-popconfirm :icon="SwitchButton" icon-color="#e6a23c" placement="top" title="是否确定停用该设备?" @confirm="disableThing">
                            <template #reference>
                                <el-button :disabled="'ENABLE' !== data.enableFlag" link type="primary">停用</el-button>
                            </template>
                        </el-popconfirm>
                        <el-popconfirm :icon="CircleCheck" icon-color="#67c23a" placement="top" title="是否确定启用该设备?" @confirm="enableThing">
                            <template #reference>
                                <el-button :disabled="'ENABLE' === data.enableFlag" link type="primary">启用</el-button>
                            </template>
                        </el-popconfirm>
                        <el-popconfirm
                            :icon="CircleClose"
                            icon-color="#f56c6c"
                            placement="top"
                            title="是否确定删除该设备?该设备下的配置将会被全部删除, 且该操作不可恢复!"
                            @confirm="deleteThing"
                        >
                            <template #reference>
                                <el-button link type="primary">删除</el-button>
                            </template>
                        </el-popconfirm>
                        <el-button link type="primary" @click="edit">编辑</el-button>
                        <el-button link type="primary" @click="detail">详情</el-button>
                    </div>
                </div>
            </div>
        </el-card>
    </div>
</template>

<script lang="ts" src="./index.ts" />

<style lang="scss">
    @use '@/components/card/styles/things-card.scss';
</style>
