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
    <div class="edit-card">
        <div class="edit-card-header">
            <el-card shadow="hover">
                <el-steps :active="reactiveData.active" finish-status="success" align-center>
                    <el-step title="模板信息配置"></el-step>
                    <el-step title="模板位号配置"></el-step>
                    <el-step title="模板配置完成"></el-step>
                </el-steps>
            </el-card>
        </div>

        <div class="edit-card-body">
            <el-card v-if="reactiveData.active === 0" shadow="hover">
                <el-divider content-position="left">模板信息配置</el-divider>
                <el-form ref="formDataRef" :inline="true" :model="reactiveData.profileFormData" :rules="formRule">
                    <div class="edit-form-item">
                        <el-form-item label="模板名称" prop="name">
                            <el-input
                                v-model="reactiveData.profileFormData.name"
                                clearable
                                class="edit-form-default"
                                placeholder="请输入模板名称"
                            ></el-input>
                        </el-form-item>
                        <el-form-item label="使能" prop="enable">
                            <el-select
                                v-model="reactiveData.profileFormData.enable"
                                clearable
                                class="edit-form-small"
                                placeholder="请选择使能"
                            >
                                <el-option label="启用" :value="true"></el-option>
                                <el-option label="停用" :value="false"></el-option>
                            </el-select>
                        </el-form-item>
                    </div>
                    <div class="edit-form-item">
                        <el-form-item label="模板描述" prop="description">
                            <el-input
                                v-model="reactiveData.profileFormData.description"
                                clearable
                                class="edit-form-large"
                                type="textarea"
                                maxlength="300"
                                show-word-limit
                                placeholder="请输入模板描述"
                            >
                            </el-input>
                        </el-form-item>
                    </div>
                    <el-form-item class="edit-form-button">
                        <el-button type="success" :icon="Back" plain @click="done">返回</el-button>
                        <el-button :icon="RefreshLeft" @click="profileReset">恢复</el-button>
                        <el-button type="warning" :icon="Right" plain @click="next">下一步</el-button>
                    </el-form-item>
                </el-form>
            </el-card>
            <el-card v-if="reactiveData.active === 1" shadow="hover">
                <el-divider content-position="left">模板位号配置</el-divider>
                <point
                    :embedded="'edit'"
                    :pre="true"
                    :profile-id="reactiveData.id"
                    @pre-handle="pre"
                    @next-handle="next"
                ></point>
            </el-card>
            <el-card v-if="reactiveData.active === 2" shadow="hover">
                <el-divider content-position="left">模板配置完成</el-divider>
                <el-result icon="success" title="配置完成" sub-title="您可以返回进行下一步操作">
                    <template #extra>
                        <el-button type="primary" plain @click="done">返回</el-button>
                    </template>
                </el-result>
            </el-card>
        </div>
    </div>
</template>

<script src="./index.ts" lang="ts" />

<style lang="less">
@import '~@/components/card/styles/edit-card.less';
</style>
