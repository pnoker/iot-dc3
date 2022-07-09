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
	<div>
		<driver-tool :page="reactiveData.page" @search="search" @reset="reset" @refresh="refresh" @sort="sort" @size-change="sizeChange" @current-change="currentChange"></driver-tool>
		<el-row class="detail-content" :gutter="3">
			<el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
				<blank-card class="detail-content-left">
					<el-scrollbar>
						<el-row>
							<el-col v-for="data in 12" :key="data.id">
								<skeleton-card :loading="reactiveData.driverLoading"></skeleton-card>
							</el-col>
							<el-col v-if="hasDriverData">
								<el-empty description="暂无驱动数据！"></el-empty>
							</el-col>
							<el-col v-for="data in reactiveData.listDriverData" :key="data.id">
								<driver-card :data="data" :status-table="reactiveData.driverStatusTable" :footer="true" @select-change="selectChange"></driver-card>
							</el-col>
						</el-row>
					</el-scrollbar>
				</blank-card>
			</el-col>
			<el-col :xs="24" :sm="12" :md="16" :lg="18" :xl="20">
				<base-card>
					<el-tabs v-model="reactiveData.active" @tab-click="changeActive">
						<el-tab-pane label="驱动详情" name="detail">
							<detail-card>
								<ul>
									<li>
										<el-icon>
											<Position />
										</el-icon>
										驱动名称: {{ reactiveData.data.name }}
									</li>
									<li>
										<el-icon>
											<Management />
										</el-icon>
										关联设备 [{{ reactiveData.listDeviceData.length }} 个]: {{ deviceName }}
									</li>
									<li class="nowrap-item">
										<el-icon>
											<Connection />
										</el-icon>
										端口: {{ reactiveData.data.port }}
									</li>
									<li class="nowrap-item">
										<el-icon>
											<Monitor />
										</el-icon>
										主机: {{ reactiveData.data.host }}
									</li>
									<li class="nowrap-item">
										<el-icon>
											<Promotion />
										</el-icon>
										驱动服务: {{ reactiveData.data.serviceName }}
									</li>
									<li>
										<el-icon>
											<Edit />
										</el-icon>
										修改日期: {{ timestamp(reactiveData.data.createTime) }}
									</li>
									<li>
										<el-icon>
											<Sunset />
										</el-icon>
										创建日期: {{ timestamp(reactiveData.data.updateTime) }}
									</li>
								</ul>
							</detail-card>
						</el-tab-pane>
						<el-tab-pane label="关联设备" name="device">
							<el-row>
								<el-col v-for="data in 12" :key="data.id" :xs="24" :sm="24" :md="12" :lg="8" :xl="6">
									<skeleton-card :loading="reactiveData.deviceLoading" :footer="true"></skeleton-card>
								</el-col>
								<el-col v-if="hasDeviceData">
									<el-empty description="暂无关联的设备数据！"></el-empty>
								</el-col>
								<el-col v-for="data in reactiveData.listDeviceData" :key="data.id" :xs="24" :sm="24" :md="12" :lg="8" :xl="6">
									<device-card
										:data="data"
										:driver="reactiveData.driverTable[data.driverId]"
										:profile-table="reactiveData.profileTable"
										:status-table="reactiveData.deviceStatusTable"
										:embedded="true"
									></device-card>
								</el-col>
							</el-row>
						</el-tab-pane>
						<el-tab-pane label="驱动模型" name="model">
							<el-empty description="暂无驱动模型数据！"></el-empty>
						</el-tab-pane>
						<el-tab-pane label="驱动事件" name="event">
							<el-empty description="暂无驱动事件数据！"></el-empty>
						</el-tab-pane>
					</el-tabs>
				</base-card>
			</el-col>
		</el-row>
	</div>
</template>

<script src="./index.ts" lang="ts"/>

<style lang="less">
.detail-content {
	margin-left: 0 !important;
	margin-right: 0 !important;

	.detail-content-left {
		.el-scrollbar {
			height: calc(100vh - 273px);
		}
	}
}
</style>
