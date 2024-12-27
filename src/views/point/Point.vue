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
    <div>
        <point-tool
            :embedded="embedded"
            :next="next"
            :page="reactiveData.page"
            :pre="pre"
            @refresh="refresh"
            @reset="reset"
            @search="search"
            @sort="sort"
            @pre-handle="preHandle"
            @next-handle="nextHandle"
            @show-add="showAdd"
            @size-change="sizeChange"
            @current-change="currentChange"
        ></point-tool>

        <blank-card>
            <el-row>
                <el-col v-for="data in 12" :key="data" :lg="8" :md="12" :sm="12" :xl="6" :xs="24">
                    <skeleton-card :loading="reactiveData.loading"></skeleton-card>
                </el-col>
                <el-col v-if="hasData">
                    <el-empty description="暂无位号数据!"></el-empty>
                </el-col>
                <el-col v-for="data in reactiveData.listData" :key="data.id" :lg="8" :md="12" :sm="12" :xl="6" :xs="24">
                    <point-card
                        :data="data"
                        :embedded="embedded != '' && embedded != 'edit'"
                        :profile="reactiveData.profileTable[data.profileId]"
                        @disable-thing="disableThing"
                        @enable-thing="enableThing"
                        @delete-thing="deleteThing"
                    ></point-card>
                </el-col>
            </el-row>
        </blank-card>

        <point-add-form ref="pointAddFormRef" :profile-id="profileId" @add-thing="addThing"></point-add-form>
    </div>
</template>

<script lang="ts" src="./index.ts" />
