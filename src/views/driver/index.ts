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

import { reactive } from "vue";

import blankCard from "@/components/card/blank-card.vue"
import skeletonCard from "@/components/card/skeleton-card.vue"
import driverTool from "./tool/DriverTool.vue"
import driverCard from "./card/DriverCard.vue"
import { driverList, driverStatus } from "@/api/driver"

export default {
    name: "Driver",
    components: {
        blankCard,
        skeletonCard,
        driverTool,
        driverCard
    },
    setup() {
        let reactiveData = reactive({
            loading: true,
            statusTable: {},
            listData: [{} as any],
            query: {
                type: "driver",
            },
            order: false,
            page: {
                total: 0,
                size: 12,
                current: 1,
                orders: [{} as any]
            },
        })

        const list = () => {
            driverList({
                page: reactiveData.page,
                ...reactiveData.query,
            })
                .then((res) => {
                    const data = res.data.data
                    reactiveData.page.total = data.total
                    reactiveData.listData = data.records
                })
                .finally(() => {
                    reactiveData.loading = false
                })

            driverStatus({
                page: reactiveData.page,
                ...reactiveData.query,
            })
                .then((res) => {
                    reactiveData.statusTable = res.data.data
                })
        }

        const search = (params) => {
            reactiveData.query = {...params, type: "driver"}
            list()
        }

        const reset = () => {
            reactiveData.query = {type: "driver"}
            list()
        }

        const refresh = () => {
            list()
        }

        const sort = () => {
            reactiveData.order = !reactiveData.order
            if (reactiveData.order) {
                reactiveData.page.orders = [{column: "create_time", asc: true}]
            } else {
                reactiveData.page.orders = [{column: "create_time", asc: false}]
            }
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
            search,
            reset,
            refresh,
            sort,
            sizeChange,
            currentChange
        }
    }
}