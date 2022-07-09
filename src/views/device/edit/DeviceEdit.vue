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
	<div class="edit-card">
		<div class="edit-card-header">
			<el-card shadow="hover">
				<el-steps :active="reactiveData.active" align-center>
					<el-step title="设备信息编辑"></el-step>
					<el-step title="设备驱动编辑"></el-step>
					<el-step title="设备位号编辑"></el-step>
				</el-steps>
			</el-card>
		</div>

		<div class="edit-card-body">
			<el-card v-if="reactiveData.active === 0" shadow="hover">
				<el-divider content-position="left">设备信息编辑</el-divider>
				<el-form ref="deviceFormRef" :inline="true" :model="reactiveData.deviceFormData" :rules="deviceFormRule">
					<div class="edit-form-item">
						<el-form-item label="设备名称" prop="name">
							<el-input v-model="reactiveData.deviceFormData.name" class="edit-form-default" placeholder="请输入设备名称" clearable @keyup.enter="deviceUpdate"></el-input>
						</el-form-item>
						<el-form-item label="所属驱动" prop="driverId">
							<el-select v-model="reactiveData.deviceFormData.driverId" placeholder="请选择所属驱动" filterable clearable>
								<el-option v-for="dictionary in reactiveData.driverDictionary" :key="dictionary.value" :label="dictionary.label" :value="dictionary.value"></el-option>
							</el-select>
						</el-form-item>
						<el-form-item label="存储类型" prop="multi">
							<el-select v-model="reactiveData.deviceFormData.multi" class="edit-form-medium" placeholder="请选择存储类型" clearable>
								<el-option label="单点数据" :value="false"></el-option>
								<el-option label="结构数据" :value="true"></el-option>
							</el-select>
						</el-form-item>
						<el-form-item label="使能" prop="enable">
							<el-select v-model="reactiveData.deviceFormData.enable" class="edit-form-small" placeholder="请选择使能" clearable>
								<el-option label="启用" :value="true"></el-option>
								<el-option label="停用" :value="false"></el-option>
							</el-select>
						</el-form-item>
					</div>
					<div class="edit-form-item">
						<el-form-item label="包含模板" prop="profileIds">
							<el-select v-model="reactiveData.deviceFormData.profileIds" class="edit-form-large" placeholder="请选择包含模板" :multiple="true" filterable clearable>
								<el-option v-for="dictionary in reactiveData.profileDictionary" :key="dictionary.value" :label="dictionary.label" :value="dictionary.value"></el-option>
							</el-select>
						</el-form-item>
					</div>
					<div class="edit-form-item">
						<el-form-item label="设备描述" prop="description">
							<el-input
								v-model="reactiveData.deviceFormData.description"
								class="edit-form-large"
								placeholder="请输入设备描述"
								type="textarea"
								maxlength="300"
								show-word-limit
								clearable
							></el-input>
						</el-form-item>
					</div>
					<el-form-item class="edit-form-button">
						<el-button type="primary" :icon="Edit" @click="deviceUpdate"> 修改</el-button>
						<el-button :icon="RefreshLeft" @click="deviceReset">恢复</el-button>
						<el-button type="warning" :icon="Right" plain @click="next">下一步</el-button>
					</el-form-item>
				</el-form>
			</el-card>

			<el-card v-if="reactiveData.active === 1 && reactiveData.driverAttributes && reactiveData.driverAttributes.length > 0" shadow="hover">
				<el-divider content-position="left">设备驱动配置</el-divider>
				<el-alert :closable="false" title="设备驱动配置说明" type="success" description="设备驱动配置用于配置连接到该设备所需的基本参数信息。"></el-alert>
				<el-form ref="driverFormRef" :inline="true" :model="reactiveData.driverFormData" :v-if="reactiveData.driverFormData.length > 0">
					<div class="edit-form-item">
						<el-row>
							<el-form-item v-for="attribute in reactiveData.driverAttributes" :key="attribute.id" :label="attribute.displayName" :prop="attribute.name">
								<el-input
									v-if="reactiveData.driverFormData[attribute.name]"
									:key="reactiveData.driverFormData[attribute.name].id"
									v-model="reactiveData.driverFormData[attribute.name].value"
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
						<el-button type="primary" :icon="Edit" @click="driverUpdate">修改</el-button>
						<el-button :icon="RefreshLeft" @click="driverReset">恢复</el-button>
						<el-button type="warning" :icon="Right" plain @click="next">下一步</el-button>
					</el-form-item>
				</el-form>
			</el-card>

			<el-card v-if="reactiveData.active === 2" shadow="hover">
				<el-divider content-position="left">设备位号配置</el-divider>
				<el-alert :closable="false" title="设备位号配置说明" type="success" description="设备位号配置用于配置采集设备该位号的数据所需的基本参数信息。"></el-alert>
				<el-form ref="pointFormRef" :inline="true" :model="reactiveData.pointFormData" :v-if="reactiveData.pointFormData.length > 0">
					<div class="edit-form-item">
						<el-row>
							<el-form-item label="位号名称" prop="name">
								<el-input v-model="reactiveData.pointFormData.name" class="edit-form-default" clearable disabled></el-input>
							</el-form-item>
							<el-form-item v-for="attribute in reactiveData.pointAttributes" :key="attribute.id" :label="attribute.displayName" :prop="attribute.name">
								<el-input
									v-if="reactiveData.pointFormData[attribute.name]"
									:key="reactiveData.pointFormData[attribute.name].id"
									v-model="reactiveData.pointFormData[attribute.name].value"
									class="edit-form-default"
									:placeholder="'请输入' + attribute.displayName"
									clearable
									@keyup.enter="pointUpdate"
								></el-input>
							</el-form-item>
						</el-row>
					</div>
					<el-form-item class="edit-form-button">
						<el-button type="success" :icon="Back" plain @click="pre">上一步</el-button>
						<el-button type="primary" :icon="Edit" :disabled="!Object.keys(reactiveData.pointFormData).length > 0" @click="pointUpdate">修改</el-button>
						<el-button :icon="RefreshLeft" :disabled="!Object.keys(reactiveData.pointFormData).length > 0" @click="pointReset">恢复</el-button>
						<el-button type="warning" :icon="Check" plain @click="next">完成</el-button>
					</el-form-item>
				</el-form>
				<el-row>
					<el-col v-for="data in 12" :key="data.id" :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
						<skeleton-card :loading="reactiveData.loading" :footer="true"></skeleton-card>
					</el-col>
					<el-col v-for="data in reactiveData.pointInfoData" :key="data.id" :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
						<point-info-card :data="data" :attributes="reactiveData.pointAttributes" @select-change="changePointInfo"></point-info-card>
					</el-col>
				</el-row>
			</el-card>
		</div>
	</div>
</template>

<script src="./index.ts" lang="ts"/>

<style lang="less">
@import '~@/components/card/styles/edit-card.less';
</style>
