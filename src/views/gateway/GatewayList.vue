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
            <el-col :xs="24" :sm="12" :md="12" :lg="8" :xl="6" :key="data.id" v-for="data in listData">
                <gateway-card
                        :data="data"
                        :statusTable="statusTable"
                ></gateway-card>
            </el-col>
        </el-row>
    </div>
</template>
<script>
    import gatewayTool from './GatewayTool'
    import gatewayCard from './GatewayCard'
    import {driverList, driverStatus} from "@/api/driver";

    export default {
        components: {gatewayTool, gatewayCard},
        data() {
            return {
                statusTable: {},
                listData: [],
                query: {
                    type: 'gateway'
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
                this.query = {...params, type: 'gateway'};
                this.list();
            },
            reset() {
                this.query = {type: 'gateway'};
                this.list();
            },
            refresh() {
                this.list();
            },
            sort() {
                this.order = !this.order;
                if (this.order) {
                    this.page.orders = [{column: 'create_time', asc: true}];
                } else {
                    this.page.orders = [{column: 'create_time', asc: false}];
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
    .home {
        height: 100%;
    }
</style>
