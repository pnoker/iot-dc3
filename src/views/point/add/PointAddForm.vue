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
    <el-dialog
        v-model="reactiveData.formVisible"
        custom-class="things-dialog"
        title="新增位号"
        :show-close="false"
        :append-to-body="true"
        :close-on-click-modal="false"
        :close-on-press-escape="false"
    >
        <el-form ref="formDataRef" :model="reactiveData.formData" :rules="formRule">
            <el-form-item class="things-dialog-form-item" label="位号名称" prop="name">
                <el-input v-model="reactiveData.formData.name" placeholder="请输入位号名称" clearable></el-input>
            </el-form-item>
            <el-form-item class="things-dialog-form-item" label="数据类型" prop="type">
                <el-select v-model="reactiveData.formData.type" placeholder="请选择数据类型" clearable>
                    <el-option label="字节(byte)" value="byte"></el-option>
                    <el-option label="短整数(short)" value="short"></el-option>
                    <el-option label="整数(int)" value="int"></el-option>
                    <el-option label="长整数(long)" value="long"></el-option>
                    <el-option label="浮点数(float)" value="float"></el-option>
                    <el-option label="双精度浮点数(double)" value="double"></el-option>
                    <el-option label="布尔量(boolean)" value="boolean"></el-option>
                </el-select>
            </el-form-item>
            <el-form-item class="things-dialog-form-item" label="读写类型" prop="rw">
                <el-select v-model="reactiveData.formData.rw" placeholder="请选择读写类型" clearable>
                    <el-option label="只读" :value="0"></el-option>
                    <el-option label="只写" :value="1"></el-option>
                    <el-option label="读写" :value="2"></el-option>
                </el-select>
            </el-form-item>
            <el-form-item class="things-dialog-form-item" label="累计标识" prop="accrue">
                <el-select v-model="reactiveData.formData.accrue" placeholder="请选择累计标识" clearable>
                    <el-option label="累计" :value="true"></el-option>
                    <el-option label="不累计" :value="false"></el-option>
                </el-select>
            </el-form-item>
            <el-form-item class="things-dialog-form-item" label="数据格式" prop="format">
                <el-input v-model="reactiveData.formData.format" placeholder="请输入数据格式" clearable></el-input>
            </el-form-item>
            <el-form-item class="things-dialog-form-item" label="单位" prop="unit">
                <el-input v-model="reactiveData.formData.unit" placeholder="请输入单位" clearable></el-input>
            </el-form-item>
            <el-form-item class="things-dialog-form-item" label="基值" prop="base">
                <el-input v-model="reactiveData.formData.base" placeholder="请输入基值" clearable></el-input>
            </el-form-item>
            <el-form-item class="things-dialog-form-item" label="倍数" prop="multiple">
                <el-input v-model="reactiveData.formData.multiple" placeholder="请输入倍数" clearable></el-input>
            </el-form-item>
            <el-form-item class="things-dialog-form-item" label="最小值" prop="minimum">
                <el-input v-model="reactiveData.formData.minimum" placeholder="请输入最小值" clearable></el-input>
            </el-form-item>
            <el-form-item class="things-dialog-form-item" label="最大值" prop="maximum">
                <el-input v-model="reactiveData.formData.maximum" placeholder="请输入最大值" clearable></el-input>
            </el-form-item>
            <el-form-item class="things-dialog-form-item" label="描述" prop="description">
                <el-input
                    v-model="reactiveData.formData.description"
                    placeholder="请输入位号描述"
                    type="textarea"
                    maxlength="300"
                    show-word-limit
                    clearable
                ></el-input>
            </el-form-item>
        </el-form>
        <div class="things-dialog-footer">
            <slot name="footer">
                <el-button @click="cancel">取消</el-button>
                <el-button type="success" plain @click="reset">重置</el-button>
                <el-button type="primary" @click="addThing">确定</el-button>
            </slot>
        </div>
    </el-dialog>
</template>

<script src="./index.ts" lang="ts" />

<style lang="less">
@import '~@/components/dialog/styles/things-dialog';
</style>
