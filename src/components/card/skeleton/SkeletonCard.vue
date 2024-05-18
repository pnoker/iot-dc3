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
    <el-skeleton :loading="props.loading" animated>
        <template #template>
            <div class="skeleton-card">
                <el-card shadow="hover">
                    <div class="skeleton-card__container">
                        <div class="skeleton-card__header">
                            <el-skeleton-item class="skeleton-card-icon" ariant="image" />
                            <el-skeleton-item class="skeleton-card-name" variant="text" />
                        </div>
                        <div class="skeleton-card__body">
                            <div class="skeleton-card-content">
                                <ul>
                                    <el-skeleton-item class="skeleton-card-text" variant="text" />
                                    <el-skeleton-item class="skeleton-card-text" variant="text" />
                                    <el-skeleton-item class="skeleton-card-text" variant="text" />
                                    <el-skeleton-item class="skeleton-card-text" variant="text" />
                                </ul>
                            </div>
                            <div class="skeleton-card-content">
                                <el-skeleton-item class="skeleton-card-description" variant="text" />
                            </div>
                        </div>
                        <div class="skeleton-card__footer" v-if="!props.footer">
                            <div class="skeleton-card-operation">
                                <el-skeleton-item class="skeleton-card-button" variant="button" />
                                <el-skeleton-item class="skeleton-card-button" variant="button" />
                                <el-skeleton-item class="skeleton-card-button" variant="button" />
                                <el-skeleton-item class="skeleton-card-button" variant="button" />
                                <el-skeleton-item class="skeleton-card-button" variant="button" />
                            </div>
                        </div>
                    </div>
                </el-card>
            </div>
        </template>
    </el-skeleton>
</template>

<script setup lang="ts">
const props = defineProps({
    loading: {
        type: Boolean,
        default: () => {
            return false
        }
    },
    footer: {
        type: Boolean,
        default: () => {
            return false
        }
    }
})
</script>

<style lang="scss">
.skeleton-card {
    border-radius: 4px;
    box-sizing: border-box;

    margin-left: 3px;
    margin-right: 3px;
    margin-bottom: 6px;
}

.skeleton-card__container {
    height: 100%;
    display: flex;
    flex-direction: column;
    justify-content: space-between;

    .skeleton-card__header {
        width: 100%;
        height: 55px;
        display: flex;
        border-bottom: 1px solid #dcdfe6;

        .skeleton-card-icon {
            width: 55px;
            height: 48px;
            margin-right: 12px;
            border-radius: 4px;
            overflow: hidden;

            img {
                width: 100%;
                height: 100%;
            }
        }

        .skeleton-card-name {
            height: 28px;
            margin-top: 10px;
            line-height: 48px;
        }
    }

    .skeleton-card__body {
        display: flex;
        flex-direction: column;

        .skeleton-card-content {
            display: flex;
            justify-content: space-around;

            ul {
                width: 100%;
                padding-inline-start: 25px;
                list-style: none;

                li {
                    font-size: 13px;
                    margin-top: 8px;
                }
            }

            .skeleton-card-text {
                margin-top: 2px;
            }

            .skeleton-card-description {
                margin-top: 10px;
            }
        }
    }

    .skeleton-card__footer {
        height: 35px;
        margin-top: 10px;
        display: flex;
        justify-content: flex-end;
        align-items: center;
        border-top: 1px solid #dcdfe6;

        .skeleton-card-operation {
            display: flex;

            .skeleton-card-button {
                width: 28px;
                height: 20px;
                margin-right: 10px;
            }
        }
    }
}
</style>
