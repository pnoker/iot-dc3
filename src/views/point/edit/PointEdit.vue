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
                        <el-form-item class="edit-form-large" label="位号名称" prop="pointName">
                            <el-input v-model="reactiveData.pointFormData.pointName" placeholder="请输入位号名称" clearable></el-input>
                        </el-form-item>
                    </div>
                    <div class="edit-form-item">
                        <el-form-item class="edit-form-large" label="数据类型" prop="pointTypeFlag">
                            <el-select class="edit-form-large" v-model="reactiveData.pointFormData.pointTypeFlag" placeholder="请选择数据类型" clearable>
                                <el-option label="字符串(string)" value="STRING"></el-option>
                                <el-option label="字节(byte)" value="BYTE"></el-option>
                                <el-option label="短整数(short)" value="SHORT"></el-option>
                                <el-option label="整数(int)" value="INT"></el-option>
                                <el-option label="长整数(long)" value="LONG"></el-option>
                                <el-option label="浮点数(float)" value="FLOAT"></el-option>
                                <el-option label="双精度浮点数(double)" value="DOUBLE"></el-option>
                                <el-option label="布尔量(boolean)" value="BOOLEAN"></el-option>
                            </el-select>
                        </el-form-item>
                    </div>
                    <div class="edit-form-item">
                        <el-form-item class="edit-form-large" label="读写标识" prop="rwFlag">
                            <el-select class="edit-form-large" v-model="reactiveData.pointFormData.rwFlag" placeholder="请选择读写标识" clearable>
                                <el-option label="只读" value="R"></el-option>
                                <el-option label="只写" value="W"></el-option>
                                <el-option label="读写" value="RW"></el-option>
                            </el-select>
                        </el-form-item>
                    </div>
                    <div class="edit-form-item">
                        <el-form-item class="edit-form-large" label="使能" prop="enableFlag">
                            <el-select class="edit-form-large" v-model="reactiveData.pointFormData.enableFlag" placeholder="请选择使能" clearable>
                                <el-option label="启用" value="ENABLE"></el-option>
                                <el-option label="停用" value="DISABLE"></el-option>
                            </el-select>
                        </el-form-item>
                    </div>
                    <div class="edit-form-item">
                        <el-form-item class="edit-form-large" label="数据精度" prop="valueDecimal">
                            <el-input v-model="reactiveData.pointFormData.valueDecimal" placeholder="请输入数据精度" clearable></el-input>
                        </el-form-item>
                    </div>
                    <div class="edit-form-item">
                        <el-form-item class="edit-form-large" label="单位" prop="unit">
                            <el-input class="edit-form-large" v-model="reactiveData.pointFormData.unit" placeholder="请输入单位" clearable> </el-input>
                        </el-form-item>
                    </div>
                    <div class="edit-form-item">
                        <el-form-item class="edit-form-large" label="基础值" prop="baseValue">
                            <el-input v-model="reactiveData.pointFormData.baseValue" placeholder="请输入基础值" clearable></el-input>
                        </el-form-item>
                    </div>
                    <div class="edit-form-item">
                        <el-form-item class="edit-form-large" label="比例系数" prop="multiple">
                            <el-input v-model="reactiveData.pointFormData.multiple" placeholder="请输入比例系数" clearable></el-input>
                        </el-form-item>
                    </div>
                    <div class="edit-form-item">
                        <el-form-item class="edit-form-large" label="位号描述" prop="remark">
                            <el-input v-model="reactiveData.pointFormData.remark" placeholder="请输入位号描述" type="textarea" maxlength="300" show-word-limit clearable>
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
@use '@/components/card/styles/edit-card.scss';
</style>
