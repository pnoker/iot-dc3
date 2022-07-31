/*
 * Copyright (c) 2022. Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { computed, defineComponent, reactive, onMounted } from 'vue'
import { CircleClose, Edit, Management, Sunrise, Sunset, Timer, ZoomIn } from '@element-plus/icons-vue'

import { TinyArea } from '@antv/g2plot'

import { setCopyContent } from '@/util/utils'
import { copyId, timestamp } from '@/util/CommonUtils'

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
            type: Boolean,
            default: () => {
                return false
            },
        },
        data: {
            type: Object,
            default: () => {
                return {}
            },
        },
        historyData: {
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

        const line = computed(() => {
            const data = props.embedded ? props.historyData[props.data.pointId] : []
            return data || []
        })

        // 图标
        const Icon = {
            CircleClose,
        }

        const showEdit = (pointValue) => {
            emit('show-edit', pointValue)
        }

        const showDetail = () => {
            const data = props.data
            let dataValue = data.value
            try {
                dataValue = JSON.parse(data.value)
            } catch (ignore) {
                // nothing to do
            }

            const content = {
                deviceId: data.deviceId,
                pointId: data.pointId,
                type: data.type,
                value: dataValue,
                calculateValue: data.calculateValue,
                rawValue: data.rawValue,
            }

            emit('show-detail', content)
        }

        const copyValue = (data) => {
            const content = {
                deviceId: data.deviceId,
                pointId: data.pointId,
                value: data.value,
            }
            setCopyContent(JSON.stringify(content, null, 2), true, '位号值')
        }

        onMounted(() => {
            const data = props.embedded ? props.historyData[props.data.pointId] : []
            if (props.embedded && data) {
                const tinyArea = new TinyArea(props.data.id, {
                    height: 60,
                    data,
                    autoFit: true,
                    smooth: true,
                })

                tinyArea.render()
            }
        })

        return {
            reactiveData,
            line,
            showEdit,
            showDetail,
            copyId,
            copyValue,
            timestamp,
            ...Icon,
        }
    },
})
