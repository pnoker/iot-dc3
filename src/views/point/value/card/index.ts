/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { defineComponent, onMounted, reactive } from 'vue'
import { CircleClose, Edit, Management, Sunrise, Sunset, Timer, ZoomIn } from '@element-plus/icons-vue'

import { TinyArea } from '@antv/g2plot'

import { copy, timestamp } from '@/utils/CommonUtil'

export default defineComponent({
    name: 'PointValueCard',
    components: {
        Edit,
        Sunset,
        Timer,
        Management,
        Sunrise,
        ZoomIn,
    },
    props: {
        embedded: {
            type: String,
            default: () => {
                return ''
            },
        },
        data: {
            type: Object,
            default: () => {
                return {}
            },
        },
        device: {
            type: Object,
            default: () => {
                return {}
            },
        },
        point: {
            type: Object,
            default: () => {
                return {}
            },
        },
        unit: {
            type: String,
            default: '',
        },
        historyData: {
            type: Array<number>,
            default: () => {
                return []
            },
        },
        icon: {
            type: String,
            default: 'images/common/point.png',
        },
    },
    emits: ['show-edit', 'show-detail'],
    setup(props, { emit }) {
        // 定义响应式数据
        const reactiveData = reactive({
            spRefLineStyles: {
                stroke: '#54a5ff',
                strokeOpacity: 0.5,
                strokeDasharray: '2, 2',
            },
            spCurveStyles: {
                stroke: '#54a5ff',
            },
        })

        // 图标
        const Icon = {
            CircleClose,
        }

        const showEdit = (pointValue) => {
            emit('show-edit', pointValue)
        }

        const copyValue = (data) => {
            const content = {
                deviceId: data.deviceId,
                pointId: data.pointId,
                value: data.value,
            }
            copy(JSON.stringify(content, null, 2), '位号值')
        }

        onMounted(() => {
            if (props.embedded == '') {
                return
            }

            const tinyArea = new TinyArea(props.data.pointId, {
                height: 60,
                data: props.historyData,
                autoFit: true,
                smooth: true,
            })

            tinyArea.render()
        })

        return {
            reactiveData,
            showEdit,
            copyId: copy,
            copyValue,
            timestamp,
            ...Icon,
        }
    },
})
