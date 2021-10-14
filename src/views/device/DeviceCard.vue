<template>
    <div class="thing-card">
        <el-card shadow="hover">
            <div class="thing-content">
                <div class="thing-header" v-bind:class="{'header-enable':data.enable,'header-disable':!data.enable}">
                    <div class="thing-icon">
                        <img :src="icon" :alt="data.name">
                    </div>
                    <div class="thing-name thing-content-nowrap-title" @click="copyId(data.id)">{{data.name}}</div>
                    <div title="状态" class="thing-status">
                        <el-tag v-if="status(data.id)==='ONLINE'" type="success" effect="plain">在线</el-tag>
                        <el-tag v-else-if="status(data.id)==='MAINTAIN'" type="warning" effect="plain">维护</el-tag>
                        <el-tag v-else-if="status(data.id)==='FAULT'" type="danger" effect="plain">故障</el-tag>
                        <el-tag v-else-if="status(data.id)==='DISABLE'" type="info" effect="plain">停用</el-tag>
                        <el-tag v-else type="info" effect="plain">离线</el-tag>
                    </div>
                </div>
                <div class="thing-body">
                    <div class="thing-body-content">
                        <ul class="thing-body-content-item">
                            <li class="thing-content-nowrap-item"><span><i class="el-icon-s-promotion"/> 所属驱动: </span>{{driver(data.driverId)}}</li>
                            <li class="thing-content-nowrap-item"><span><i class="el-icon-tickets"/> 包含模板: </span>{{data.profileIds?profile(data.profileIds):''}}</li>
                            <li class="thing-content-nowrap-item"><span><i class="el-icon-coin"/> 存储类型: </span>{{data.multi?'结构数据':'单点数据'}}</li>
                        </ul>
                    </div>
                    <div title="设备描述信息" class="thing-body-content">
                        <p class="thing-content-nowrap-description">{{data.description?data.description:'无描述信息'}}</p>
                    </div>
                    <div class="thing-body-content-time">
                        <ul class="thing-body-content-item">
                            <li><span><i class="el-icon-edit-outline"/> 修改日期: </span>{{timestamp(data.createTime)}}</li>
                        </ul>
                        <ul class="thing-body-content-item">
                            <li><span><i class="el-icon-sunset"/> 创建日期: </span>{{timestamp(data.updateTime)}}</li>
                        </ul>
                    </div>
                </div>
                <div v-if="!embedded" class="thing-footer">
                    <div class="thing-copy-id">
                        <el-tooltip class="item" effect="dark" placement="top" v-if="data.id" content="点击复制ID">
                            <el-button class="button" type="text" icon="el-icon-document-copy" v-on:click="copyId(data.id)">{{data.id}}</el-button>
                        </el-tooltip>
                    </div>
                    <div class="thing-operation">
                        <el-popconfirm title="是否确定停用该设备？" placement="top" icon="el-icon-remove-outline">
                            <el-button class="thing-tooltip" type="text" slot="reference" :disabled="!data.enable">停用</el-button>
                        </el-popconfirm>
                        <el-popconfirm title="是否确定启用该设备？" placement="top" icon="el-icon-remove-outline">
                            <el-button class="thing-tooltip" type="text" slot="reference" :disabled="data.enable">启用</el-button>
                        </el-popconfirm>
                        <el-popconfirm title="是否确定删除该设备？" placement="top" icon="el-icon-circle-close" icon-color="red" @confirm="deleteThing(data.id)">
                            <el-button class="thing-tooltip" type="text" slot="reference">删除</el-button>
                        </el-popconfirm>
                        <el-button type="text" @click="edit(data.id, data.driverId)">编辑</el-button>
                        <el-button type="text" @click="detail(data.id, data.driverId)">详情</el-button>
                    </div>
                </div>
            </div>
        </el-card>
    </div>
</template>

<script>
    import {dateFormat, setCopyContent, successMessage} from '@/util/util'

    export default {
        name: "device-card",
        props: {
            embedded: {
                type: Boolean,
                default: () => {
                    return false;
                }
            },
            driverTable: {
                type: Object,
                default: () => {
                    return {};
                }
            },
            profileTable: {
                type: Object,
                default: () => {
                    return {};
                }
            },
            statusTable: {
                type: Object,
                default: () => {
                    return {};
                }
            },
            data: {
                type: Object,
                default: () => {
                    return {
                        name: '',
                        driverId: '',
                        profileIds: [],
                        multi: false,
                        enable: true,
                        description: '',
                        createTime: '',
                        updateTime: ''
                    };
                }
            },
            icon: {
                type: String,
                default: 'images/common/device.png'
            }
        },
        methods: {
            driver(id) {
                return this.driverTable[id];
            },
            profile(ids) {
                if (ids) {
                    if (ids.length > 10) {
                        ids = ids.slice(0, 10);
                    }
                    return ids.map(id => this.profileTable[id]).join(", ");
                }
            },
            status(id) {
                return this.statusTable[id];
            },
            deleteThing(id) {
                this.$emit('delete-thing', id, () => {
                    successMessage();
                });
            },
            edit(id, driverId) {
                this.$router.push({name: 'deviceEdit', query: {id, driverId, active: '0'}});
            },
            detail(id, driverId) {
                this.$router.push({name: 'deviceDetail', query: {id, driverId, active: 'detail'}});
            },
            copyId(content) {
                setCopyContent(content, true, '设备ID');
            },
            timestamp(timestamp) {
                return dateFormat(new Date(timestamp));
            }
        }
    };
</script>

<style lang="scss">
    @import "~@/components/card/styles/thing-card.scss";
</style>

