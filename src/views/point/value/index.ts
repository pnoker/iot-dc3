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

import { defineComponent, reactive, computed } from "vue"

import { pointUnitApi, pointValueListApi, pointByIdsApi } from "@/api/point"
import { deviceByIdsApi } from "@/api/device"
import { deviceDictionaryApi, pointDictionaryApi } from "@/api/dictionary"

import { Dictionary } from "@/config/type/types"

import blankCard from "@/components/card/blank/BlankCard.vue"
import skeletonCard from "@/components/card/skeleton/SkeletonCard.vue"
import pointValueTool from "./tool/PointValueTool.vue"
import pointValueCard from "./card/PointValueCard.vue"

export default defineComponent({
    components: {
        blankCard,
        skeletonCard,
        pointValueTool,
        pointValueCard
    },

    setup() {
        // 定义响应式数据
        const reactiveData = reactive({
            loading: true,
            deviceDictionary: [] as Dictionary[],
            pointDictionary: [] as Dictionary[],
            deviceTable: {},
            pointTable: {},
            unitTable: {},
            listData: [] as any[],
            query: {},
            page: {
                total: 0,
                size: 12,
                current: 1
            }
        })

        const hasData = computed(() => {
            return !reactiveData.loading && reactiveData.listData?.length < 1
        })

        const list = () => {
            pointValueListApi({
                page: reactiveData.page,
                ...reactiveData.query
            }).then(res => {
                reactiveData.listData = res.data.data.records.map(record => {
                    const tempDate1 = new Date(record.createTime)
                    const tempDate2 = new Date(record.originTime)
                    record.interval = tempDate1.getTime() - tempDate2.getTime()
                    return record
                })
                reactiveData.page.total = res.data.data.total

                // device
                const deviceIds = Array.from(new Set(reactiveData.listData.map(pointValue => pointValue.deviceId)));
                if (deviceIds.length > 0) {
                    deviceByIdsApi(deviceIds).then(res => {
                        reactiveData.deviceTable = res.data.data
                    }).catch(() => {
                        // nothing to do
                    })
                }

                // point & unit
                const pointIds = Array.from(new Set(reactiveData.listData.map(pointValue => pointValue.pointId)));
                if (pointIds.length > 0) {
                    pointByIdsApi(pointIds).then(res => {
                        reactiveData.pointTable = res.data.data
                    }).catch(() => {
                        // nothing to do
                    })

                    pointUnitApi(pointIds).then(res => {
                        reactiveData.unitTable = res.data.data
                    }).catch(() => {
                        // nothing to do
                    })
                }
            }).catch(() => {
                // nothing to do
            }).finally(() => {
                reactiveData.loading = false
            })
        }

        const device = () => {
            deviceDictionaryApi().then(res => {
                reactiveData.deviceDictionary = res.data.data
            }).catch(() => {
                // nothing to do
            });
        }

        const point = () => {
            pointDictionaryApi("profile").then(res => {
                reactiveData.pointDictionary = res.data.data
            }).catch(() => {
                // nothing to do
            });
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

        device()
        point()
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
    }
})