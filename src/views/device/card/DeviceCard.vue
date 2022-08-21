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
    <div class="things-card">
        <el-card shadow="hover">
            <div class="things-card-content">
                <div
                    class="things-card__header"
                    :class="{
                        'header-enable': data.enable,
                        'header-disable': !data.enable,
                    }"
                >
                    <div class="things-card-header-icon">
                        <img :src="icon" :alt="data.name" />
                    </div>
                    <div class="things-card-header-name nowrap-name" @click="copyId(data.id, '设备ID')">
                        {{ data.name }}
                    </div>
                    <div title="状态" class="things-card-header-status">
                        <el-tag v-if="status === 'ONLINE'" type="success" effect="plain">在线</el-tag>
                        <el-tag v-else-if="status === 'MAINTAIN'" type="warning" effect="plain">维护</el-tag>
                        <el-tag v-else-if="status === 'FAULT'" type="danger" effect="plain">故障</el-tag>
                        <el-tag v-else-if="status === 'DISABLE'" type="info" effect="plain">停用</el-tag>
                        <el-tag v-else type="info" effect="plain">离线</el-tag>
                    </div>
                </div>
                <div class="things-card__body">
                    <div class="things-card-body-content">
                        <ul>
                            <li class="nowrap-item">
                                <el-icon>
                                    <Promotion />
                                </el-icon>
                                所属驱动: {{ driver.name }}
                            </li>
                            <li class="nowrap-item">
                                <el-icon>
                                    <Coin />
                                </el-icon>
                                存储类型: {{ data.multi ? '结构数据' : '单点数据' }}
                            </li>
                            <li class="nowrap-item">
                                <el-icon>
                                    <Edit />
                                </el-icon>
                                修改日期: {{ timestamp(data.updateTime) }}
                            </li>
                            <li class="nowrap-item">
                                <el-icon>
                                    <Sunset />
                                </el-icon>
                                创建日期: {{ timestamp(data.createTime) }}
                            </li>
                        </ul>
                    </div>
                    <div title="设备描述信息" class="things-card-body-content">
                        <p class="nowrap-description">
                            {{ data.description ? data.description : '无描述信息' }}
                        </p>
                    </div>
                </div>
                <div v-if="!embedded" class="things-card__footer">
                    <div class="things-card-footer-operation">
                        <el-popconfirm
                            title="是否确定停用该设备？"
                            placement="top"
                            :icon="SwitchButton"
                            icon-color="#e6a23c"
                            @confirm="disableThing"
                        >
                            <template #reference>
                                <el-button type="primary" :disabled="!data.enable" link>停用</el-button>
                            </template>
                        </el-popconfirm>
                        <el-popconfirm
                            title="是否确定启用该设备？"
                            placement="top"
                            :icon="CircleCheck"
                            icon-color="#67c23a"
                            @confirm="enableThing"
                        >
                            <template #reference>
                                <el-button type="primary" :disabled="data.enable" link>启用</el-button>
                            </template>
                        </el-popconfirm>
                        <el-popconfirm
                            title="是否确定删除该设备？该设备下的配置将会被全部删除，且该操作不可恢复！"
                            placement="top"
                            :icon="CircleClose"
                            icon-color="#f56c6c"
                            @confirm="deleteThing"
                        >
                            <template #reference>
                                <el-button type="primary" link>删除</el-button>
                            </template>
                        </el-popconfirm>
                        <el-button type="primary" link @click="edit">编辑</el-button>
                        <el-button type="primary" link @click="detail">详情</el-button>
                    </div>
                </div>
            </div>
        </el-card>
    </div>
</template>

<script src="./index.ts" lang="ts" />

<style lang="less">
@import '~@/components/card/styles/things-card.less';
</style>
