<template>
    <div class="thing-card">
        <el-card shadow="hover">
            <div class="thing-content">
                <div class="thing-header" v-bind:class="{'header-enable':data.enable,'header-disable':!data.enable}">
                    <div class="thing-icon"><img :src="icon" :alt="data.name"></div>
                    <div class="thing-name thing-content-nowrap-title" @click="copyId(data.id)">{{data.name}}</div>
                    <div title="状态" class="thing-status"></div>
                </div>
                <div class="thing-body">
                    <div class="thing-body-content">
                        <ul class="thing-body-content-item-point thing-body-content-item">
                            <li class="thing-content-nowrap-item"><span><i class="el-icon-location-outline"/> 所属模板: </span>{{profile(data.profileId)}}</li>
                            <li class="thing-content-nowrap-item"><span><i class="el-icon-location-outline"/> 累计标识: </span>{{data.accrue?'累计':'不累计'}}</li>
                            <li class="thing-content-nowrap-item"><span><i class="el-icon-location-outline"/> 基值: </span>{{data.base}}</li>
                            <li class="thing-content-nowrap-item"><span><i class="el-icon-location-outline"/> 格式: </span>{{data.format}}</li>
                            <li class="thing-content-nowrap-item"><span><i class="el-icon-location-outline"/> 最小值: </span>{{data.minimum}}</li>
                        </ul>
                        <ul class="thing-body-content-item-point thing-body-content-item">
                            <li class="thing-content-nowrap-item"><span><i class="el-icon-location-outline"/> 数据类型: </span>{{type(data.type)}}</li>
                            <li class="thing-content-nowrap-item"><span><i class="el-icon-location-outline"/> 读写类型: </span>{{rw(data.rw)}}</li>
                            <li class="thing-content-nowrap-item"><span><i class="el-icon-location-outline"/> 倍数: </span>{{data.multiple}}</li>
                            <li class="thing-content-nowrap-item"><span><i class="el-icon-location-outline"/> 单位: </span>{{data.unit}}</li>
                            <li class="thing-content-nowrap-item"><span><i class="el-icon-location-outline"/> 最大值: </span>{{data.maximum}}</li>
                        </ul>
                    </div>
                    <div title="位号描述信息" class="thing-body-content">
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
                        <el-popconfirm title="是否确定停用该位号？" placement="top" icon="el-icon-remove-outline">
                            <el-button class="thing-tooltip" type="text" slot="reference" :disabled="!data.enable">停用</el-button>
                        </el-popconfirm>
                        <el-popconfirm title="是否确定启用该位号？" placement="top" icon="el-icon-remove-outline">
                            <el-button class="thing-tooltip" type="text" slot="reference" :disabled="data.enable">启用</el-button>
                        </el-popconfirm>
                        <el-popconfirm title="是否确定删除该位号？" placement="top" icon="el-icon-circle-close" icon-color="red" @confirm="deleteThing(data.id)">
                            <el-button class="thing-tooltip" type="text" slot="reference">删除</el-button>
                        </el-popconfirm>
                        <el-button type="text" @click="edit(data.id)">编辑</el-button>
                        <el-button type="text" @click="detail(data.id)" disabled>详情</el-button>
                    </div>
                </div>
            </div>
        </el-card>
    </div>
</template>

<script>
    import {dateFormat, setCopyContent, successMessage} from '@/util/util'

    export default {
        name: "point-card",
        props: {
            embedded: {
                type: Boolean,
                default: () => {
                    return false;
                }
            },
            profileTable: {
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
                        enable: '',
                        description: '',
                        createTime: '',
                        updateTime: ''
                    };
                }
            },
            icon: {
                type: String,
                default: 'images/common/point.png'
            }
        },
        methods: {
            profile(id) {
                return this.profileTable[id];
            },
            type(type) {
                if (type === 'string') {
                    return '字符串';
                } else if (type === 'int') {
                    return '整数';
                } else if (type === 'double') {
                    return '双精度浮点数';
                } else if (type === 'float') {
                    return '浮点数';
                } else if (type === 'long') {
                    return '长整型';
                } else if (type === 'boolean') {
                    return '布尔量';
                } else if (type === 'short') {
                    return '短整型';
                } else if (type === 'byte') {
                    return '字节';
                }
            },
            rw(rw) {
                if (rw === 0) {
                    return '只读';
                } else if (rw === 1) {
                    return '只写';
                } else if (rw === 2) {
                    return '读写';
                }
            },
            deleteThing(id) {
                this.$emit('delete-thing', id, () => {
                    successMessage();
                });
            },
            edit(id) {
                this.$router.push({name: 'pointEdit', query: {id, profileId: this.data.profileId, active: '0'}});
            },
            detail(id) {
                this.$router.push({name: 'pointDetail', query: {id, active: 'detail'}});
            },
            copyId(content) {
                setCopyContent(content, true, '位号ID');
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
