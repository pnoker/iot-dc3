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
    <div class="things-card cursor-pointer" @click="select(data)">
        <el-card :shadow="data.shadow">
            <div class="things-card-content">
                <div class="things-card__header" v-bind:class="{'header-enable':isConfig,'header-disable':!isConfig}">
                    <div class="things-card-header-icon"><img :src="isSelect" :alt="data.name"></div>
                    <div class="things-card-header-name nowrap-name">{{ data.name }}</div>
                </div>
                <div class="things-card__body">
                    <div class="things-card-body-content">
                        <ul>
                            <li class="nowrap-item" :key="attribute.id" v-if="attribute" v-for="attribute in attributes">
                                <span><i class="el-icon-goblet"/> {{ attribute.displayName }}: </span>{{ data[attribute.name].value }}
                            </li>
                        </ul>
                    </div>
                </div>
            </div>
        </el-card>
    </div>
</template>

<script>

export default {
    name: "point-info-card",
    props: {
        data: {
            type: Object,
            default: () => {
                return {
                    name: "",
                    description: "",
                    createTime: "",
                    updateTime: ""
                };
            }
        },
        attributes: {
            type: Array,
            default: []
        },
        icon: {
            type: String,
            default: "images/common/point-info-disable.png"
        }
    },
    data() {
        return {}
    },
    computed: {
        isConfig() {
            for (let attribute of this.attributes) {
                if (this.data[attribute.name].value === "") {
                    return false;
                }
            }
            return true;
        },
        isSelect() {
            if (this.data.shadow === "always") {
                return "images/common/point-info.png";
            } else {
                return "images/common/point-info-disable.png";
            }
        }
    },
    methods: {
        select(data) {
            this.$emit("select-change", data);
        }
    }
};
</script>

<style lang="scss">
@import "~@/components/card/styles/things-card.scss";
</style>
