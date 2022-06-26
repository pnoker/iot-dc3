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
                    <el-form-item prop="deviceId" label="设备">
                        <el-cascader class="search-form-large"
                                     placeholder="请选择设备"
                                     v-model="formData.deviceId"
                                     :options="deviceDictionary"
                                     :show-all-levels="false"
                                     clearable>
                        </el-cascader>
                    </el-form-item>
                    <el-form-item prop="pointId" label="位号">
                        <el-cascader class="search-form-large"
                                     placeholder="请选择位号"
                                     v-model="formData.pointId"
                                     :options="pointDictionary"
                                     :show-all-levels="false"
                                     clearable>
                        </el-cascader>
                    </el-form-item>
                </div>
                <el-form-item class="tool-card-body-button">
                    <el-button type="primary" icon="el-icon-search" @click="search">搜索</el-button>
                    <el-button icon="el-icon-refresh-left" @click="reset">重置</el-button>
                </el-form-item>
            </el-form>
            <div class="tool-card-footer">
                <div class="tool-card-footer-button">
                    <el-button type="success" icon="el-icon-plus" disabled>新增</el-button>
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
                </div>
            </div>
        </el-card>
    </div>
</template>

<script>
export default {
    name: "point-value-tool",
    props: {
        deviceDictionary: {
            type: Array,
            default: () => {
                return [];
            }
        },
        pointDictionary: {
            type: Array,
            default: () => {
                return [];
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
                    let formData = {};
                    let data = this.formData;
                    if (data.deviceId && data.deviceId[1]) {
                        formData.deviceId = data.deviceId[1];
                    }
                    if (data.pointId && data.pointId[1]) {
                        formData.pointId = data.pointId[1];
                    }
                    this.$emit("search", formData);
                }
            });
        },
        reset() {
            this.$refs["formData"].resetFields();
            this.$emit("reset");
        },
        refresh() {
            this.$emit("refresh");
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
