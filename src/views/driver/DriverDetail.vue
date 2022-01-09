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
        <el-row :gutter="3">
            <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
                <blank-card>
                    <el-row>
                        <el-col :key="data.id" v-for="data in 12">
                            <skeleton-card :loading="driverLoading"></skeleton-card>
                        </el-col>
                        <el-col>
                            <el-empty v-if="listDriverData.length<1" description="暂无驱动数据！"></el-empty>
                        </el-col>
                        <el-col :key="data.id" v-for="data in listDriverData">
                            <driver-card
                                    :data="data"
                                    :statusTable="driverStatusTable"
                                    :footer="true"
                                    @select-change="selectChange"
                            ></driver-card>
                        </el-col>
                    </el-row>
                </blank-card>
            </el-col>
            <el-col :xs="24" :sm="12" :md="16" :lg="18" :xl="20">
                <base-card>
                    <el-tabs v-model="active" @tab-click="changeActive">
                        <el-tab-pane label="驱动详情" name="detail">
                            <detail-card>
                                <ul>
                                    <li><i class="el-icon-data-line"></i> 驱动名称: {{data.name}}</li>
                                    <li><i class="el-icon-collection-tag"></i> 关联设备 [{{listDeviceData.length||0}} 个]: {{deviceName()}}</li>
                                    <li class="nowrap-item"><span><i class="el-icon-connection"/> 端口: </span>{{data.port}}</li>
                                    <li class="nowrap-item"><span><i class="el-icon-monitor"/> 主机: </span>{{data.host}}</li>
                                    <li class="nowrap-item"><span><i class="el-icon-s-promotion"/> 驱动服务: </span>{{data.serviceName}}</li>
                                    <li><i class="el-icon-edit-outline"></i> 修改日期: {{timestamp(data.createTime)}}</li>
                                    <li><i class="el-icon-sunset"></i> 创建日期: {{timestamp(data.updateTime)}}</li>
                                </ul>
                            </detail-card>
                        </el-tab-pane>
                        <el-tab-pane label="关联设备" name="device">
                            <el-row>
                                <el-col :xs="24" :sm="24" :md="12" :lg="8" :xl="6" :key="data.id" v-for="data in 12">
                                    <skeleton-card :loading="deviceLoading" :footer="true"></skeleton-card>
                                </el-col>
                                <el-col>
                                    <el-empty v-if="listDeviceData.length<1" description="暂无关联的设备数据！"></el-empty>
                                </el-col>
                                <el-col :xs="24" :sm="24" :md="12" :lg="8" :xl="6" :key="data.id" v-for="data in listDeviceData">
                                    <device-card
                                            :data="data"
                                            :driverTable="driverTable"
                                            :profileTable="profileTable"
                                            :statusTable="deviceStatusTable"
                                            :embedded="true"
                                    ></device-card>
                                </el-col>
                            </el-row>
                        </el-tab-pane>
                        <el-tab-pane label="驱动模型" name="model">
                            <el-empty description="暂无驱动模型数据！"></el-empty>
                        </el-tab-pane>
                        <el-tab-pane label="驱动事件" name="event">
                            <el-empty description="暂无驱动事件数据！"></el-empty>
                        </el-tab-pane>
                    </el-tabs>
                </base-card>
            </el-col>
        </el-row>
    </div>
</template>
<script>
    import blankCard from '@/components/card/blank-card';
    import baseCard from '@/components/card/base-card';
    import detailCard from '@/components/card/detail-card';
    import skeletonCard from '@/components/card/skeleton-card';
    import driverTool from './DriverTool';
    import deviceList from '../device/DeviceList';
    import driverCard from './DriverCard';
    import deviceCard from '../device/DeviceCard';
    import pointCard from '../point/PointCard';
    import {dateFormat, setCopyContent} from "@/util/util";
    import {deviceByDriverId, deviceStatusByDriverId} from "@/api/device";
    import {driverDictionary, profileDictionary} from "@/api/dictionary";
    import {driverById, driverList, driverStatus} from "@/api/driver";

    export default {
        components: {blankCard, baseCard, detailCard, skeletonCard, driverTool, deviceList, driverCard, deviceCard, pointCard},
        data() {
            return {
                id: this.$route.query.id,
                active: this.$route.query.active,
                driverLoading: true,
                deviceLoading: true,
                driverTable: {},
                profileTable: {},
                driverStatusTable: {},
                deviceStatusTable: {},
                data: {},
                listDriverData: [],
                listDeviceData: [],
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
            this.driver();
            this.device();
            this.drivers();
            this.profiles();
            this.list();
        },
        methods: {
            driver() {
                driverById(this.id).then(res => {
                    this.data = res.data;
                }).catch(() => {
                });
            },
            device() {
                deviceByDriverId(this.id).then(res => {
                    this.listDeviceData = res.data;
                }).catch(() => {
                    this.listDeviceData = [];
                }).finally(() => {
                    this.deviceLoading = false;
                });

                deviceStatusByDriverId(this.id).then(res => {
                    this.deviceStatusTable = res.data;
                }).catch(() => {
                    this.deviceStatusTable = {};
                });
            },
            drivers() {
                driverDictionary().then(res => {
                    this.driverTable = res.data.reduce((pre, cur) => {
                        pre[cur.value] = cur.label;
                        return pre;
                    }, {});
                }).catch(() => {
                });
            },
            profiles() {
                profileDictionary().then(res => {
                    this.profileTable = res.data.reduce((pre, cur) => {
                        pre[cur.value] = cur.label;
                        return pre;
                    }, {});
                }).catch(() => {
                });
            },
            list() {
                driverList({
                    page: this.page,
                    ...this.query
                }).then(res => {
                    const data = res.data;
                    this.page.total = data.total;
                    data.records.forEach(driver => driver.active = this.id === driver.id);
                    this.listDriverData = data.records;
                }).catch(() => {
                }).finally(() => {
                    this.driverLoading = false;
                });

                driverStatus({
                    page: this.page,
                    ...this.query
                }).then(res => {
                    this.driverStatusTable = res.data;
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
            },
            deviceName() {
                return this.listDeviceData.map(device => device.name).join(", ");
            },
            changeActive(tab) {
                this.active = tab.name;
                let query = this.$route.query;
                this.$router.push({query: {...query, active: tab.name}})
                    .catch(() => {
                    });
            },
            selectChange(data) {
                this.listDriverData.forEach(driver => driver.active = data.id === driver.id);
                this.listDriverData = JSON.parse(JSON.stringify(this.listDriverData));

                this.id = data.id;
                let query = this.$route.query;
                this.$router.push({query: {...query, id: data.id}}).then(() => {
                    this.driver();
                    this.device();
                }).catch(() => {
                });
            },
            copyId(content) {
                setCopyContent(content, true, '驱动ID');
            },
            timestamp(timestamp) {
                return dateFormat(new Date(timestamp));
            }
        }
    }

</script>

<style lang="scss">
</style>
