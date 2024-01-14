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
    <div class="tool-card">
        <el-card :shadow="embedded == '' ? 'hover' : 'never'">
            <el-form class="tool-card-body" ref="formDataRef" :model="reactiveData.formData" :rules="formRule" :inline="true">
                <div class="tool-card-body-form">
                    <el-form-item v-if="embedded == ''" prop="deviceId" label="设备">
                        <el-select
                            class="edit-form-special"
                            v-model="reactiveData.formData.deviceId"
                            placeholder="请选择设备"
                            filterable
                            remote
                            :remote-method="deviceDictionary"
                            clearable
                            @change="pointDictionaryChange"
                            @visible-change="deviceDictionaryVisible"
                        >
                            <div class="tool-select">
                                <!-- <el-form-item class="tool-select-input">
                                    <el-input v-model="reactiveData.deviceQuery" placeholder="请输入设备名称" clearable @input="deviceDictionary" />
                                </el-form-item> -->
                                <el-pagination
                                    class="tool-select-pagination"
                                    :hide-on-single-page="true"
                                    layout="prev, pager, next"
                                    :pager-count="5"
                                    :page-size="+reactiveData.devicePage.size"
                                    :current-page="+reactiveData.devicePage.current"
                                    :total="+reactiveData.devicePage.total"
                                    small
                                    background
                                    @current-change="deviceCurrentChange"
                                ></el-pagination>
                            </div>
                            <el-option v-for="dictionary in reactiveData.deviceDictionary" :key="dictionary.value" :label="dictionary.label" :value="dictionary.value"></el-option>
                        </el-select>
                    </el-form-item>
                    <el-form-item v-if="embedded == ''" prop="pointId" label="位号">
                        <el-select class="edit-form-special" v-model="reactiveData.formData.pointId" placeholder="请选择位号" clearable @visible-change="pointDictionaryVisible">
                            <div class="tool-select">
                                <el-form-item class="tool-select-input">
                                    <el-input v-model="reactiveData.pointQuery" placeholder="请输入位号名称" clearable @input="pointDictionary" />
                                </el-form-item>
                                <el-pagination
                                    class="tool-select-pagination"
                                    :hide-on-single-page="true"
                                    layout="prev, pager, next"
                                    :pager-count="5"
                                    :page-size="+reactiveData.pointPage.size"
                                    :current-page="+reactiveData.pointPage.current"
                                    :total="+reactiveData.pointPage.total"
                                    small
                                    background
                                    @current-change="pointCurrentChange"
                                ></el-pagination>
                            </div>
                            <el-option v-for="dictionary in reactiveData.pointDictionary" :key="dictionary.value" :label="dictionary.label" :value="dictionary.value"></el-option>
                        </el-select>
                    </el-form-item>
                    <el-form-item v-if="embedded == 'device'" prop="pointName" label="位号名称">
                        <el-input class="edit-form-default" v-model="reactiveData.formData.pointName" placeholder="请输入位号名称" clearable @keyup.enter="search"> </el-input>
                    </el-form-item>
                    <el-form-item v-if="embedded == 'device'" prop="enableFlag" label="使能">
                        <el-select class="edit-form-small" v-model="reactiveData.formData.enableFlag" placeholder="请选择使能" clearable>
                            <el-option label="启用" value="ENABLE"></el-option>
                            <el-option label="停用" value="DISABLE"></el-option>
                        </el-select>
                    </el-form-item>
                </div>
                <el-form-item class="tool-card-body-button">
                    <el-button type="primary" :icon="Search" @click="search">搜索</el-button>
                    <el-button :icon="RefreshRight" @click="reset">重置</el-button>
                </el-form-item>
            </el-form>
            <div class="tool-card-footer">
                <div class="tool-card-footer-button">
                    <el-button v-if="embedded == ''" type="success" :icon="Plus" disabled>新增</el-button>
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

<style lang="scss">
@import '@/components/card/styles/tool-card.scss';
</style>
