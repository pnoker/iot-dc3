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
import { CollectionTag, Edit, List, Management, Sunset } from "@element-plus/icons-vue"

import { profileByIdApi, profileByIdsApi } from "@/api/profile"
import { pointByProfileIdApi } from "@/api/point"
import { deviceByProfileIdApi, deviceStatusByProfileIdApi } from "@/api/device"
import { driverByIdsApi } from "@/api/driver"

import router from "@/config/router";
import { useRoute } from "vue-router";

import baseCard from "@/components/card/base/BaseCard.vue"
import detailCard from "@/components/card/detail/DetailCard.vue"
import skeletonCard from "@/components/card/skeleton/SkeletonCard.vue"
import deviceCard from "@/views/device/card/DeviceCard.vue"
import pointCard from "@/views/point/card/PointCard.vue"

import { timestamp } from "@/util/CommonUtils"

export default defineComponent({
    components: {
        baseCard,
        detailCard,
        skeletonCard,
        deviceCard,
        pointCard,
        List,
        CollectionTag,
        Management,
        Edit,
        Sunset
    },
    setup() {
        const route = useRoute()

        // 定义响应式数据
        const reactiveData = reactive({
            id: route.query.id as string,
            active: route.query.active,
            deviceLoading: true,
            pointLoading: true,
            driverTable: {} as any,
            profileTable: {} as any,
            statusTable: {} as any,
            data: {} as any,
            listDeviceData: [] as any[],
            listPointData: [] as any[]
        })

        const pointName = computed(() => {
            return reactiveData.listPointData.map(point => point.name).join(", ") || "-"
        })

        const hasPointData = computed(() => {
            return !reactiveData.pointLoading && reactiveData.listPointData?.length < 1
        })

        const deviceName = computed(() => {
            return reactiveData.listDeviceData.map(device => device.name).join(", ") || "-"
        })

        const hasDeviceData = computed(() => {
            return !reactiveData.deviceLoading && reactiveData.listDeviceData?.length < 1
        })

        const profile = () => {
            profileByIdApi(reactiveData.id).then(res => {
                reactiveData.data = res.data.data
            })
        }

        const device = () => {
            deviceByProfileIdApi(reactiveData.id).then(res => {
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
                // nothing to do
            }).finally(() => {
                reactiveData.deviceLoading = false
            })

            deviceStatusByProfileIdApi(reactiveData.id).then(res => {
                reactiveData.statusTable = res.data.data
            })
        }

        const point = () => {
            pointByProfileIdApi(reactiveData.id).then(res => {
                reactiveData.listPointData = res.data.data
            }).catch(() => {
                // nothing to do
            }).finally(() => {
                reactiveData.pointLoading = false
            })
        }

        const changeActive = (tab) => {
            const query = route.query
            router.push({ query: { ...query, active: tab.props.name } })
            switch (tab.props.name) {
                case "device":
                    device()
                    break
                case "point":
                    point()
                    break
                default:
                    break
            }
        }

        profile()
        device()
        point()

        return {
            reactiveData,
            pointName,
            hasPointData,
            deviceName,
            hasDeviceData,
            changeActive,
            timestamp
        }
    }
})