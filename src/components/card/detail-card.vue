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
    <div class="detail-card">
        <el-card shadow="hover">
            <slot/>
        </el-card>
    </div>
</template>

<script>
export default {
    name: "detail-card"
}
</script>

<style lang="scss">
.detail-card {
    margin-left: 3px;
    margin-right: 3px;
    margin-bottom: 6px;
    border-radius: 5px;
    box-sizing: border-box;

    ul {
        list-style: none;

        li {
            font-size: 13px;
            margin-top: 8px;
        }
    }

    .el-card {
        width: 100%;
        box-sizing: border-box;
    }

    .el-card__body {
        padding: 10px 3px;
    }
}
</style>
