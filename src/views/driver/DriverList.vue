<template>
    <div>
        <el-row>
            <driver-tool
                    :page="page"
                    @search="search"
                    @reset="reset"
                    @refresh="refresh"
                    @sort="sort"
                    @size-change="sizeChange"
                    @current-change="currentChange"
            ></driver-tool>
        </el-row>

        <el-row>
            <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4" :key="data.id" v-for="data in listData">
                <driver-card
                        :data="data"
                        :statusTable="statusTable"
                ></driver-card>
            </el-col>
        </el-row>
    </div>
</template>
<script>
    import driverTool from './DriverTool'
    import driverCard from './DriverCard'
    import {driverList, driverStatus} from "@/api/driver";

    export default {
        components: {driverTool, driverCard},
        data() {
            return {
                statusTable: {},
                listData: [],
                query: {
                    type: 'driver'
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
                this.query = {...params, type: 'driver'};
                this.list();
            },
            reset() {
                this.query = {type: 'driver'};
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
</style>
