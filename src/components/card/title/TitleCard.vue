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
    <div class="title-card">
        <el-card shadow="hover">
            <div class="title-card__container">
                <slot name="header">
                    <span class="title-card__header">{{ props.title }}</span>
                </slot>
                <slot />
            </div>
        </el-card>
    </div>
</template>

<script lang="ts" setup>
const props = defineProps({
    title: {
        type: String,
        default: ''
    }
})
</script>

<style lang="scss">
.title-card {
    border-radius: 4px;
    box-sizing: border-box;

    .title-card__header {
        font-size: 14px;
        font-weight: bold;
    }

    &:first-child {
        padding-top: 0;
        padding-left: 0;
    }

    .el-card {
        width: 100%;
        box-sizing: border-box;
    }

    .el-card__header {
        padding: 10px;
    }

    .el-card__body {
        padding: 10px 5px 0px 5px;
    }

    .el-tabs__nav {
        margin: 0 5px 0 5px;
    }
}
</style>
