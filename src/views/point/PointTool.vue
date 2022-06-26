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
                    <el-form-item prop="name" label="位号名称">
                        <el-input placeholder="请输入位号名称"
                                  @keyup.enter.native="search"
                                  v-model="formData.name"
                                  clearable>
                        </el-input>
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
                    <el-button type="success" icon="el-icon-back" v-if="pre" @click="preHandle" plain>上一步</el-button>
                    <el-button type="primary" icon="el-icon-search" @click="search">搜索</el-button>
                    <el-button icon="el-icon-refresh-left" @click="reset">重置</el-button>
                    <el-button type="warning" icon="el-icon-right" v-if="pre" @click="nextHandle" plain>完成</el-button>
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
    name: "point-tool",
    props: {
        pre: {
            type: Boolean,
            default: () => {
                return false;
            }
        },
        next: {
            type: Boolean,
            default: () => {
                return false;
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
            formRule: {
                port: [
                    {type: "number", message: "端口必须为数字值"}
                ]
            }
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
        },
        preHandle() {
            this.$emit("pre-handle");
        },
        nextHandle() {
            this.$emit("next-handle");
        }
    }
};
</script>

<style lang="scss">
@import "~@/components/card/styles/tool-card.less";
</style>
