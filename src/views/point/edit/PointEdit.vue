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
                <el-steps :active="reactiveData.active" align-center>
                    <el-step title="位号信息配置"></el-step>
                    <el-step title="位号配置完成"></el-step>
                </el-steps>
            </el-card>
        </div>

        <div class="edit-card-body">
            <el-card v-if="reactiveData.active === 0" shadow="hover">
                <el-divider content-position="left">位号信息配置</el-divider>
                <el-form ref="formDataRef" :inline="true" :model="reactiveData.pointFormData" :rules="pointFormRule">
                    <div class="edit-form-item">
                        <el-form-item label="位号名称" prop="name">
                            <el-input v-model="reactiveData.pointFormData.name" class="edit-form-large" placeholder="请输入位号名称" clearable></el-input>
                        </el-form-item>
                    </div>
                    <div class="edit-form-item">
                        <el-form-item label="数据类型" prop="type">
                            <el-select v-model="reactiveData.pointFormData.type" class="edit-form-default" placeholder="请选择数据类型" clearable>
                                <el-option label="字节(byte)" value="byte"></el-option>
                                <el-option label="短整数(short)" value="short"></el-option>
                                <el-option label="整数(int)" value="int"></el-option>
                                <el-option label="长整数(long)" value="long"></el-option>
                                <el-option label="浮点数(float)" value="float"></el-option>
                                <el-option label="双精度浮点数(double)" value="double"></el-option>
                                <el-option label="布尔量(boolean)" value="boolean"></el-option>
                            </el-select>
                        </el-form-item>
                        <el-form-item label="读写类型" prop="rw">
                            <el-select v-model="reactiveData.pointFormData.rw" class="edit-form-small" placeholder="请选择读写类型" clearable>
                                <el-option label="只读" :value="0"></el-option>
                                <el-option label="只写" :value="1"></el-option>
                                <el-option label="读写" :value="2"></el-option>
                            </el-select>
                        </el-form-item>
                        <el-form-item label="累计标识" prop="accrue">
                            <el-select v-model="reactiveData.pointFormData.accrue" class="edit-form-small" placeholder="请选择累计标识" clearable>
                                <el-option label="累计" :value="true"></el-option>
                                <el-option label="不累计" :value="false"></el-option>
                            </el-select>
                        </el-form-item>
                        <el-form-item label="使能" prop="enable">
                            <el-select v-model="reactiveData.pointFormData.enable" class="edit-form-small" placeholder="请选择使能" clearable>
                                <el-option label="启用" :value="true"></el-option>
                                <el-option label="停用" :value="false"></el-option>
                            </el-select>
                        </el-form-item>
                    </div>
                    <div class="edit-form-item">
                        <el-form-item label="数据格式" prop="format">
                            <el-input v-model="reactiveData.pointFormData.format" class="edit-form-default" placeholder="请输入数据格式" clearable></el-input>
                        </el-form-item>
                        <el-form-item label="单位" prop="unit">
                            <el-input v-model="reactiveData.pointFormData.unit" class="edit-form-default" placeholder="请输入单位" clearable></el-input>
                        </el-form-item>
                    </div>
                    <div class="edit-form-item">
                        <el-form-item label="基值" prop="base">
                            <el-input v-model="reactiveData.pointFormData.base" class="edit-form-default" placeholder="请输入基值" clearable></el-input>
                        </el-form-item>
                        <el-form-item label="倍数" prop="multiple">
                            <el-input v-model="reactiveData.pointFormData.multiple" class="edit-form-default" placeholder="请输入倍数" clearable></el-input>
                        </el-form-item>
                    </div>
                    <div class="edit-form-item">
                        <el-form-item label="最小值" prop="minimum">
                            <el-input v-model="reactiveData.pointFormData.minimum" class="edit-form-default" placeholder="请输入最小值" clearable></el-input>
                        </el-form-item>
                        <el-form-item label="最大值" prop="maximum">
                            <el-input v-model="reactiveData.pointFormData.maximum" class="edit-form-default" placeholder="请输入最大值" clearable></el-input>
                        </el-form-item>
                    </div>
                    <div class="edit-form-item">
                        <el-form-item label="位号描述" prop="description">
                            <el-input
                                v-model="reactiveData.pointFormData.description"
                                class="edit-form-large"
                                placeholder="请输入位号描述"
                                type="textarea"
                                maxlength="300"
                                show-word-limit
                                clearable
                            >
                            </el-input>
                        </el-form-item>
                    </div>
                    <el-form-item class="edit-form-button">
                        <el-button type="success" :icon="Back" plain @click="done">返回</el-button>
                        <el-button :icon="RefreshLeft" @click="pointReset">恢复</el-button>
                        <el-button type="warning" :icon="Right" plain @click="next">下一步</el-button>
                    </el-form-item>
                </el-form>
            </el-card>
            <el-card v-if="reactiveData.active === 1" shadow="hover">
                <el-divider content-position="left">位号配置完成</el-divider>
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

<style lang="scss">
@import '@/components/card/styles/edit-card.scss';
</style>
