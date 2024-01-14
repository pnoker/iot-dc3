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
        class="things-dialog"
        v-model="reactiveData.formVisible"
        title="编辑位号值"
        :show-close="false"
        :append-to-body="true"
        :close-on-click-modal="false"
        :close-on-press-escape="false"
        draggable
    >
        <el-form ref="formDataRef" :model="reactiveData.formData" :rules="formRule">
            <el-form-item class="things-dialog-form-item" label="位号值" prop="value">
                <el-input v-model="formData.value" placeholder="请输入位号值" clearable></el-input>
            </el-form-item>
            <el-form-item class="things-dialog-form-item" label="操作描述" prop="remark">
                <el-input v-model="reactiveData.remark" type="textarea" maxlength="300" show-word-limit placeholder="请输入本次操作描述" clearable></el-input>
            </el-form-item>
        </el-form>
        <div class="things-dialog-footer">
            <slot name="footer">
                <el-button @click="cancel">取消</el-button>
                <el-button type="success" plain @click="reset">重置</el-button>
                <el-button type="primary" @click="updateThing">确定</el-button>
            </slot>
        </div>
    </el-dialog>
</template>

<script src="./index.ts" lang="ts" />

<style lang="scss">
@import '@/components/dialog/styles/things-dialog';
</style>
