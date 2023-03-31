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
    <el-dialog
        v-model="reactiveData.formVisible"
        class="things-dialog"
        title="新增设备"
        :show-close="false"
        :append-to-body="true"
        :close-on-click-modal="false"
        :close-on-press-escape="false"
        draggable
    >
        <el-form ref="formDataRef" :model="reactiveData.formData" :rules="formRule">
            <el-form-item class="things-dialog-form-item" label="设备名称" prop="deviceName">
                <el-input v-model="reactiveData.formData.deviceName" placeholder="请输入设备名称" clearable></el-input>
            </el-form-item>
            <el-form-item class="things-dialog-form-item" label="所属驱动" prop="driverId">
                <el-select v-model="reactiveData.formData.driverId" class="edit-form-special" placeholder="请选择所属驱动" clearable @visible-change="driverDictionaryVisible">
                    <div class="tool-select">
                        <el-form-item class="tool-select-input">
                            <el-input v-model="reactiveData.driverQuery" placeholder="请选择所属驱动" clearable @input="driverDictionary" />
                        </el-form-item>
                        <el-pagination
                            class="tool-select-pagination"
                            :hide-on-single-page="true"
                            layout="prev, pager, next"
                            :pager-count="5"
                            :page-size="+reactiveData.driverPage.size"
                            :current-page="+reactiveData.driverPage.current"
                            :total="+reactiveData.driverPage.total"
                            small
                            background
                            @current-change="driverCurrentChange"
                        ></el-pagination>
                    </div>
                    <el-option v-for="dictionary in reactiveData.driverDictionary" :key="dictionary.value" :label="dictionary.label" :value="dictionary.value"></el-option>
                </el-select>
            </el-form-item>
            <el-form-item class="things-dialog-form-item" label="关联模板" prop="profileIds">
                <el-select
                    v-model="reactiveData.formData.profileIds"
                    class="edit-form-special"
                    placeholder="请选择关联模板"
                    :multiple="true"
                    clearable
                    @visible-change="profileDictionaryVisible"
                >
                    <div class="tool-select">
                        <el-form-item class="tool-select-input">
                            <el-input v-model="reactiveData.profileQuery" placeholder="请选择关联模板" clearable @input="profileDictionary" />
                        </el-form-item>
                        <el-pagination
                            class="tool-select-pagination"
                            :hide-on-single-page="true"
                            layout="prev, pager, next"
                            :pager-count="5"
                            :page-size="+reactiveData.profilePage.size"
                            :current-page="+reactiveData.profilePage.current"
                            :total="+reactiveData.profilePage.total"
                            small
                            background
                            @current-change="profileCurrentChange"
                        ></el-pagination>
                    </div>
                    <el-option v-for="dictionary in reactiveData.profileDictionary" :key="dictionary.value" :label="dictionary.label" :value="dictionary.value"></el-option>
                </el-select>
            </el-form-item>
            <el-form-item class="things-dialog-form-item" label="描述" prop="remark">
                <el-input v-model="reactiveData.formData.remark" placeholder="请输入设备描述" type="textarea" maxlength="300" show-word-limit clearable></el-input>
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

<style lang="scss">
@import '@/components/dialog/styles/things-dialog';
@import '@/components/card/styles/tool-card';
</style>
