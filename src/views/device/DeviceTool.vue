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
            <el-form class="tool-card-body"
                     ref="formData"
                     :model="formData"
                     :rules="formRule"
                     :inline="true">
                <div class="tool-card-body-form">
                    <el-form-item prop="name" label="设备名称">
                        <el-input placeholder="请输入设备名称"
                                  @keyup.enter.native="search"
                                  v-model="formData.name"
                                  clearable>
                        </el-input>
                    </el-form-item>
                    <el-form-item prop="driverId" label="所属驱动">
                        <el-select placeholder="请选择所属驱动"
                                   v-model="formData.driverId"
                                   filterable
                                   clearable>
                            <el-option :key="dictionary.value"
                                       :label="dictionary.label"
                                       :value="dictionary.value"
                                       v-for="dictionary in driverDictionary">
                            </el-option>
                        </el-select>
                    </el-form-item>
                    <el-form-item prop="multi" label="存储类型">
                        <el-select class="search-form-medium"
                                   placeholder="请选择存储类型"
                                   v-model="formData.multi"
                                   clearable>
                            <el-option label="单点数据" :value="false"></el-option>
                            <el-option label="结构数据" :value="true"></el-option>
                        </el-select>
                    </el-form-item>
                    <el-form-item prop="enable" label="使能">
                        <el-select class="search-form-small"
                                   placeholder="请选择使能"
                                   v-model="formData.enable"
                                   clearable>
                            <el-option label="启用" :value="true"></el-option>
                            <el-option label="停用" :value="false"></el-option>
                        </el-select>
                    </el-form-item>
                </div>
                <el-form-item class="tool-card-body-button">
                    <el-button type="primary" icon="el-icon-search" @click="search">搜索</el-button>
                    <el-button icon="el-icon-refresh-left" @click="reset">重置</el-button>
                </el-form-item>
            </el-form>
            <div class="tool-card-footer">
                <div class="tool-card-footer-button">
                    <el-button type="success" icon="el-icon-plus" @click="showAdd">新增</el-button>
                </div>
                <div class="tool-card-footer-page">
                    <el-pagination layout="total, prev, pager, next, sizes"
                                   :page-sizes="[12, 24, 36, 48]"
                                   :page-size="+page.size"
                                   :current-page="+page.current"
                                   :total="+page.total"
                                   @size-change="sizeChange"
                                   @current-change="currentChange"
                                   background>
                    </el-pagination>
                    <el-tooltip class="item" effect="dark" content="刷新" placement="top">
                        <el-button icon="el-icon-refresh" @click="refresh" circle></el-button>
                    </el-tooltip>
                    <el-tooltip class="item" effect="dark" content="排序" placement="top">
                        <el-button icon="el-icon-sort" @click="sort" circle></el-button>
                    </el-tooltip>
                </div>
            </div>
        </el-card>
    </div>
</template>

<script>
export default {
    name: "device-tool",
    props: {
        driverDictionary: {
            type: Array,
            default: () => {
                return []
            }
        },
        page: {
            type: Object,
            default: () => {
                return {}
            }
        }
    },
    data() {
        return {
            formData: {},
            formRule: {}
        }
    },
    methods: {
        search() {
            this.$refs["formData"].validate((valid) => {
                if (valid) {
                    this.$emit("search", this.formData);
                }
            });
        },
        reset() {
            this.$refs["formData"].resetFields();
            this.$emit("reset");
        },
        showAdd() {
            this.$emit("showAdd");
        },
        refresh() {
            this.$emit("refresh");
        },
        sort() {
            this.$emit("sort");
        },
        sizeChange(size) {
            this.$emit("size-change", size);
        },
        currentChange(current) {
            this.$emit("current-change", current);
        }
    }
};
</script>

<style lang="scss">
@import "~@/components/card/styles/tool-card.less";
</style>
