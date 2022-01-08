<template>
    <div class="things-card">
        <el-card shadow="hover">
            <div class="things-card-content">
                <div class="things-card__header" v-bind:class="{'header-enable':data.interval<200,'header-disable':data.interval>=200}">
                    <div class="things-card-header-icon"><img :src="icon" :alt="data.name"></div>
                    <div class="things-card-header-name nowrap-name" @click="copyId(data.id)">{{point(data.pointId)}}</div>
                    <div title="读写类型" class="things-card-header-status">
                        <el-tag v-if="data.rw===0" type="warning" effect="plain">只读</el-tag>
                        <el-tag v-else-if="data.rw===1" type="info" effect="plain">只写</el-tag>
                        <el-tag v-else-if="data.rw===2" type="success" effect="plain">读写</el-tag>
                    </div>
                </div>
                <div class="things-card__body">
                    <div class="things-card-body-content">
                        <ul class="things-card-body-content-value">
                            <li title="处理值，点击复制" class="nowrap-item value" @click="copyValue(data)">{{data.value}}  {{data.unit}}</li>
                            <li title="计算值" class="nowrap-item">{{data.calculateValue||'-'}}</li>
                            <li class="nowrap-item"><span><i class="el-icon-sunrise"/> 原始值: </span>{{data.rawValue}}</li>
                            <li v-if="!embedded" class="nowrap-item value-point"><span><i class="el-icon-s-finance"/> 所属设备: </span>{{device(data.deviceId)}}</li>
                            <li v-if="!embedded" class="nowrap-item"><span><i class="el-icon-timer"/> 数据延时: </span>{{data.interval}} ms</li>
                            <li class="nowrap-item"><span><i class="el-icon-edit-outline"/> 采集日期: </span>{{timestamp(data.originTime)}}</li>
                            <li class="nowrap-item"><span><i class="el-icon-sunset"/> 保存日期: </span>{{timestamp(data.createTime)}}</li>
                            <li v-if="embedded" title="详细内容" class="value-detail nowrap-item">
                                <i class="el-icon-zoom-in" @click="showDetail(data)"/>
                            </li>
                        </ul>
                    </div>
                    <div v-if="embedded" class="things-card-body-content-time">
                        <sparkline>
                            <sparklineCurve :data="line"
                                            :limit="100"
                                            :refLineStyles="spRefLineStyles"
                                            :styles="spCurveStyles"/>
                        </sparkline>
                    </div>
                </div>
                <div v-if="!embedded" class="things-card__footer">
                    <div class="things-card-footer-operation">
                        <el-popconfirm title="是否确定删除该数据？该数据下的配置将会被全部删除，且该操作不可恢复！" placement="top" icon="el-icon-remove-outline">
                            <el-button class="operation-tooltip" type="text" slot="reference">删除</el-button>
                        </el-popconfirm>
                        <el-button type="text">编辑</el-button>
                        <el-button type="text">详情</el-button>
                    </div>
                </div>
            </div>
        </el-card>
    </div>
</template>

<script>
    import {dateFormat, setCopyContent} from '@/util/util';

    export default {
        name: "point-value-card",
        props: {
            embedded: {
                type: Boolean,
                default: () => {
                    return false;
                }
            },
            deviceTable: {
                type: Object,
                default: () => {
                    return {};
                }
            },
            pointTable: {
                type: Object,
                default: () => {
                    return {};
                }
            },
            historyData: {
                type: Object,
                default: () => {
                    return {};
                }
            },
            data: {
                type: Object,
                default: () => {
                    return {
                        deviceId: '',
                        pointId: '',
                        value: '',
                        rawValue: '',
                        calculateValue: '',
                        interval: '',
                        createTime: '',
                        originTime: ''
                    };
                }
            },
            icon: {
                type: String,
                default: 'images/common/point.png'
            }
        },
        data() {
            return {
                spRefLineStyles: {
                    stroke: '#54a5ff',
                    strokeOpacity: 0.5,
                    strokeDasharray: '2, 2'
                },
                spCurveStyles: {
                    stroke: '#54a5ff'
                }
            }
        },
        mounted() {
        },
        computed: {
            line() {
                return this.embedded ? this.historyData[this.data.pointId] : [];
            }
        },
        methods: {
            device(id) {
                return this.deviceTable[id] || 'NaN';
            },
            point(id) {
                return this.pointTable[id] || 'NaN';
            },
            showEdit(pointValue) {
                this.$emit('showEdit', pointValue);
            },
            showDetail(data) {
                let dataValue = data.value;
                try {
                    dataValue = JSON.parse(data.value)
                } catch (ignore) {
                }

                let content = {
                    deviceId: data.deviceId,
                    pointId: data.pointId,
                    type: data.type,
                    value: dataValue,
                    calculateValue: data.calculateValue,
                    rawValue: data.rawValue
                };

                this.$emit('showDetail', content);
            },
            copyId(content) {
                setCopyContent(content, true, '位号值ID');
            },
            copyValue(data) {
                let content = {
                    deviceId: data.deviceId,
                    pointId: data.pointId,
                    value: data.value
                };
                setCopyContent(JSON.stringify(content, null, 2), true, '位号值');
            },
            timestamp(timestamp) {
                return dateFormat(new Date(timestamp));
            }
        }
    };
</script>

<style lang="scss">
    @import "~@/components/card/styles/things-card.scss";

    .things-card-body-content-time {
        #value-line {
            width: 100%;
            height: 50px;
        }

        .sparkline-wrap {
            width: 100%;

            svg {
                width: 100% !important;
            }
        }
    }
</style>

