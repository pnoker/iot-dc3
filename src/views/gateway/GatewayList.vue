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
        <el-row>
            <gateway-tool
                :page="page"
                @search="search"
                @reset="reset"
                @refresh="refresh"
                @sort="sort"
                @size-change="sizeChange"
                @current-change="currentChange"
            ></gateway-tool>
        </el-row>

        <el-row>
            <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4" :key="data.id" v-for="data in 12">
                <skeleton-card :loading="loading"></skeleton-card>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4" :key="data.id" v-for="data in listData">
                <gateway-card
                    :data="data"
                    :statusTable="statusTable"
                ></gateway-card>
            </el-col>
        </el-row>
    </div>
</template>
<script>
import skeletonCard from "@/components/card/skeleton-card";
import gatewayTool from "./GatewayTool";
import gatewayCard from "./GatewayCard";
import {driverList, driverStatus} from "@/api/driver";

export default {
    components: {skeletonCard, gatewayTool, gatewayCard},
    data() {
        return {
            loading: true,
            statusTable: {},
            listData: [],
            query: {
                type: "gateway"
            },
            order: false,
            page: {
                total: 0,
                size: 12,
                current: 1
            }
        }
    },
    created() {
        this.list();
    },
    methods: {
        list() {
            driverList({
                page: this.page,
                ...this.query
            }).then(res => {
                const data = res.data;
                this.page.total = data.total;
                this.listData = data.records;
            }).catch(() => {
            }).finally(() => {
                this.loading = false;
            });

            driverStatus({
                page: this.page,
                ...this.query
            }).then(res => {
                this.statusTable = res.data;
            }).catch(() => {
            });
        },
        search(params) {
            this.query = {...params, type: "gateway"};
            this.list();
        },
        reset() {
            this.query = {type: "gateway"};
            this.list();
        },
        refresh() {
            this.list();
        },
        sort() {
            this.order = !this.order;
            if (this.order) {
                this.page.orders = [{column: "create_time", asc: true}];
            } else {
                this.page.orders = [{column: "create_time", asc: false}];
            }
            this.list();
        },
        sizeChange(size) {
            this.page.size = size;
            this.list();
        },
        currentChange(current) {
            this.page.current = current;
            this.list();
        }
    }
}

</script>

<style lang="scss">
</style>
