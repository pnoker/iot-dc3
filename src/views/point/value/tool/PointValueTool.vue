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
    <div class="tool-card">
        <el-card shadow="hover">
            <el-form
                ref="formDataRef"
                class="tool-card-body"
                :model="reactiveData.formData"
                :rules="formRule"
                :inline="true"
            >
                <div class="tool-card-body-form">
                    <el-form-item prop="deviceId" label="设备">
                        <el-cascader
                            v-model="reactiveData.formData.deviceId"
                            class="edit-form-large"
                            placeholder="请选择设备"
                            :options="deviceDictionary"
                            :show-all-levels="false"
                            filterable
                            clearable
                        >
                        </el-cascader>
                    </el-form-item>
                    <el-form-item prop="pointId" label="位号">
                        <el-cascader
                            v-model="reactiveData.formData.pointId"
                            class="edit-form-large"
                            placeholder="请选择位号"
                            :options="pointDictionary"
                            :show-all-levels="false"
                            filterable
                            clearable
                        >
                        </el-cascader>
                    </el-form-item>
                </div>
                <el-form-item class="tool-card-body-button">
                    <el-button type="primary" :icon="Search" @click="search">搜索</el-button>
                    <el-button :icon="RefreshRight" @click="reset">重置</el-button>
                </el-form-item>
            </el-form>
            <div class="tool-card-footer">
                <div class="tool-card-footer-button">
                    <el-button type="success" :icon="Plus" disabled>新增</el-button>
                </div>
                <div class="tool-card-footer-page">
                    <el-pagination
                        layout="total, prev, pager, next, sizes"
                        :page-sizes="[6, 12, 24, 36, 48]"
                        :page-size="+page.size"
                        :current-page="+page.current"
                        :total="+page.total"
                        background
                        @size-change="sizeChange"
                        @current-change="currentChange"
                    >
                    </el-pagination>
                    <el-tooltip class="item" effect="dark" content="刷新" placement="top">
                        <el-button :icon="Refresh" circle @click="refresh"></el-button>
                    </el-tooltip>
                </div>
            </div>
        </el-card>
    </div>
</template>

<script src="./index.ts" lang="ts" />

<style lang="less">
@import '~@/components/card/styles/tool-card.less';
</style>
