<!--
  - Copyright (c) 2022. Pnoker. All Rights Reserved.
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -     http://www.apache.org/licenses/LICENSE-2.0
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->

<template>
    <div class="things-card">
        <el-card shadow="hover">
            <div class="things-card-content">
                <div class="things-card__header" v-bind:class="{'header-enable':data.enable,'header-disable':!data.enable}">
                    <div class="things-card-header-icon">
                        <img :src="icon" :alt="data.name">
                    </div>
                    <div class="things-card-header-name nowrap-name" @click="copyId(data.id)">{{ data.name }}</div>
                    <div title="状态" class="things-card-header-status">
                        <el-tag v-if="status(data.id)==='ONLINE'" type="success" effect="plain">在线</el-tag>
                        <el-tag v-else-if="status(data.id)==='MAINTAIN'" type="warning" effect="plain">维护</el-tag>
                        <el-tag v-else-if="status(data.id)==='FAULT'" type="danger" effect="plain">故障</el-tag>
                        <el-tag v-else-if="status(data.id)==='DISABLE'" type="info" effect="plain">停用</el-tag>
                        <el-tag v-else type="info" effect="plain">离线</el-tag>
                    </div>
                </div>
                <div class="things-card__body">
                    <div class="things-card-body-content">
                        <ul>
                            <li class="nowrap-item"><span><i class="el-icon-s-promotion"/> 所属驱动: </span>{{ driver(data.driverId) }}</li>
                            <li class="nowrap-item"><span><i class="el-icon-tickets"/> 包含模板: </span>{{ data.profileIds ? profile(data.profileIds) : "" }}</li>
                            <li class="nowrap-item"><span><i class="el-icon-coin"/> 存储类型: </span>{{ data.multi ? "结构数据" : "单点数据" }}</li>
                            <li class="nowrap-item"><span><i class="el-icon-edit-outline"/> 修改日期: </span>{{ timestamp(data.updateTime) }}</li>
                            <li class="nowrap-item"><span><i class="el-icon-sunset"/> 创建日期: </span>{{ timestamp(data.createTime) }}</li>
                        </ul>
                    </div>
                    <div title="设备描述信息" class="things-card-body-content">
                        <p class="nowrap-description">{{ data.description ? data.description : "无描述信息" }}</p>
                    </div>
                </div>
                <div v-if="!embedded" class="things-card__footer">
                    <div class="things-card-footer-operation">
                        <el-popconfirm title="是否确定停用该设备？" placement="top" icon="el-icon-remove-outline">
                            <el-button class="operation-tooltip" type="text" slot="reference" :disabled="!data.enable">停用</el-button>
                        </el-popconfirm>
                        <el-popconfirm title="是否确定启用该设备？" placement="top" icon="el-icon-remove-outline">
                            <el-button class="operation-tooltip" type="text" slot="reference" :disabled="data.enable">启用</el-button>
                        </el-popconfirm>
                        <el-popconfirm title="是否确定删除该设备？该设备下的配置将会被全部删除，且该操作不可恢复！" placement="top" icon="el-icon-circle-close" icon-color="red" @confirm="deleteThing(data.id)">
                            <el-button class="operation-tooltip" type="text" slot="reference">删除</el-button>
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
import {dateFormat, setCopyContent, successMessage} from "@/util/utils"

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
                return {}
            }
        },
        profileTable: {
            type: Object,
            default: () => {
                return {}
            }
        },
        statusTable: {
            type: Object,
            default: () => {
                return {}
            }
        },
        data: {
            type: Object,
            default: () => {
                return {
                    name: "",
                    driverId: "",
                    profileIds: [],
                    multi: false,
                    enable: true,
                    description: "",
                    createTime: "",
                    updateTime: ""
                }
            }
        },
        icon: {
            type: String,
            default: "images/common/device.png"
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
            this.$emit("delete-thing", id, () => {
                successMessage();
            });
        },
        edit(id, driverId) {
            this.$router.push({name: "deviceEdit", query: {id, driverId, active: "0"}})
                .catch(() => {
                });
        },
        detail(id, driverId) {
            this.$router.push({name: "deviceDetail", query: {id, driverId, active: "detail"}})
                .catch(() => {
                });
        },
        copyId(content) {
            setCopyContent(content, true, "设备ID");
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

