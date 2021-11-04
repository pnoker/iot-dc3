<template>
    <div class="things-card">
        <el-card shadow="hover">
            <div class="things-card-content">
                <div class="things-card__header" v-bind:class="{'header-enable':data.enable,'header-disable':!data.enable}">
                    <div class="things-card-header-icon"><img :src="icon" :alt="data.name"></div>
                    <div class="things-card-header-name nowrap-name" @click="copyId(data.id)">{{data.name}}</div>
                    <div title="状态" class="things-card-header-status"></div>
                </div>
                <div class="things-card__body">
                    <div class="things-card-body-content">
                        <ul>
                            <li class="nowrap-item"><span><i class="el-icon-ice-cream-square"/> 位号个数: </span>{{data.pointIds.length||0}}</li>
                            <li class="nowrap-item"><span><i class="el-icon-collection-tag"/> 位号: </span>{{point(data.pointIds)}}</li>
                            <li class="nowrap-item"><span><i class="el-icon-edit-outline"/> 修改日期: </span>{{timestamp(data.updateTime)}}</li>
                            <li class="nowrap-item"><span><i class="el-icon-sunset"/> 创建日期: </span>{{timestamp(data.createTime)}}</li>
                        </ul>
                    </div>
                    <div title="模板描述信息" class="things-card-body-content">
                        <p class="nowrap-description">{{data.description?data.description:'无描述信息'}}</p>
                    </div>
                </div>
                <div v-if="!embedded" class="things-card__footer">
                    <div class="things-card-footer-operation">
                        <el-popconfirm title="是否确定停用该模板？" placement="top" icon="el-icon-remove-outline">
                            <el-button class="operation-tooltip" type="text" slot="reference" :disabled="!data.enable">停用</el-button>
                        </el-popconfirm>
                        <el-popconfirm title="是否确定启用该模板？" placement="top" icon="el-icon-remove-outline">
                            <el-button class="operation-tooltip" type="text" slot="reference" :disabled="data.enable">启用</el-button>
                        </el-popconfirm>
                        <el-popconfirm title="是否确定删除该模板？" placement="top" icon="el-icon-circle-close" icon-color="red" @confirm="deleteThing(data.id)">
                            <el-button class="operation-tooltip" type="text" slot="reference">删除</el-button>
                        </el-popconfirm>
                        <el-button type="text" @click="edit(data.id)">编辑</el-button>
                        <el-button type="text" @click="detail(data.id)">详情</el-button>
                    </div>
                </div>
            </div>
        </el-card>
    </div>
</template>

<script>
    import {dateFormat, setCopyContent, successMessage} from '@/util/util'

    export default {
        name: "profile-card",
        props: {
            embedded: {
                type: Boolean,
                default: () => {
                    return false;
                }
            },
            pointTable: {
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
                default: 'images/common/profile.png'
            }
        },
        methods: {
            point(ids) {
                if (ids) {
                    if (ids.length > 10) {
                        ids = ids.slice(0, 10);
                    }
                    return ids.map(id => this.pointTable[id]).join(", ");
                }
            },
            deleteThing(id) {
                this.$emit('delete-thing', id, () => {
                    successMessage();
                });
            },
            edit(id) {
                this.$router.push({name: 'profileEdit', query: {id, active: '0'}});
            },
            detail(id) {
                this.$router.push({name: 'profileDetail', query: {id, active: 'detail'}});
            },
            copyId(content) {
                setCopyContent(content, true, '模板ID');
            },
            timestamp(timestamp) {
                return dateFormat(new Date(timestamp));
            }
        }
    };
</script>

<style lang="scss">
    @import "~@/components/card/styles/things-card.scss";
</style>
