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
    <el-dialog
        v-model="reactiveData.formVisible"
        :append-to-body="true"
        :close-on-click-modal="false"
        :close-on-press-escape="false"
        :show-close="false"
        class="things-dialog"
        draggable
        title="新增位号"
    >
        <el-form ref="formDataRef" :model="reactiveData.formData" :rules="formRule">
            <el-form-item class="things-dialog-form-item" label="位号名称" prop="pointName">
                <el-input v-model="reactiveData.formData.pointName" clearable placeholder="请输入位号名称"></el-input>
            </el-form-item>
            <el-form-item class="things-dialog-form-item" label="数据类型" prop="pointTypeFlag">
                <el-select v-model="reactiveData.formData.pointTypeFlag" clearable placeholder="请选择数据类型">
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
            <el-form-item class="things-dialog-form-item" label="读写标识" prop="rwFlag">
                <el-select v-model="reactiveData.formData.rwFlag" clearable placeholder="请选择读写标识">
                    <el-option label="只读" value="R"></el-option>
                    <el-option label="只写" value="W"></el-option>
                    <el-option label="读写" value="RW"></el-option>
                </el-select>
            </el-form-item>
            <el-form-item class="things-dialog-form-item" label="数据精度" prop="valueDecimal">
                <el-input v-model="reactiveData.formData.valueDecimal" clearable placeholder="请输入数据精度"></el-input>
            </el-form-item>
            <el-form-item class="things-dialog-form-item" label="单位" prop="unit">
                <el-input v-model="reactiveData.formData.unit" clearable placeholder="请输入单位"></el-input>
            </el-form-item>
            <el-form-item class="things-dialog-form-item" label="基础值" prop="baseValue">
                <el-input v-model="reactiveData.formData.baseValue" clearable placeholder="请输入基础值"></el-input>
            </el-form-item>
            <el-form-item class="things-dialog-form-item" label="比例系数" prop="multiple">
                <el-input v-model="reactiveData.formData.multiple" clearable placeholder="请输入比例系数"></el-input>
            </el-form-item>
            <el-form-item class="things-dialog-form-item" label="描述" prop="remark">
                <el-input v-model="reactiveData.formData.remark" clearable maxlength="300" placeholder="请输入位号描述" show-word-limit type="textarea"></el-input>
            </el-form-item>
        </el-form>
        <div class="things-dialog-footer">
            <slot name="footer">
                <el-button @click="cancel">取消</el-button>
                <el-button plain type="success" @click="reset">重置</el-button>
                <el-button type="primary" @click="addThing">确定</el-button>
            </slot>
        </div>
    </el-dialog>
</template>

<script lang="ts" src="./index.ts" />

<style lang="scss" scoped>
    @use '@/components/dialog/styles/things-dialog';
</style>
