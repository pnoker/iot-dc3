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
    <div class="things-card cursor-pointer" @click="select(data)">
        <el-card :shadow="data.shadow">
            <div class="things-card-content">
                <div
                    class="things-card__header"
                    :class="{
                        'header-enable': isConfig,
                        'header-disable': !isConfig,
                    }"
                >
                    <div class="things-card-header-icon">
                        <img :src="isSelect" :alt="data.name" />
                    </div>
                    <div class="things-card-header-name nowrap-name">{{ data.name }}</div>
                </div>
                <div class="things-card__body">
                    <div class="things-card-body-content">
                        <ul>
                            <li v-for="attribute in attributes" :key="attribute.id" class="nowrap-item">
                                <el-icon>
                                    <Goblet />
                                </el-icon>
                                {{ attribute.displayName }}: {{ data[attribute.name].value }}
                            </li>
                        </ul>
                    </div>
                </div>
            </div>
        </el-card>
    </div>
</template>

<script src="./index.ts" lang="ts" />

<style lang="scss">
@import '~@/components/card/styles/things-card.scss';
</style>
