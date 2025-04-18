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
        <el-card shadow="hover">
            <el-form ref="formDataRef" :inline="true" :model="reactiveData.formData" :rules="formRule" class="tool-card__body">
                <div class="tool-card-body-form">
                    <el-form-item label="设备名称" prop="deviceName">
                        <el-input v-model="reactiveData.formData.deviceName" class="edit-form-default" clearable placeholder="请输入设备名称" @keyup.enter="search" />
                    </el-form-item>
                    <el-form-item v-if="embedded != 'driver'" label="所属驱动" prop="driverId">
                        <el-select
                            v-model="reactiveData.formData.driverId"
                            :remote-method="driverDictionary"
                            class="edit-form-special"
                            clearable
                            filterable
                            placeholder="请选择所属驱动"
                            remote
                            @change="driverCurrentChange"
                            @visible-change="driverDictionaryVisible"
                        >
                            <div class="tool-select">
                                <el-pagination
                                    :current-page="+reactiveData.driverPage.current"
                                    :hide-on-single-page="true"
                                    :page-size="+reactiveData.driverPage.size"
                                    :pager-count="5"
                                    :total="+reactiveData.driverPage.total"
                                    background
                                    class="tool-select-pagination"
                                    layout="prev, pager, next"
                                    @current-change="driverCurrentChange"
                                ></el-pagination>
                            </div>
                            <el-option v-for="dictionary in reactiveData.driverDictionary" :key="dictionary.value" :label="dictionary.label" :value="dictionary.value"></el-option>
                        </el-select>
                    </el-form-item>
                    <el-form-item label="使能" prop="enableFlag">
                        <el-select v-model="reactiveData.formData.enableFlag" class="edit-form-small" clearable placeholder="请选择使能">
                            <el-option label="启用" value="ENABLE"></el-option>
                            <el-option label="停用" value="DISABLE"></el-option>
                        </el-select>
                    </el-form-item>
                </div>
                <el-form-item class="tool-card-body-button">
                    <el-button :icon="Search" type="primary" @click="search">搜索</el-button>
                    <el-button :icon="RefreshRight" @click="reset">重置</el-button>
                </el-form-item>
            </el-form>
            <div class="tool-card__footer">
                <div class="tool-card-footer-button">
                    <el-button v-if="embedded == ''" :icon="Plus" type="success" @click="showAdd">新增</el-button>
                    <el-button v-if="embedded == ''" :icon="Upload" type="primary" @click="showImport">导入</el-button>
                </div>
                <div class="tool-card-footer-page">
                    <el-pagination
                        :current-page="+page.current"
                        :page-size="+page.size"
                        :page-sizes="[6, 12, 24, 36, 48, 96]"
                        :total="+page.total"
                        background
                        layout="total, prev, pager, next, sizes"
                        @size-change="sizeChange"
                        @current-change="currentChange"
                    ></el-pagination>
                    <el-tooltip class="item" content="刷新" effect="dark" placement="top">
                        <el-button :icon="Refresh" circle @click="refresh"></el-button>
                    </el-tooltip>
                    <el-tooltip class="item" content="排序" effect="dark" placement="top">
                        <el-button :icon="Sort" circle @click="sort"></el-button>
                    </el-tooltip>
                </div>
            </div>
        </el-card>
    </div>
</template>

<script lang="ts" src="./index.ts" />

<style lang="scss" scoped>
    @use '@/components/card/styles/tool-card';
</style>
