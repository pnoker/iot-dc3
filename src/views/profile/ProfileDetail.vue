<template>
    <div>
        <base-card>
            <el-tabs v-model="active" @tab-click="changeActive">
                <el-tab-pane label="模板详情" name="detail">
                    <el-card shadow="hover">
                        <ul class="detail-body">
                            <li><i class="el-icon-data-line"></i> 模板名称: {{data.name}}</li>
                            <li><i class="el-icon-collection-tag"></i> 包含位号 [{{listPointData.length||0}} 个]: {{pointName()}}</li>
                            <li><i class="el-icon-collection-tag"></i> 关联设备 [{{listDeviceData.length||0}} 个]: {{deviceName()}}</li>
                            <li><i class="el-icon-edit-outline"></i> 修改日期: {{timestamp(data.createTime)}}</li>
                            <li><i class="el-icon-sunset"></i> 创建日期: {{timestamp(data.updateTime)}}</li>
                        </ul>
                    </el-card>
                </el-tab-pane>
                <el-tab-pane label="包含位号" name="point">
                    <el-row>
                        <el-col :xs="24" :sm="12" :md="12" :lg="8" :xl="6" :key="data.id" v-for="data in listPointData">
                            <point-card
                                    :data="data"
                                    :profileTable="profileTable"
                                    :embedded="true"
                            ></point-card>
                        </el-col>
                    </el-row>
                </el-tab-pane>
                <el-tab-pane label="关联设备" name="device">
                    <el-row>
                        <el-col :xs="24" :sm="12" :md="12" :lg="8" :xl="6" :key="data.id" v-for="data in listDeviceData">
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
            </el-tabs>
        </base-card>
    </div>
</template>
<script>
    import baseCard from '@/components/card/base-card'
    import deviceCard from '../device/DeviceCard'
    import pointCard from '../point/PointCard'
    import {dateFormat, setCopyContent} from "@/util/util";
    import {profileById} from "@/api/profile";
    import {pointByProfileId} from "@/api/point";
    import {deviceByProfileId, deviceStatusByProfileId} from "@/api/device";
    import {driverDictionary, profileDictionary} from "@/api/dictionary";

    export default {
        components: {baseCard, deviceCard, pointCard},
        data() {
            return {
                id: this.$route.query.id,
                active: this.$route.query.active,
                driverTable: {},
                profileTable: {},
                statusTable: {},
                data: {},
                listDeviceData: [],
                listPointData: []
            }
        },
        created() {
            this.profile();
            this.device();
            this.point();
            this.drivers();
            this.profiles();
        },
        beforeRouteLeave(to, from, next) {
            if (this.interval) {
                clearInterval(this.interval);
            }
            next();
        },
        methods: {
            profile() {
                profileById(this.id).then(res => {
                    this.data = res.data;
                });
            },
            device() {
                deviceByProfileId(this.id).then(res => {
                    this.listDeviceData = res.data;
                });

                deviceStatusByProfileId(this.id).then(res => {
                    this.statusTable = res.data;
                }).catch(() => {
                });
            },
            point() {
                pointByProfileId(this.id).then(res => {
                    this.listPointData = res.data;
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
            pointName() {
                return this.listPointData.map(point => point.name).join(", ");
            },
            changeActive(tab) {
                let query = this.$route.query;
                this.$router.push({query: {...query, active: tab.name}});
            },
            copyId(content) {
                setCopyContent(content, true, '模板ID');
            },
            timestamp(timestamp) {
                return dateFormat(new Date(timestamp));
            }
        }
    }

</script>

<style lang="scss">
    @import "~@/components/card/styles/thing-card.scss";
</style>
