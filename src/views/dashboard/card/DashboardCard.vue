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
    <div class="things-card things-card-dashboard">
        <el-card shadow="hover">
            <div class="things-card-content">
                <div class="things-card__body">
                    <div class="things-card-body-content">
                        <div class="things-card-body-content-dashboard-snapshot" @click="show()">
                            <img :src="snapshot" />
                        </div>
                    </div>
                </div>
                <div class="things-card__footer">
                    <div class="things-card-footer-title">
                        <span class="things-card-body-content-dashboard-name">{{ name }}</span>
                    </div>
                </div>
            </div>
        </el-card>
    </div>
</template>

<script>
import { encode } from 'js-base64'

export default {
    name: 'DashboardCard',
    props: {
        name: {
            type: String,
            default: '数据看板',
        },
        snapshot: {
            type: String,
            default: 'images/dashboard/dashboard.jpg',
        },
        url: {
            type: String,
            default: '',
        },
    },
    methods: {
        show() {
            if (this.url) {
                this.$router.push({ name: 'dashboard', query: { id: encode(this.url) } }).catch(() => {
                    // nothing to do
                })
            }
        },
    },
}
</script>

<style lang="scss">
@import '@/components/card/styles/things-card.scss';

.things-card-dashboard {
    width: 300px;
    height: 210px;
    margin-right: 10px;
    margin-bottom: 10px;

    .el-card {
        width: 100%;
        min-width: 300px;
        min-height: 210px !important;
        box-sizing: border-box;
    }
}

.things-card-body-content-dashboard-snapshot {
    width: 280px;
    min-width: 200px;
    max-width: 280px;
    cursor: pointer;

    img {
        width: 280px;
        min-width: 200px;
        max-width: 280px;
    }
}

.things-card-body-content-dashboard-name {
    display: block;
    width: 280px;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
}
</style>
