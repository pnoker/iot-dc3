/*
 * Copyright 2022 Pnoker All Rights Reserved
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

import { defineComponent, reactive, computed } from 'vue'

import { pointUnitApi, pointValueListApi, pointByIdsApi, pointValueLatestApi } from '@/api/point'
import { deviceByIdsApi } from '@/api/device'

import blankCard from '@/components/card/blank/BlankCard.vue'
import skeletonCard from '@/components/card/skeleton/SkeletonCard.vue'
import pointValueTool from './tool/PointValueTool.vue'
import pointValueCard from './card/PointValueCard.vue'

import { isNull } from '@/util/utils'

export default defineComponent({
    components: {
        blankCard,
        skeletonCard,
        pointValueTool,
        pointValueCard,
    },
    props: {
        embedded: {
            type: String,
            default: () => {
                return ''
            },
        },
        deviceId: {
            type: String,
            default: () => {
                return ''
            },
        },
    },
    setup(props) {
        // 定义响应式数据
        const reactiveData = reactive({
            loading: true,
            deviceTable: {},
            pointTable: {},
            unitTable: {},
            historyData: {},
            listData: [] as any[],
            query: {},
            page: {
                total: 0,
                size: 12,
                current: 1,
            },
        })

        const hasData = computed(() => {
            return !reactiveData.loading && reactiveData.listData?.length < 1
        })

        const list = () => {
            if (!isNull(props.deviceId)) {
                reactiveData.query = {
                    ...reactiveData.query,
                    deviceId: props.deviceId,
                }
            }

            if (props.embedded == 'device') {
                pointValueLatestApi({
                    page: reactiveData.page,
                    ...reactiveData.query,
                    history: true,
                })
                    .then((res) => {
                        loadPointValueList(res)
                    })
                    .catch(() => {
                        // nothing to do
                    })
                    .finally(() => {
                        reactiveData.loading = false
                    })
            } else {
                pointValueListApi({
                    page: reactiveData.page,
                    ...reactiveData.query,
                })
                    .then((res) => {
                        loadPointValueList(res)
                    })
                    .catch(() => {
                        // nothing to do
                    })
                    .finally(() => {
                        reactiveData.loading = false
                    })
            }
        }

        const loadPointValueList = (res) => {
            reactiveData.listData = res.data.data.records.map((record) => {
                const tempDate1 = new Date(record.createTime)
                const tempDate2 = new Date(record.originTime)
                record.interval = tempDate1.getTime() - tempDate2.getTime()
                return record
            })
            reactiveData.page.total = res.data.data.total

            // device
            const deviceIds = Array.from(new Set(reactiveData.listData.map((pointValue) => pointValue.deviceId)))
            if (deviceIds.length > 0) {
                deviceByIdsApi(deviceIds)
                    .then((res) => {
                        reactiveData.deviceTable = res.data.data
                    })
                    .catch(() => {
                        // nothing to do
                    })
            }

            // point & unit
            const pointIds = Array.from(new Set(reactiveData.listData.map((pointValue) => pointValue.pointId)))
            if (pointIds.length > 0) {
                pointByIdsApi(pointIds)
                    .then((res) => {
                        reactiveData.pointTable = res.data.data
                    })
                    .catch(() => {
                        // nothing to do
                    })

                pointUnitApi(pointIds)
                    .then((res) => {
                        reactiveData.unitTable = res.data.data
                    })
                    .catch(() => {
                        // nothing to do
                    })
            }

            // history point value
            if (props.embedded == 'device') {
                reactiveData.listData.forEach((pointValue) => {
                    if (pointValue.type === 'string') {
                        reactiveData.historyData[pointValue.pointId] = []
                    } else if (pointValue.type === 'boolean') {
                        reactiveData.historyData[pointValue.pointId] = pointValue.children
                            .reverse()
                            .map((value) => (value === 'true' ? 1 : 0))
                    } else {
                        reactiveData.historyData[pointValue.pointId] = pointValue.children
                            .reverse()
                            .map((value) => +value)
                    }
                })
            }
        }

        const search = (params) => {
            reactiveData.query = params
            list()
        }

        const reset = () => {
            reactiveData.query = {}
            list()
        }

        const refresh = () => {
            list()
        }

        const sizeChange = (size) => {
            reactiveData.page.size = size
            list()
        }

        const currentChange = (current) => {
            reactiveData.page.current = current
            list()
        }

        list()

        return {
            reactiveData,
            hasData,
            search,
            reset,
            refresh,
            sizeChange,
            currentChange,
        }
    },
})
