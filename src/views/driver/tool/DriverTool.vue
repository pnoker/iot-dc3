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
                     ref="formDataRef"
                     :rules="formRule"
                     :model="formData"
                     :inline="true">
                <div class="tool-card-body-form">
                    <el-form-item prop="name" label="驱动名称">
                        <el-input placeholder="请输入驱动名称"
                                  @keyup.enter.native="search"
                                  v-model="formData.name"
                                  clearable>
                        </el-input>
                    </el-form-item>
                    <el-form-item prop="serviceName" label="服务名称">
                        <el-input placeholder="请输入服务名称"
                                  @keyup.enter.native="search"
                                  v-model="formData.serviceName"
                                  clearable>
                        </el-input>
                    </el-form-item>
                    <el-form-item prop="host" label="主机">
                        <el-input placeholder="请输入主机"
                                  @keyup.enter.native="search"
                                  v-model="formData.host"
                                  clearable>
                        </el-input>
                    </el-form-item>
                    <el-form-item prop="port" label="端口">
                        <el-input class="search-form-small"
                                  autocomplete="off"
                                  placeholder="请输入端口"
                                  @keyup.enter.native="search"
                                  v-model.number="formData.port"
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
                    <el-button type="primary" :icon="Search" @click="search(formDataRef)">搜索</el-button>
                    <el-button :icon="RefreshRight" @click="reset(formDataRef)">重置</el-button>
                </el-form-item>
            </el-form>
            <div class="tool-card-footer">
                <div class="tool-card-footer-button">
                    <el-button v-if="add" type="success" icon="el-icon-plus">新增</el-button>
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
                        <el-button :icon="Refresh" @click="refresh" circle></el-button>
                    </el-tooltip>
                    <el-tooltip class="item" effect="dark" content="排序" placement="top">
                        <el-button :icon="Sort" @click="sort" circle></el-button>
                    </el-tooltip>
                </div>
            </div>
        </el-card>
    </div>
</template>

<script lang="ts">
import DriverTool from "@/views/driver/tool"

export default DriverTool
</script>

<style lang="scss">
@import "~@/components/card/styles/tool-card.less";
</style>
