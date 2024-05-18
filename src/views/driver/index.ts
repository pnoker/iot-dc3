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

import { defineComponent, reactive, computed } from 'vue'

import { getDriverList, getDriverStatus } from '@/api/driver'

import { Order } from '@/config/entity'

import blankCard from '@/components/card/blank/BlankCard.vue'
import skeletonCard from '@/components/card/skeleton/SkeletonCard.vue'
import driverTool from './tool/DriverTool.vue'
import driverCard from './card/DriverCard.vue'

export default defineComponent({
    name: 'Driver',
    components: {
        blankCard,
        skeletonCard,
        driverTool,
        driverCard
    },
    setup() {
        // 定义响应式数据
        const reactiveData = reactive({
            loading: true,
            statusTable: {},
            listData: [] as any[],
            query: {
                type: 'driver'
            },
            order: false,
            page: {
                total: 0,
                size: 12,
                current: 1,
                orders: [] as Order[]
            }
        })

        const hasData = computed(() => {
            return !reactiveData.loading && reactiveData.listData?.length < 1
        })

        const list = () => {
            getDriverList({
                page: reactiveData.page,
                ...reactiveData.query
            })
                .then((res) => {
                    const data = res.data
                    reactiveData.page.total = data.total
                    reactiveData.listData = data.records
                })
                .catch(() => {
                    // nothing to do
                })
                .finally(() => {
                    reactiveData.loading = false
                })

            getDriverStatus({
                page: reactiveData.page,
                ...reactiveData.query
            })
                .then((res) => {
                    reactiveData.statusTable = res.data
                })
                .catch(() => {
                    // nothing to do
                })
        }

        const search = (params) => {
            reactiveData.query = { ...params, type: 'driver' }
            list()
        }

        const reset = () => {
            reactiveData.query = { type: 'driver' }
            list()
        }

        const refresh = () => {
            list()
        }

        const sort = () => {
            reactiveData.order = !reactiveData.order
            if (reactiveData.order) {
                reactiveData.page.orders = [{ column: 'create_time', asc: true }]
            } else {
                reactiveData.page.orders = [{ column: 'create_time', asc: false }]
            }
            list()
        }

        const sizeChange = (size: number) => {
            reactiveData.page.size = size
            list()
        }

        const currentChange = (current: number) => {
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
            sort,
            sizeChange,
            currentChange
        }
    }
})
