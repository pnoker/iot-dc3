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
                <div class="things-card__header" :class="{
                    'header-enable': 'ENABLE' === data.enableFlag,
                    'header-disable': 'ENABLE' !== data.enableFlag,
                }">
                    <div class="things-card-header-icon">
                        <img :src="icon" :alt="data.flowLabel" />
                    </div>
                    <div class="things-card-header-name nowrap-name" @click="openFlows(data.flowId)">
                        {{ data.flowLabel }}
                    </div>
                    <div title="状态" class="things-card-header-status">
                        <el-tag v-if="data.flowDisabled === 0" type="success" effect="plain"> 启用 </el-tag>
                        <el-tag v-else type="info" effect="plain">禁用</el-tag>
                    </div>
                </div>
                <div class="things-card__body">
                    <div class="things-card-body-content">
                        <ul>
                            <li class="nowrap-item">
                                <el-icon>
                                    <Edit />
                                </el-icon>
                                执行次数: 开发中...
                            </li>
                            <li class="nowrap-item">
                                <el-icon>
                                    <Edit />
                                </el-icon>
                                子节点数: 开发中...
                            </li>
                            <li class="nowrap-item">
                                <el-icon>
                                    <Edit />
                                </el-icon>
                                最后操作时间: {{ timestamp(data.operateTime) }}
                            </li>
                        </ul>
                    </div>
                </div>

            </div>
        </el-card>
    </div>
</template>

<script src="./index.ts" lang="ts" />

<style lang="scss">
@import '@/components/card/styles/things-card.scss';
</style>
