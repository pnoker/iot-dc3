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
                <el-steps :active="reactiveData.active" align-center finish-status="success">
                    <el-step title="设备信息配置"></el-step>
                    <el-step title="驱动属性配置"></el-step>
                    <el-step title="位号属性配置"></el-step>
                    <el-step title="设备配置完成"></el-step>
                </el-steps>
            </el-card>
        </div>

        <div class="edit-card-body">
            <el-card v-if="reactiveData.active === 0" shadow="hover">
                <el-divider content-position="left">设备信息配置</el-divider>
                <el-form ref="deviceFormRef" :inline="true" :model="reactiveData.deviceFormData" :rules="deviceFormRule">
                    <div class="edit-form-item">
                        <el-form-item class="edit-form-large" label="设备名称" prop="deviceName">
                            <el-input v-model="reactiveData.deviceFormData.deviceName" clearable placeholder="请输入设备名称"></el-input>
                        </el-form-item>
                    </div>
                    <div class="edit-form-item">
                        <el-form-item class="edit-form-large" label="所属驱动" prop="driverId">
                            <el-select
                                v-model="reactiveData.deviceFormData.driverId"
                                class="edit-form-large"
                                clearable
                                placeholder="请选择所属驱动"
                                @change="changeAttribute"
                                @visible-change="driverDictionaryVisible"
                            >
                                <div class="tool-select">
                                    <el-form-item class="tool-select-input">
                                        <el-input v-model="reactiveData.driverQuery" clearable placeholder="请输入驱动名称" @input="driverDictionary" />
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
                                        @current-change="driverCurrentChange"
                                    ></el-pagination>
                                </div>
                                <el-option
                                    v-for="dictionary in reactiveData.driverDictionary"
                                    :key="dictionary.value"
                                    :label="dictionary.label"
                                    :value="dictionary.value"
                                ></el-option>
                            </el-select>
                        </el-form-item>
                    </div>
                    <div class="edit-form-item">
                        <el-form-item class="edit-form-large" label="使能" prop="enableFlag">
                            <el-select v-model="reactiveData.deviceFormData.enableFlag" class="edit-form-large" clearable placeholder="请选择使能">
                                <el-option label="启用" value="ENABLE"></el-option>
                                <el-option label="停用" value="DISABLE"></el-option>
                            </el-select>
                        </el-form-item>
                    </div>
                    <div class="edit-form-item">
                        <el-form-item class="edit-form-large" label="关联模板" prop="profileIds">
                            <el-select
                                v-model="reactiveData.deviceFormData.profileIds"
                                :multiple="true"
                                class="edit-form-large"
                                clearable
                                placeholder="请选择关联模板"
                                @visible-change="profileDictionaryVisible"
                            >
                                <div class="tool-select">
                                    <el-form-item class="tool-select-input">
                                        <el-input v-model="reactiveData.profileQuery" clearable placeholder="请输入驱动名称" @input="profileDictionary" />
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
                                        @current-change="profileCurrentChange"
                                    ></el-pagination>
                                </div>
                                <el-option
                                    v-for="dictionary in reactiveData.profileDictionary"
                                    :key="dictionary.value"
                                    :label="dictionary.label"
                                    :value="dictionary.value"
                                ></el-option>
                            </el-select>
                        </el-form-item>
                    </div>
                    <div class="edit-form-item">
                        <el-form-item class="edit-form-large" label="设备描述" prop="remark">
                            <el-input
                                v-model="reactiveData.deviceFormData.remark"
                                clearable
                                maxlength="300"
                                placeholder="请输入设备描述"
                                show-word-limit
                                type="textarea"
                            ></el-input>
                        </el-form-item>
                    </div>
                    <el-form-item class="edit-form-button">
                        <el-button :icon="Back" plain type="success" @click="done">返回</el-button>
                        <el-button :icon="RefreshLeft" @click="deviceReset">恢复</el-button>
                        <el-button :icon="Right" plain type="warning" @click="next">下一步</el-button>
                    </el-form-item>
                </el-form>
            </el-card>

            <el-card v-if="reactiveData.active === 1 && reactiveData.driverAttributes && reactiveData.driverAttributes.length > 0" shadow="hover">
                <el-divider content-position="left">驱动属性配置</el-divider>
                <el-alert :closable="false" description="驱动属性配置用于配置连接到该设备所需的基本参数信息。" title="驱动属性配置说明" type="success"></el-alert>
                <el-form ref="driverFormRef" :inline="true" :model="reactiveData.driverFormData" :v-if="reactiveData.driverFormData.length > 0">
                    <div class="edit-form-item">
                        <el-row>
                            <el-form-item v-for="attribute in reactiveData.driverAttributes" :key="attribute.id" :label="attribute.attributeName" :prop="attribute.attributeCode">
                                <el-input
                                    v-if="reactiveData.driverFormData[attribute.attributeCode]"
                                    :key="reactiveData.driverFormData[attribute.attributeCode].id"
                                    v-model="reactiveData.driverFormData[attribute.attributeCode].configValue"
                                    :placeholder="'请输入' + attribute.attributeName"
                                    class="edit-form-default"
                                    clearable
                                    @keyup.enter="driverUpdate"
                                ></el-input>
                            </el-form-item>
                        </el-row>
                    </div>
                    <el-form-item class="edit-form-button">
                        <el-button :icon="Back" plain type="success" @click="pre">上一步</el-button>
                        <el-button :icon="RefreshLeft" @click="driverInfoReset">恢复</el-button>
                        <el-button :icon="Right" plain type="warning" @click="next">下一步</el-button>
                    </el-form-item>
                </el-form>
            </el-card>

            <el-card v-if="reactiveData.active === 2" shadow="hover">
                <el-divider content-position="left">位号属性配置</el-divider>
                <el-alert :closable="false" description="位号属性配置用于配置采集设备该位号的数据所需的基本参数信息。" title="位号属性配置说明" type="success"></el-alert>
                <el-form ref="pointFormRef" :inline="true" :model="reactiveData.pointFormData" :v-if="reactiveData.pointFormData.length > 0">
                    <div class="edit-form-item">
                        <el-form-item label="位号名称" prop="pointName">
                            <el-input v-model="reactiveData.pointFormData.pointName" class="edit-form-default" disabled></el-input>
                        </el-form-item>
                    </div>
                    <div class="edit-form-item">
                        <el-row>
                            <el-form-item v-for="attribute in reactiveData.pointAttributes" :key="attribute.id" :label="attribute.attributeName" :prop="attribute.attributeCode">
                                <el-input
                                    v-if="reactiveData.pointFormData[attribute.attributeCode]"
                                    :key="reactiveData.pointFormData[attribute.attributeCode].id"
                                    v-model="reactiveData.pointFormData[attribute.attributeCode].configValue"
                                    :placeholder="'请输入' + attribute.attributeName"
                                    class="edit-form-default"
                                    clearable
                                    @keyup.enter="pointUpdate"
                                ></el-input>
                                <el-input v-else class="edit-form-default" disabled></el-input>
                            </el-form-item>
                        </el-row>
                    </div>
                </el-form>
                <el-form-item class="edit-form-button">
                    <el-button :icon="Back" plain type="success" @click="pre">上一步</el-button>
                    <el-button :disabled="!hasPointFormData" :icon="Edit" type="primary" @click="pointUpdate"> 修改 </el-button>
                    <el-button :disabled="!hasPointFormData" :icon="RefreshLeft" @click="pointInfoReset"> 恢复 </el-button>
                    <el-button :icon="Check" plain type="warning" @click="next">下一步</el-button>
                </el-form-item>
                <el-row>
                    <el-col v-for="data in 12" :key="data" :lg="6" :md="8" :sm="12" :xl="4" :xs="24">
                        <skeleton-card :footer="false" :loading="reactiveData.loading"></skeleton-card>
                    </el-col>
                    <el-col v-for="data in reactiveData.pointInfoData" :key="data.id" :lg="6" :md="8" :sm="12" :xl="4" :xs="24">
                        <point-info-card :attributes="reactiveData.pointAttributes" :data="data" @select="selectPoint"></point-info-card>
                    </el-col>
                </el-row>
            </el-card>
            <el-card v-if="reactiveData.active === 3" shadow="hover">
                <el-divider content-position="left">设备配置完成</el-divider>
                <el-result icon="success" sub-title="您可以返回进行下一步操作" title="配置完成">
                    <template #extra>
                        <el-button plain type="primary" @click="done">返回</el-button>
                    </template>
                </el-result>
            </el-card>
        </div>
    </div>
</template>

<script lang="ts" src="./index.ts" />

<style lang="scss">
    @use '@/components/card/styles/edit-card';
    @use '@/components/card/styles/tool-card';
</style>
