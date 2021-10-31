<template>
    <div class="thing-card">
        <el-card shadow="hover">
            <div class="thing-card-content">
                <div class="thing-card__header" v-bind:class="{'header-enable':data.enable,'header-disable':!data.enable}">
                    <div class="thing-card-header-icon"><img :src="icon" :alt="data.name"></div>
                    <div class="thing-card-header-name nowrap-name" @click="copyId(data.id)">{{data.name}}</div>
                    <div title="状态" class="thing-card-header-status">
                        <el-tag v-if="status(data.id)==='ONLINE'" type="success" effect="plain">在线</el-tag>
                        <el-tag v-else-if="status(data.id)==='MAINTAIN'" type="warning" effect="plain">维护</el-tag>
                        <el-tag v-else-if="status(data.id)==='FAULT'" type="danger" effect="plain">故障</el-tag>
                        <el-tag v-else-if="status(data.id)==='DISABLE'" type="info" effect="plain">停用</el-tag>
                        <el-tag v-else-if="status(data.id)==='UNREGISTERED'" type="info" effect="plain">未注册</el-tag>
                        <el-tag v-else type="info" effect="plain">离线</el-tag>
                    </div>
                </div>
                <div class="thing-card__body">
                    <div class="thing-card-body__content">
                        <ul class="thing-card-body-content-item">
                            <li class="nowrap-item"><span><i class="el-icon-connection"/> 端口: </span>{{data.port}}</li>
                            <li class="nowrap-item"><span><i class="el-icon-monitor"/> 主机: </span>{{data.host}}</li>
                            <li class="nowrap-item"><span><i class="el-icon-s-promotion"/> 网关服务: </span>{{data.serviceName}}</li>
                        </ul>
                    </div>
                    <div title="网关描述信息" class="thing-card-body__content">
                        <p class="nowrap-description">{{data.description?data.description:'无描述信息'}}</p>
                    </div>
                    <div class="thing-card-body-content-time">
                        <ul class="thing-card-body-content-item">
                            <li><span><i class="el-icon-edit-outline"/> 修改日期: </span>{{timestamp(data.createTime)}}</li>
                        </ul>
                        <ul class="thing-card-body-content-item">
                            <li><span><i class="el-icon-sunset"/> 创建日期: </span>{{timestamp(data.updateTime)}}</li>
                        </ul>
                    </div>
                </div>
                <div class="thing-card__footer">
                    <div class="thing-card-footer-copy-id">
                        <el-tooltip class="item" effect="dark" placement="top" v-if="data.id" content="点击复制ID">
                            <el-button class="button" type="text" icon="el-icon-document-copy" v-on:click="copyId(data.id)">{{data.id}}</el-button>
                        </el-tooltip>
                    </div>
                    <div class="thing-card-footer-operation">
                        <el-popconfirm title="是否确定停用该网关？" placement="top" icon="el-icon-remove-outline">
                            <el-button class="operation-tooltip" type="text" slot="reference" disabled>停用</el-button>
                        </el-popconfirm>
                        <el-popconfirm title="是否确定启用该网关？" placement="top" icon="el-icon-remove-outline">
                            <el-button class="operation-tooltip" type="text" slot="reference" disabled>启用</el-button>
                        </el-popconfirm>
                        <el-popconfirm title="是否确定删除该网关？" placement="top" icon="el-icon-remove-outline">
                            <el-button class="operation-tooltip" type="text" slot="reference" disabled>删除</el-button>
                        </el-popconfirm>
                        <el-button type="text" @click="detail(data.id)">详情</el-button>
                    </div>
                </div>
            </div>
        </el-card>
    </div>
</template>

<script>
    import {dateFormat, setCopyContent} from '@/util/util'

    export default {
        name: "gateway-card",
        props: {
            data: {
                type: Object,
                default: () => {
                    return {
                        name: '',
                        serviceName: '',
                        host: '',
                        port: '',
                        enable: '',
                        description: '',
                        createTime: '',
                        updateTime: ''
                    };
                }
            },
            statusTable: {
                type: Object,
                default: () => {
                    return {};
                }
            },
            icon: {
                type: String,
                default: 'images/common/gateway.png'
            }
        },
        methods: {
            status(id) {
                return this.statusTable[id];
            },
            detail(id) {
                this.$router.push({name: 'gatewayDetail', query: {id, active: 'detail'}});
            },
            copyId(content) {
                setCopyContent(content, true, '网关ID');
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
