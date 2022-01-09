<template>
    <div>
        <base-card>
            <el-tabs v-model="active" @tab-click="changeActive">
                <el-tab-pane label="驱动详情" name="detail">
                    <detail-card>
                        <ul>
                            <li><i class="el-icon-data-line"></i> 驱动名称: {{data.name}}</li>
                            <li><i class="el-icon-collection-tag"></i> 关联设备 [{{listDeviceData.length||0}} 个]: {{deviceName()}}</li>
                            <li><i class="el-icon-edit-outline"></i> 修改日期: {{timestamp(data.createTime)}}</li>
                            <li><i class="el-icon-sunset"></i> 创建日期: {{timestamp(data.updateTime)}}</li>
                        </ul>
                    </detail-card>
                </el-tab-pane>
                <el-tab-pane label="关联设备" name="device">
                    <el-row>
                        <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4" :key="data.id" v-for="data in 12">
                            <skeleton-card :loading="deviceLoading" :footer="true"></skeleton-card>
                        </el-col>
                        <el-empty v-if="listDeviceData.length<1" description="暂无关联的设备数据！"></el-empty>
                        <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4" :key="data.id" v-for="data in listDeviceData">
                            <device-card
                                    :data="data"
                                    :driverTable="driverTable"
                                    :profileTable="profileTable"
                                    :statusTable="statusTable"
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
    </div>
</template>
<script>
    import baseCard from '@/components/card/base-card';
    import detailCard from '@/components/card/detail-card';
    import skeletonCard from '@/components/card/skeleton-card';
    import deviceList from '../device/DeviceList';
    import deviceCard from '../device/DeviceCard';
    import pointCard from '../point/PointCard';
    import {dateFormat, setCopyContent} from "@/util/util";
    import {deviceByDriverId, deviceStatusByDriverId} from "@/api/device";
    import {driverDictionary, profileDictionary} from "@/api/dictionary";
    import {driverById} from "@/api/driver";
    import DriverList from "@/views/driver/DriverList";

    export default {
        components: {DriverList, baseCard, detailCard, skeletonCard, deviceList, deviceCard, pointCard},
        data() {
            return {
                id: this.$route.query.id,
                active: this.$route.query.active,
                deviceLoading: true,
                driverTable: {},
                profileTable: {},
                statusTable: {},
                data: {},
                listDeviceData: []
            }
        },
        created() {
            this.driver();
            this.device();
            this.drivers();
            this.profiles();
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
                }).finally(() => {
                    this.deviceLoading = false;
                });

                deviceStatusByDriverId(this.id).then(res => {
                    this.statusTable = res.data;
                }).catch(() => {
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
            deviceName() {
                return this.listDeviceData.map(device => device.name).join(", ");
            },
            changeActive(tab) {
                let query = this.$route.query;
                this.$router.push({query: {...query, active: tab.name}});
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
