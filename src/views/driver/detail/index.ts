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
import { Connection, Edit, Management, Monitor, Position, Promotion, Sunset } from "@element-plus/icons-vue"

import { useRoute } from 'vue-router'
import router from "@/config/router"

import { Order } from "@/config/type/types"

import blankCard from "@/components/card/blank/BlankCard.vue"
import baseCard from "@/components/card/base/BaseCard.vue"
import detailCard from "@/components/card/detail/DetailCard.vue"
import skeletonCard from "@/components/card/skeleton/SkeletonCard.vue"
import driverTool from "@/views/driver/tool/DriverTool.vue"
import deviceList from "@/views/device/Device.vue"
import driverCard from "@/views/driver/card/DriverCard.vue"
import deviceCard from "@/views/device/card/DeviceCard.vue"
import pointCard from "@/views/point/card/PointCard.vue"

import { deviceByDriverIdApi, deviceStatusByDriverIdApi } from "@/api/device"
import { driverByIdApi, driverListApi, driverStatusApi, driverByIdsApi } from "@/api/driver"
import { profileByIdsApi } from "@/api/profile"
import { timestamp } from "@/util/CommonUtils"

export default defineComponent({
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
        pointCard,
        Position,
        Promotion,
        Edit,
        Sunset,
        Management,
        Connection,
        Monitor
    },
    setup() {
        const route = useRoute()

        // 定义响应式数据
        const reactiveData = reactive({
            id: route.query.id as string,
            active: route.query.active,
            driverLoading: true,
            deviceLoading: true,
            data: {} as any,
            driverTable: {} as any,
            profileTable: {} as any,
            driverStatusTable: {} as any,
            deviceStatusTable: {} as any,
            listDriverData: [] as any[],
            listDeviceData: [] as any[],
            query: {
                type: "driver"
            },
            order: false,
            page: {
                total: 0,
                size: 12,
                current: 1,
                orders: [] as Order[]
            }
        })

        const hasDriverData = computed(() => {
            return !reactiveData.driverLoading && reactiveData.listDriverData?.length < 1
        })

        const deviceName = computed(() => {
            return reactiveData.listDeviceData.map(device => device.name).join(", ") || "-"
        })

        const hasDeviceData = computed(() => {
            return !reactiveData.deviceLoading && reactiveData.listDeviceData?.length < 1
        })

        const driver = () => {
            driverByIdApi(reactiveData.id).then(res => {
                reactiveData.data = res.data.data
            })
        }

        const device = () => {
            deviceByDriverIdApi(reactiveData.id).then(res => {
                reactiveData.listDeviceData = res.data.data

                // driver 
                const driverIds = Array.from(new Set(reactiveData.listDeviceData.map(device => device.driverId)))
                driverByIdsApi(driverIds).then(res => {
                    reactiveData.driverTable = res.data.data
                }).catch(() => {
                    // nothing to do
                })

                // profile
                const profileIds = Array.from(new Set(reactiveData.listDeviceData.reduce((pre, cur) => {
                    pre.push(...cur.profileIds)
                    return pre
                }, [])))
                profileByIdsApi(profileIds).then(res => {
                    reactiveData.profileTable = res.data.data
                }).catch(() => {
                    // nothing to do
                })
            }).catch(() => {
                reactiveData.listDeviceData = []
            }).finally(() => {
                reactiveData.deviceLoading = false
            })

            deviceStatusByDriverIdApi(reactiveData.id).then(res => {
                reactiveData.deviceStatusTable = res.data.data
            }).catch(() => {
                reactiveData.deviceStatusTable = {}
            })
        }

        const list = () => {
            driverListApi({
                page: reactiveData.page,
                ...reactiveData.query
            }).then(res => {
                const data = res.data.data
                reactiveData.page.total = data.total
                data.records.forEach(driver => driver.active = reactiveData.id === driver.id)
                reactiveData.listDriverData = data.records
            }).catch(() => {
                // nothing to do
            }).finally(() => {
                reactiveData.driverLoading = false
            })

            driverStatusApi({
                page: reactiveData.page,
                ...reactiveData.query
            }).then(res => {
                reactiveData.driverStatusTable = res.data.data
            })
        }

        const search = (params) => {
            reactiveData.query = { ...params, type: "driver" }
            list()
        }

        const reset = () => {
            reactiveData.query = { type: "driver" }
            list()
        }

        const refresh = () => {
            list()
        }

        const sort = () => {
            reactiveData.order = !reactiveData.order;
            if (reactiveData.order) {
                reactiveData.page.orders = [{ column: "create_time", asc: true }]
            } else {
                reactiveData.page.orders = [{ column: "create_time", asc: false }]
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

        const changeActive = (tab) => {
            reactiveData.active = tab.name
            const query = route.query;
            router.push({ query: { ...query, active: tab.props.name } })
        }

        const selectChange = (data) => {
            reactiveData.listDriverData.forEach(driver => driver.active = data.id === driver.id)

            reactiveData.id = data.id
            const query = route.query
            router.push({ query: { ...query, id: data.id } })
                .then(() => {
                    driver()
                    device()
                })
        }

        driver()
        device()
        list()

        return {
            reactiveData,
            hasDriverData,
            deviceName,
            hasDeviceData,
            search,
            reset,
            refresh,
            sort,
            sizeChange,
            currentChange,
            changeActive,
            selectChange,
            timestamp
        }
    }
})