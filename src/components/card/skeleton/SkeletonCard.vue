<!--
  - Copyright 2016-present the original author or authors.
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
    <el-skeleton :loading="loading" animated>
        <template #template>
            <div class="things-card">
                <el-card shadow="hover">
                    <div class="things-card-content">
                        <div v-if="!header" class="things-card__header">
                            <el-skeleton-item class="things-card-header-icon skeleton" ariant="image" />
                            <el-skeleton-item class="things-card-header-name skeleton" variant="text" />
                        </div>
                        <div class="things-card__body">
                            <div class="things-card-body-content" :class="{ left: header, center: !header }">
                                <ul>
                                    <el-skeleton-item class="nowrap-item skeleton" variant="text" />
                                    <el-skeleton-item class="nowrap-item skeleton" variant="text" />
                                    <el-skeleton-item class="nowrap-item skeleton" variant="text" />
                                    <el-skeleton-item class="nowrap-item skeleton" variant="text" />
                                    <div v-if="header">
                                        <el-skeleton-item class="nowrap-item skeleton" variant="text" />
                                        <el-skeleton-item class="nowrap-item skeleton" variant="text" />
                                        <el-skeleton-item class="nowrap-item skeleton" variant="text" />
                                        <el-skeleton-item class="nowrap-item skeleton" variant="text" />
                                    </div>
                                </ul>
                            </div>
                            <div v-if="!header" class="things-card-body-content">
                                <el-skeleton-item class="nowrap-description skeleton" variant="text" />
                            </div>
                        </div>
                        <div v-if="!footer" class="things-card__footer">
                            <div class="things-card-footer-operation">
                                <el-skeleton-item class="operation-tooltip skeleton" variant="button" />
                                <el-skeleton-item class="operation-tooltip skeleton" variant="button" />
                                <el-skeleton-item class="operation-tooltip skeleton" variant="button" />
                                <el-skeleton-item class="operation-tooltip skeleton" variant="button" />
                                <el-skeleton-item class="operation-tooltip skeleton" variant="button" />
                            </div>
                        </div>
                    </div>
                </el-card>
            </div>
        </template>
    </el-skeleton>
</template>

<script src="./index.ts" lang="ts" />

<style lang="scss">
@import './style.scss';
</style>
