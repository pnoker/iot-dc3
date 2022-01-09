<template>
    <div>
        <el-row>
            <device-tool
                    :page="page"
                    :driverDictionary="driverDictionary"
                    @search="search"
                    @reset="reset"
                    @showAdd="showAdd"
                    @refresh="refresh"
                    @sort="sort"
                    @size-change="sizeChange"
                    @current-change="currentChange"
            ></device-tool>
        </el-row>

        <blank-card>
            <el-row>
                <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4" :key="data.id" v-for="data in 12">
                    <skeleton-card :loading="loading"></skeleton-card>
                </el-col>
                <el-empty v-if="listData.length<1" description="暂无设备数据！"></el-empty>
                <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4" :key="data.id" v-for="data in listData">
                    <device-card
                            :data="data"
                            :driverTable="driverTable"
                            :profileTable="profileTable"
                            :statusTable="statusTable"
                            @delete-thing="deleteThing"
                    ></device-card>
                </el-col>
            </el-row>
        </blank-card>

        <device-add-form
                ref="add-form"
                :driverDictionary="driverDictionary"
                :profileDictionary="profileDictionary"
                @add-thing="addThing"
        ></device-add-form>
    </div>
</template>
<script>
    import blankCard from '@/components/card/blank-card';
    import skeletonCard from '@/components/card/skeleton-card';
    import deviceTool from './DeviceTool';
    import deviceAddForm from './DeviceAddForm';
    import deviceCard from './DeviceCard';
    import {deviceAdd, deviceDelete, deviceList, deviceStatus} from "@/api/device";
    import {driverDictionary, profileDictionary} from "@/api/dictionary";

    export default {
        components: {blankCard, skeletonCard, deviceTool, deviceAddForm, deviceCard},
        data() {
            return {
                loading: true,
                driverDictionary: [],
                profileDictionary: [],
                driverTable: {},
                profileTable: {},
                statusTable: {},
                listData: [],
                query: {},
                order: false,
                page: {
                    total: 0,
                    size: 12,
                    current: 1
                }
            }
        },
        created() {
            this.driver();
            this.profile();
            this.list();
        },
        methods: {
            list() {
                deviceList({
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

                deviceStatus({
                    page: this.page,
                    ...this.query
                }).then(res => {
                    this.statusTable = res.data;
                }).catch(() => {
                });
            },
            driver() {
                driverDictionary().then(res => {
                    this.driverDictionary = res.data;
                    this.driverTable = this.driverDictionary.reduce((pre, cur) => {
                        pre[cur.value] = cur.label;
                        return pre;
                    }, {});
                }).catch(() => {
                });
            },
            profile() {
                profileDictionary().then(res => {
                    this.profileDictionary = res.data;
                    this.profileTable = this.profileDictionary.reduce((pre, cur) => {
                        pre[cur.value] = cur.label;
                        return pre;
                    }, {});
                }).catch(() => {
                });
            },
            search(params) {
                this.query = params;
                this.list();
            },
            reset() {
                this.query = {};
                this.list();
            },
            showAdd() {
                this.$refs['add-form'].show();
            },
            addThing(form, done) {
                deviceAdd(form).then(() => {
                    this.list();
                    done();
                }).catch(() => {
                });
            },
            deleteThing(id, done) {
                deviceDelete(id).then(() => {
                    this.list();
                    done();
                }).catch(() => {
                });
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
