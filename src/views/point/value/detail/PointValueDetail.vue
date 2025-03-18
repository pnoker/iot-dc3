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
        v-model="detailVisible"
        :append-to-body="true"
        :close-on-click-modal="false"
        :close-on-press-escape="false"
        :show-close="false"
        class="things-dialog"
        draggable
        title="数据信息"
    >
        <pre v-highlightjs>
            <code class="json">{{ detailData }}</code>
        </pre>
        <div>
            <el-button type="success" @click="detailVisible = false">关闭</el-button>
        </div>
    </el-dialog>
</template>

<script lang="ts" src="./index.ts" />

<style lang="scss">
    @use '@/components/dialog/styles/things-dialog';
</style>
