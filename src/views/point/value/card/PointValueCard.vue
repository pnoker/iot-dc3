<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -      https://www.apache.org/licenses/LICENSE-2.0
  -
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
                <div
                    :class="{
                        'header-enable': data.interval < 200,
                        'header-disable': data.interval >= 200
                    }"
                    class="things-card__header"
                >
                    <div class="things-card-header-icon">
                        <img :alt="data.pointName" :src="icon" />
                    </div>
                    <div class="things-card-header-name nowrap-name" @click="copy(data.pointId, '位号值ID')">
                        {{ point.pointName }}
                    </div>
                    <div class="things-card-header-status" title="读写标识">
                        <el-tag v-if="data.rwFlag === 'R'" effect="plain" type="warning">只读</el-tag>
                        <el-tag v-else-if="data.rwFlagrw === 'W'" effect="plain" type="info">只写</el-tag>
                        <el-tag v-else-if="data.rwFlag === 'RW'" effect="plain" type="success">读写</el-tag>
                    </div>
                </div>
                <div class="things-card__body">
                    <div class="things-card-body-content">
                        <div class="things-card-body-content-column">
                            <div class="things-card-body-content-value">
                                <span class="nowrap-item value" title="处理值, 点击复制" @click="copyValue(data)">{{ data.value }} {{ unit }}</span>
                            </div>
                            <ul>
                                <li class="nowrap-item">
                                    <el-icon>
                                        <Sunrise />
                                    </el-icon>
                                    原始值: {{ data.rawValue }}
                                </li>
                                <li v-if="embedded == ''" class="nowrap-item value-point">
                                    <el-icon>
                                        <Management />
                                    </el-icon>
                                    所属设备: {{ device.deviceName }}
                                </li>
                                <li class="nowrap-item">
                                    <el-icon>
                                        <Timer />
                                    </el-icon>
                                    数据延时: {{ data.interval }} ms
                                </li>
                                <li class="nowrap-item">
                                    <el-icon>
                                        <Edit />
                                    </el-icon>
                                    采集日期: {{ timestamp(data.originTime) }}
                                </li>
                                <li class="nowrap-item">
                                    <el-icon>
                                        <Sunset />
                                    </el-icon>
                                    保存日期: {{ timestamp(data.createTime) }}
                                </li>
                            </ul>
                        </div>
                    </div>
                    <div v-if="embedded != ''" class="things-card-body-content-time">
                        <div :id="data.pointId"></div>
                    </div>
                </div>
                <div v-if="embedded == ''" class="things-card__footer">
                    <div class="things-card-footer-operation">
                        <el-popconfirm :icon="CircleClose" icon-color="#f56c6c" placement="top" title="是否确定删除该数据?该数据下的配置将会被全部删除, 且该操作不可恢复!">
                            <template #reference>
                                <el-button link type="primary">删除</el-button>
                            </template>
                        </el-popconfirm>
                        <el-button link type="primary">编辑</el-button>
                        <el-button link type="primary">详情</el-button>
                    </div>
                </div>
            </div>
        </el-card>
    </div>
</template>

<script lang="ts" setup>
import { onMounted, watch } from 'vue'
import { CircleClose, Edit, Management, Sunrise, Sunset, Timer } from '@element-plus/icons-vue'

import { Chart } from '@antv/g2'

import { copy, timestamp } from '@/utils/CommonUtil'
import { getPointValueHistory } from '@/api/point'

const props = defineProps({
    embedded: {
        type: String,
        default: () => {
            return ''
        }
    },
    data: {
        type: Object,
        default: () => {
            return {}
        }
    },
    device: {
        type: Object,
        default: () => {
            return {}
        }
    },
    point: {
        type: Object,
        default: () => {
            return {}
        }
    },
    unit: {
        type: String,
        default: ''
    },
    icon: {
        type: String,
        default: 'images/common/point.png'
    }
})

const copyValue = (data) => {
    const content = {
        deviceId: data.deviceId,
        pointId: data.pointId,
        value: data.value
    }
    copy(JSON.stringify(content, null, 2), '位号值')
}

let tinyArea: Chart
const history = () => {
    getPointValueHistory(props.data.deviceId, props.data.pointId, 100)
        .then((res) => {
            let historyData: number[]
            const pointValueType = props.point.pointTypeFlag.toLowerCase()
            if (pointValueType === 'string') {
                historyData = []
            } else if (pointValueType === 'boolean') {
                historyData = res.data.reverse().map((value: string) => (value === 'true' ? 1 : 0))
            } else {
                historyData = res.data.reverse().map((value: string) => +value)
            }

            tinyArea.changeData(historyData)
        })
        .catch(() => {
            // nothing to do
        })
}

watch(
    () => props.data,
    () => {
        if (props.embedded != '') {
            history()
        }
    }
)

onMounted(() => {
    window.dispatchEvent(new Event('resize'))

    if (props.embedded != '') {
        tinyArea = new TinyArea(props.data.pointId, {
            height: 60,
            data: [],
            autoFit: true,
            smooth: true,
            annotations: [
                {
                    type: 'line',
                    start: ['min', 'mean'],
                    end: ['max', 'mean'],
                    text: {
                        content: 'AVG',
                        offsetY: -5,
                        style: {
                            textAlign: 'left',
                            fontSize: 10,
                            fill: 'rgba(44, 53, 66, 0.45)',
                            textBaseline: 'bottom'
                        }
                    },
                    style: {
                        stroke: 'rgba(0, 0, 0, 0.25)'
                    }
                }
            ]
        })

        tinyArea.render()

        history()
    }
})
</script>

<style lang="scss">
@use '@/components/card/styles/things-card';
</style>
