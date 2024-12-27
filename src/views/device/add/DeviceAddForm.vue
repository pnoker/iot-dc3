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
        title="新增设备"
    >
        <el-form ref="formDataRef" :model="reactiveData.formData" :rules="formRule">
            <el-form-item class="things-dialog-form-item" label="设备名称" prop="deviceName">
                <el-input v-model="reactiveData.formData.deviceName" clearable placeholder="请输入设备名称"></el-input>
            </el-form-item>
            <el-form-item class="things-dialog-form-item" label="所属驱动" prop="driverId">
                <el-select v-model="reactiveData.formData.driverId" class="edit-form-special" clearable placeholder="请选择所属驱动" @visible-change="driverDictionaryVisible">
                    <div class="tool-select">
                        <el-form-item class="tool-select-input">
                            <el-input v-model="reactiveData.driverQuery" clearable placeholder="请选择所属驱动" @input="driverDictionary" />
                        </el-form-item>
                        <el-pagination
                            :current-page="+reactiveData.driverPage.current"
                            :hide-on-single-page="true"
                            :page-size="+reactiveData.driverPage.size"
                            :pager-count="5"
                            :total="+reactiveData.driverPage.total"
                            background
                            class="tool-select-pagination"
                            layout="prev, pager, next"
                            small
                            @current-change="driverCurrentChange"
                        ></el-pagination>
                    </div>
                    <el-option v-for="dictionary in reactiveData.driverDictionary" :key="dictionary.value" :label="dictionary.label" :value="dictionary.value"></el-option>
                </el-select>
            </el-form-item>
            <el-form-item class="things-dialog-form-item" label="关联模板" prop="profileIds">
                <el-select
                    v-model="reactiveData.formData.profileIds"
                    :multiple="true"
                    class="edit-form-special"
                    clearable
                    placeholder="请选择关联模板"
                    @visible-change="profileDictionaryVisible"
                >
                    <div class="tool-select">
                        <el-form-item class="tool-select-input">
                            <el-input v-model="reactiveData.profileQuery" clearable placeholder="请选择关联模板" @input="profileDictionary" />
                        </el-form-item>
                        <el-pagination
                            :current-page="+reactiveData.profilePage.current"
                            :hide-on-single-page="true"
                            :page-size="+reactiveData.profilePage.size"
                            :pager-count="5"
                            :total="+reactiveData.profilePage.total"
                            background
                            class="tool-select-pagination"
                            layout="prev, pager, next"
                            small
                            @current-change="profileCurrentChange"
                        ></el-pagination>
                    </div>
                    <el-option v-for="dictionary in reactiveData.profileDictionary" :key="dictionary.value" :label="dictionary.label" :value="dictionary.value"></el-option>
                </el-select>
            </el-form-item>
            <el-form-item class="things-dialog-form-item" label="描述" prop="remark">
                <el-input v-model="reactiveData.formData.remark" clearable maxlength="300" placeholder="请输入设备描述" show-word-limit type="textarea"></el-input>
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

<style lang="scss">
@use '@/components/dialog/styles/things-dialog';
@use '@/components/card/styles/tool-card';
</style>
