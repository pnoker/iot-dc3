<!--
  - Copyright 2016-present the original author or authors.
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
                        <el-form-item label="设备名称" prop="deviceName" class="edit-form-large">
                            <el-input v-model="reactiveData.deviceFormData.deviceName" placeholder="请输入设备名称" clearable></el-input>
                        </el-form-item>
                    </div>
                    <div class="edit-form-item">
                        <el-form-item label="所属驱动" prop="driverId" class="edit-form-large">
                            <el-select
                                v-model="reactiveData.deviceFormData.driverId"
                                class="edit-form-large"
                                placeholder="请选择所属驱动"
                                clearable
                                @change="changeAttribute"
                                @visible-change="driverDictionaryVisible"
                            >
                                <div class="tool-select">
                                    <el-form-item class="tool-select-input">
                                        <el-input v-model="reactiveData.driverQuery" placeholder="请输入驱动名称" clearable @input="driverDictionary" />
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
                        <el-form-item label="使能" prop="enableFlag" class="edit-form-large">
                            <el-select v-model="reactiveData.deviceFormData.enableFlag" class="edit-form-large" placeholder="请选择使能" clearable>
                                <el-option label="启用" value="ENABLE"></el-option>
                                <el-option label="停用" value="DISABLE"></el-option>
                            </el-select>
                        </el-form-item>
                    </div>
                    <div class="edit-form-item">
                        <el-form-item label="关联模板" prop="profileIds" class="edit-form-large">
                            <el-select
                                v-model="reactiveData.deviceFormData.profileIds"
                                class="edit-form-large"
                                placeholder="请选择关联模板"
                                :multiple="true"
                                clearable
                                @visible-change="profileDictionaryVisible"
                            >
                                <div class="tool-select">
                                    <el-form-item class="tool-select-input">
                                        <el-input v-model="reactiveData.profileQuery" placeholder="请输入驱动名称" clearable @input="profileDictionary" />
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
                        <el-form-item label="设备描述" prop="remark" class="edit-form-large">
                            <el-input
                                v-model="reactiveData.deviceFormData.remark"
                                placeholder="请输入设备描述"
                                type="textarea"
                                maxlength="300"
                                show-word-limit
                                clearable
                            ></el-input>
                        </el-form-item>
                    </div>
                    <el-form-item class="edit-form-button">
                        <el-button type="success" :icon="Back" plain @click="done">返回</el-button>
                        <el-button :icon="RefreshLeft" @click="deviceReset">恢复</el-button>
                        <el-button type="warning" :icon="Right" plain @click="next">下一步</el-button>
                    </el-form-item>
                </el-form>
            </el-card>

            <el-card v-if="reactiveData.active === 1 && reactiveData.driverAttributes && reactiveData.driverAttributes.length > 0" shadow="hover">
                <el-divider content-position="left">驱动属性配置</el-divider>
                <el-alert :closable="false" title="驱动属性配置说明" type="success" description="驱动属性配置用于配置连接到该设备所需的基本参数信息。"></el-alert>
                <el-form ref="driverFormRef" :inline="true" :model="reactiveData.driverFormData" :v-if="reactiveData.driverFormData.length > 0">
                    <div class="edit-form-item">
                        <el-row>
                            <el-form-item v-for="attribute in reactiveData.driverAttributes" :key="attribute.id" :label="attribute.displayName" :prop="attribute.attributeName">
                                <el-input
                                    v-if="reactiveData.driverFormData[attribute.attributeName]"
                                    :key="reactiveData.driverFormData[attribute.attributeName].id"
                                    v-model="reactiveData.driverFormData[attribute.attributeName].configValue"
                                    class="edit-form-default"
                                    :placeholder="'请输入' + attribute.displayName"
                                    clearable
                                    @keyup.enter="driverUpdate"
                                ></el-input>
                            </el-form-item>
                        </el-row>
                    </div>
                    <el-form-item class="edit-form-button">
                        <el-button type="success" :icon="Back" plain @click="pre">上一步</el-button>
                        <el-button :icon="RefreshLeft" @click="driverInfoReset">恢复</el-button>
                        <el-button type="warning" :icon="Right" plain @click="next">下一步</el-button>
                    </el-form-item>
                </el-form>
            </el-card>

            <el-card v-if="reactiveData.active === 2" shadow="hover">
                <el-divider content-position="left">位号属性配置</el-divider>
                <el-alert :closable="false" title="位号属性配置说明" type="success" description="位号属性配置用于配置采集设备该位号的数据所需的基本参数信息。"></el-alert>
                <el-form ref="pointFormRef" :inline="true" :model="reactiveData.pointFormData" :v-if="reactiveData.pointFormData.length > 0">
                    <div class="edit-form-item">
                        <el-form-item label="位号名称" prop="pointName">
                            <el-input v-model="reactiveData.pointFormData.pointName" class="edit-form-default" disabled></el-input>
                        </el-form-item>
                    </div>
                    <div class="edit-form-item">
                        <el-row>
                            <el-form-item v-for="attribute in reactiveData.pointAttributes" :key="attribute.id" :label="attribute.displayName" :prop="attribute.attributeName">
                                <el-input
                                    v-if="reactiveData.pointFormData[attribute.attributeName]"
                                    :key="reactiveData.pointFormData[attribute.attributeName].id"
                                    v-model="reactiveData.pointFormData[attribute.attributeName].configValue"
                                    class="edit-form-default"
                                    :placeholder="'请输入' + attribute.displayName"
                                    clearable
                                    @keyup.enter="pointUpdate"
                                ></el-input>
                                <el-input v-else class="edit-form-default" disabled></el-input>
                            </el-form-item>
                        </el-row>
                    </div>
                </el-form>
                <el-form-item class="edit-form-button">
                    <el-button type="success" :icon="Back" plain @click="pre">上一步</el-button>
                    <el-button type="primary" :icon="Edit" :disabled="!hasPointFormData" @click="pointUpdate"> 修改 </el-button>
                    <el-button :icon="RefreshLeft" :disabled="!hasPointFormData" @click="pointInfoReset"> 恢复 </el-button>
                    <el-button type="warning" :icon="Check" plain @click="next">下一步</el-button>
                </el-form-item>
                <el-row>
                    <el-col v-for="data in 12" :key="data" :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
                        <skeleton-card :loading="reactiveData.loading" :footer="true"></skeleton-card>
                    </el-col>
                    <el-col v-for="data in reactiveData.pointInfoData" :key="data.id" :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
                        <point-info-card :data="data" :attributes="reactiveData.pointAttributes" @select="selectPoint"></point-info-card>
                    </el-col>
                </el-row>
            </el-card>
            <el-card v-if="reactiveData.active === 3" shadow="hover">
                <el-divider content-position="left">设备配置完成</el-divider>
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
@import '@/components/card/styles/edit-card';
@import '@/components/card/styles/tool-card';
</style>
