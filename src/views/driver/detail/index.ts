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

import { reactive } from "vue"
import { useRoute } from 'vue-router'
import router from "@/config/router"

import blankCard from "@/components/card/blank-card.vue"
import baseCard from "@/components/card/base-card.vue"
import detailCard from "@/components/card/detail-card.vue"
import skeletonCard from "@/components/card/skeleton-card.vue"
import driverTool from "../tool/DriverTool.vue"
import deviceList from "../../device/DeviceList.vue"
import driverCard from "../card/DriverCard.vue"
import deviceCard from "../../device/DeviceCard.vue"
import pointCard from "../../point/PointCard.vue"

import { deviceByDriverId, deviceStatusByDriverId } from "@/api/device"
import { driverDictionary, profileDictionary } from "@/api/dictionary"
import { driverById, driverList, driverStatus } from "@/api/driver"

import { dateFormat, setCopyContent } from "@/util/utils"

export default {
    name: "DriverDetail",
    components: {
        blankCard,
        baseCard,
        detailCard,
        skeletonCard,
        driverTool,
        deviceList,
        driverCard,
        deviceCard,
        pointCard
    },
    setup() {
        const route = useRoute()

        let reactiveData = reactive({
            id: route.query.id as string,
            active: route.query.active,
            driverLoading: true,
            deviceLoading: true,
            data: {} as any,
            driverTable: {} as any,
            profileTable: {} as any,
            driverStatusTable: {} as any,
            deviceStatusTable: {} as any,
            listDriverData: [{} as any],
            listDeviceData: [{} as any],
            query: {
                type: "driver"
            },
            order: false,
            page: {
                total: 0,
                size: 12,
                current: 1,
                orders: [{} as any]
            }
        })

        const driver = () => {
            driverById(reactiveData.id).then(res => {
                reactiveData.data = res.data.data
            })
        }

        const device = () => {
            deviceByDriverId(reactiveData.id).then(res => {
                reactiveData.listDeviceData = res.data.data
            }).catch(() => {
                reactiveData.listDeviceData = []
            }).finally(() => {
                reactiveData.deviceLoading = false
            })

            deviceStatusByDriverId(reactiveData.id).then(res => {
                reactiveData.deviceStatusTable = res.data.data
            }).catch(() => {
                reactiveData.deviceStatusTable = {}
            })
        }

        const drivers = () => {
            driverDictionary().then(res => {
                reactiveData.driverTable = res.data.data.reduce((pre, cur) => {
                    pre[cur.value] = cur.label
                    return pre
                }, {})
            })
        }

        const profiles = () => {
            profileDictionary().then(res => {
                reactiveData.profileTable = res.data.data.reduce((pre, cur) => {
                    pre[cur.value] = cur.label
                    return pre
                }, {})
            })
        }

        const list = () => {
            driverList({
                page: reactiveData.page,
                ...reactiveData.query
            }).then(res => {
                const data = res.data.data
                reactiveData.page.total = data.total
                data.records.forEach(driver => driver.active = reactiveData.id === driver.id)
                reactiveData.listDriverData = data.records
            }).finally(() => {
                reactiveData.driverLoading = false
            })

            driverStatus({
                page: reactiveData.page,
                ...reactiveData.query
            }).then(res => {
                reactiveData.driverStatusTable = res.data.data
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
            reactiveData.order = !reactiveData.order;
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

        const deviceName = () => {
            return reactiveData.listDeviceData.map(device => device.name).join(", ")
        }

        const changeActive = (tab) => {
            reactiveData.active = tab.name
            let query = route.query;
            router.push({query: {...query, active: tab.name}})
        }

        const selectChange = (data) => {
            reactiveData.listDriverData.forEach(driver => driver.active = data.id === driver.id)

            reactiveData.id = data.id
            let query = route.query
            router.push({query: {...query, id: data.id}})
                .then(() => {
                    driver()
                    device()
                })
        }

        // 复制ID
        const copyId = (content) => {
            setCopyContent(content, true, "驱动ID")
        }

        // 格式化时间
        const timestamp = (timestamp) => {
            return dateFormat(new Date(timestamp))
        }

        // 加载数据
        driver()
        device()
        drivers()
        profiles()
        list()

        return {
            reactiveData,
            search,
            reset,
            refresh,
            sort,
            sizeChange,
            currentChange,
            deviceName,
            changeActive,
            selectChange,
            copyId,
            timestamp
        }
    }
}