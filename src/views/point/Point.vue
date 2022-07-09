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
		<point-tool
			:pre="pre"
			:next="next"
			:page="reactiveData.page"
			@pre-handle="preHandle"
			@next-handle="nextHandle"
			@search="search"
			@reset="reset"
			@show-add="showAdd"
			@refresh="refresh"
			@sort="sort"
			@size-change="sizeChange"
			@current-change="currentChange"
		></point-tool>

		<el-row>
			<el-col v-for="data in 12" :key="data.id" :xs="24" :sm="12" :md="12" :lg="8" :xl="6">
				<skeleton-card :loading="reactiveData.loading"></skeleton-card>
			</el-col>
			<el-col v-if="hasData">
				<el-empty description="暂无位号数据！"></el-empty>
			</el-col>
			<el-col v-for="data in reactiveData.listData" :key="data.id" :xs="24" :sm="12" :md="12" :lg="8" :xl="6">
				<point-card :data="data" :profile="reactiveData.profileTable[data.profileId]" @delete-thing="deleteThing"></point-card>
			</el-col>
		</el-row>

		<point-add-form ref="pointAddFormRef" :profile-id="profileId" @add-thing="addThing"></point-add-form>
	</div>
</template>

<script src="./index.ts" lang="ts"/>
