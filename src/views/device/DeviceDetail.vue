<template>
    <div>
        <base-card>
            <el-tabs v-model="active" @tab-click="changeActive">
                <el-tab-pane label="设备详情" name="detail">
                    <detail-card>
                        <ul>
                            <li><i class="el-icon-data-line"></i> 设备名称: {{data.name}}</li>
                            <li><i class="el-icon-tickets"></i> 包含模板 [{{data.profileIds.length||0}} 个]: {{profileName(data.profileIds)}}</li>
                            <li><i class="el-icon-collection-tag"></i> 包含位号 [{{listPointData.length||0}} 个]: {{pointName()}}</li>
                            <li><i class="el-icon-s-promotion"></i> 所属驱动: {{driverName(data.driverId)}}</li>
                            <li><i class="el-icon-edit-outline"></i> 修改日期: {{timestamp(data.createTime)}}</li>
                            <li><i class="el-icon-sunset"></i> 创建日期: {{timestamp(data.updateTime)}}</li>
                        </ul>
                    </detail-card>
                </el-tab-pane>
                <el-tab-pane label="包含模板" name="profile">
                    <el-row>
                        <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4" :key="data.id" v-for="data in 12">
                            <skeleton-card :loading="profileLoading" :footer="true"></skeleton-card>
                        </el-col>
                        <el-col>
                            <el-empty v-if="listProfileData.length<1" description="暂无包含的模板数据！"></el-empty>
                        </el-col>
                        <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4" :key="data.id" v-for="data in listProfileData">
                            <profile-card
                                    :data="data"
                                    :pointTable="pointTable"
                                    :embedded="true"
                            ></profile-card>
                        </el-col>
                    </el-row>
                </el-tab-pane>
                <el-tab-pane label="包含位号" name="point">
                    <el-row>
                        <el-col :xs="24" :sm="12" :md="12" :lg="8" :xl="6" :key="data.id" v-for="data in 12">
                            <skeleton-card :loading="pointLoading" :footer="true"></skeleton-card>
                        </el-col>
                        <el-col>
                            <el-empty v-if="listPointData.length<1" description="暂无包含的位号数据！"></el-empty>
                        </el-col>
                        <el-col :xs="24" :sm="12" :md="12" :lg="8" :xl="6" :key="data.id" v-for="data in listPointData">
                            <point-card
                                    :data="data"
                                    :profileTable="profileTable"
                                    :embedded="true"
                            ></point-card>
                        </el-col>
                    </el-row>
                </el-tab-pane>
                <el-tab-pane label="设备模型" name="deviceCModel">
                    <el-empty description="暂无设备模型数据！"></el-empty>
                </el-tab-pane>
                <el-tab-pane label="设备数据" name="pointValue">
                    <el-row>
                        <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4" :key="data.id" v-for="data in 12">
                            <skeleton-card :loading="pointValueLoading" :footer="true"></skeleton-card>
                        </el-col>
                        <el-col>
                            <el-empty v-if="listPointValueData.length<1" description="暂无设备位号数据！"></el-empty>
                        </el-col>
                        <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4" :key="data.id" v-if="data" v-for="data in listPointValueData">
                            <point-value-card
                                    v-if="data"
                                    :data="data"
                                    :deviceTable="deviceTable"
                                    :pointTable="pointTable"
                                    :historyData="listPointValueHistoryData"
                                    @showEdit="showPointValueEdit"
                                    @showDetail="showPointValueDetail"
                                    :embedded="true"
                            ></point-value-card>
                        </el-col>
                    </el-row>
                </el-tab-pane>
                <el-tab-pane label="设备指令" name="deviceCommand">
                    <el-empty description="暂无设备指令数据！"></el-empty>
                </el-tab-pane>
                <el-tab-pane label="设备事件" name="deviceEvent">
                    <el-timeline>
                        <el-timeline-item timestamp="2021/7/30" placement="top">
                            <el-card>
                                <h4>设备数据上报</h4>
                                <p>该设备于 2021/7/30 20:46 开始上报数据</p>
                            </el-card>
                        </el-timeline-item>
                        <el-timeline-item timestamp="2021/7/30" placement="top">
                            <el-card>
                                <h4>设备上线</h4>
                                <p>该设备于 2021/7/30 20:46 上线</p>
                            </el-card>
                        </el-timeline-item>
                        <el-timeline-item timestamp="2021/7/30" placement="top">
                            <el-card>
                                <h4>设备注册</h4>
                                <p>该设备于 2021/7/30 20:46 注册成功</p>
                            </el-card>
                        </el-timeline-item>
                    </el-timeline>
                </el-tab-pane>
            </el-tabs>
        </base-card>

        <point-value-edit-form
                ref="point-value-edit-form"
                :formData="pointValueFormData"
                @update-thing="updateThing">
        </point-value-edit-form>

        <point-value-detail
                ref="point-value-detail"
                :detailData="pointValueDetailData">
        </point-value-detail>

    </div>
</template>
<script>
    import baseCard from '@/components/card/base-card';
    import detailCard from '@/components/card/detail-card';
    import skeletonCard from '@/components/card/skeleton-card';
    import deviceCard from './DeviceCard';
    import profileCard from '../profile/ProfileCard';
    import pointCard from '../point/PointCard';
    import pointValueCard from '../point/value/PointValueCard';
    import pointValueEditForm from '../point/value/PointValueEditForm';
    import pointValueDetail from '../point/value/PointValueDetail';
    import {deviceById, deviceStatus} from "@/api/device";
    import {driverDictionary, pointDictionary, profileDictionary} from "@/api/dictionary";
    import {dateFormat, setCopyContent} from "@/util/util";
    import {profileByDeviceId} from "@/api/profile";
    import {pointByDeviceId, pointValueByDeviceId} from "@/api/point";

    export default {
        components: {baseCard, detailCard, skeletonCard, deviceCard, profileCard, pointCard, pointValueCard, pointValueEditForm, pointValueDetail},
        props: {
            name: String
        },
        data() {
            return {
                id: this.$route.query.id,
                active: this.$route.query.active,
                profileLoading: true,
                pointLoading: true,
                pointValueLoading: true,
                data: {
                    profileIds: []
                },
                driverDictionary: [],
                profileDictionary: [],
                pointDictionary: [],
                driverTable: {},
                profileTable: {},
                pointTable: {},
                deviceTable: {},
                statusTable: {},
                listProfileData: [],
                listPointData: [],
                listPointValueData: [],
                listPointValueHistoryData: {},
                pointValueFormData: {},
                pointValueDetailData: {}
            }
        },
        created() {
            this.pointValues();
            this.driver();
            this.profile();
            this.point();
            this.device();
            this.status();
            this.profiles();
            this.points();
        },
        methods: {
            device() {
                deviceById(this.id).then(res => {
                    this.data = res.data;
                    this.deviceTable[this.data.id] = this.data.name;
                }).catch(() => {
                });
            },
            profiles() {
                profileByDeviceId(this.id).then(res => {
                    this.listProfileData = res.data;
                }).catch(() => {
                }).finally(() => {
                    this.profileLoading = false;
                });
            },
            points() {
                pointByDeviceId(this.id).then(res => {
                    this.listPointData = res.data;
                }).catch(() => {
                }).finally(() => {
                    this.pointLoading = false;
                });
            },
            pointValues() {
                pointValueByDeviceId(this.id, true).then(res => {
                    this.listPointValueData = res.data;

                    res.data.forEach(pointValue => {
                        if (pointValue.type === 'string') {
                            this.listPointValueHistoryData[pointValue.pointId] = [];
                        } else if (pointValue.type === 'boolean') {
                            this.listPointValueHistoryData[pointValue.pointId] = pointValue.children.reverse().map(pointValue => pointValue.value === 'true' ? 1 : 0);
                        } else {
                            this.listPointValueHistoryData[pointValue.pointId] = pointValue.children.reverse().map(pointValue => pointValue.value);
                        }
                    });
                }).catch(() => {
                }).finally(() => {
                    this.pointValueLoading = false;
                });
            },
            status() {
                deviceStatus({
                    id: this.id
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
            point() {
                pointDictionary('point').then(res => {
                    this.pointDictionary = res.data;
                    this.pointTable = this.pointDictionary.reduce((pre, cur) => {
                        pre[cur.value] = cur.label;
                        return pre;
                    }, {});
                }).catch(() => {
                });
            },
            showPointValueEdit(pointValue) {
                this.pointValueFormData = {
                    deviceId: pointValue.deviceId,
                    pointId: pointValue.pointId,
                    value: pointValue.value,
                    type: pointValue.type
                };
                this.$refs['point-value-edit-form'].show();
            },
            showPointValueDetail(pointValue) {
                this.pointValueDetailData = pointValue;
                this.$refs['point-value-detail'].show();
            },
            updateThing(pointValue) {
                console.log('update things', pointValue);
            },
            driverName(driverId) {
                return this.driverTable[driverId];
            },
            profileName(ids) {
                if (ids) {
                    return ids.map(id => this.profileTable[id]).join(", ");
                }
            },
            pointName() {
                return this.listPointData.map(point => this.pointTable[point.id]).join(", ");
            },
            changeActive(tab) {
                let query = this.$route.query;
                this.$router.push({query: {...query, active: tab.name}})
                    .catch(() => {
                    });
            },
            copyId(content) {
                setCopyContent(content, true, '设备ID');
            },
            timestamp(timestamp) {
                return dateFormat(new Date(timestamp));
            }
        }
    }

</script>

<style lang="scss">
    @import "~@/components/card/styles/things-card.scss";
</style>
