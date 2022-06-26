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
    <div class="things-card" @click="select(data)">
        <el-card :shadow="data.active?'always':'hover'" v-bind:class="{'active':data.active}">
            <div class="things-card-content">
                <div class="things-card__header" v-bind:class="{'header-enable':data.enable,'header-disable':!data.enable}">
                    <div class="things-card-header-icon"><img :src="icon" :alt="data.name"></div>
                    <div class="things-card-header-name nowrap-name" @click="copyId(data.id)">{{ data.name }}</div>
                    <div title="状态" class="things-card-header-status" v-bind:class="{'active':data.active}">
                        <el-tag v-if="status(data.id)==='ONLINE'" type="success" effect="plain">在线</el-tag>
                        <el-tag v-else-if="status(data.id)==='MAINTAIN'" type="warning" effect="plain">维护</el-tag>
                        <el-tag v-else-if="status(data.id)==='FAULT'" type="danger" effect="plain">故障</el-tag>
                        <el-tag v-else-if="status(data.id)==='DISABLE'" type="info" effect="plain">停用</el-tag>
                        <el-tag v-else-if="status(data.id)==='UNREGISTERED'" type="info" effect="plain">未注册</el-tag>
                        <el-tag v-else type="info" effect="plain">离线</el-tag>
                    </div>
                </div>
                <div class="things-card__body">
                    <div class="things-card-body-content">
                        <ul>
                            <li class="nowrap-item"><span><i class="el-icon-connection"/> 端口: </span>{{ data.port }}</li>
                            <li class="nowrap-item"><span><i class="el-icon-monitor"/> 主机: </span>{{ data.host }}</li>
                            <li class="nowrap-item"><span><i class="el-icon-s-promotion"/> 驱动服务: </span>{{ data.serviceName }}</li>
                            <li class="nowrap-item"><span><i class="el-icon-edit-outline"/> 修改日期: </span>{{ timestamp(data.updateTime) }}</li>
                            <li class="nowrap-item"><span><i class="el-icon-sunset"/> 创建日期: </span>{{ timestamp(data.createTime) }}</li>
                        </ul>
                    </div>
                    <div :title="data.description?data.description:'驱动描述信息'" class="things-card-body-content">
                        <p class="nowrap-description">{{ data.description ? data.description : "无描述信息" }}</p>
                    </div>
                </div>
                <div v-if="!footer" class="things-card__footer">
                    <div class="things-card-footer-operation">
                        <el-popconfirm title="是否确定停用该驱动？" placement="top" icon="el-icon-remove-outline">
                            <el-button class="operation-tooltip" type="text" slot="reference" disabled>停用</el-button>
                        </el-popconfirm>
                        <el-popconfirm title="是否确定启用该驱动？" placement="top" icon="el-icon-remove-outline">
                            <el-button class="operation-tooltip" type="text" slot="reference" disabled>启用</el-button>
                        </el-popconfirm>
                        <el-popconfirm title="是否确定删除该驱动？" placement="top" icon="el-icon-remove-outline">
                            <el-button class="operation-tooltip" type="text" slot="reference" disabled>删除</el-button>
                        </el-popconfirm>
                        <el-button type="text" @click="detail(data.id)">详情</el-button>
                    </div>
                </div>
            </div>
        </el-card>
    </div>
</template>

<script lang="ts">
import DriverCard from "@/views/driver/card"

export default DriverCard
</script>

<style lang="less">
@import "~@/components/card/styles/things-card.scss";
@import "~@/views/driver/card/style.less";
</style>
