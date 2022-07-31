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

import { defineComponent, reactive } from 'vue'
import { CollectionTag, Edit, Management, Sunset } from '@element-plus/icons-vue'

import { useRoute } from 'vue-router'
import router from '@/config/router'

import { deviceByDriverIdApi, deviceStatusByDriverIdApi } from '@/api/device'
import { profileByIdsApi } from '@/api/profile'
import { driverByIdsApi } from '@/api/driver'
import { driverByIdApi } from '@/api/driver'

import baseCard from '@/components/card/base/BaseCard.vue'
import detailCard from '@/components/card/detail/DetailCard.vue'
import deviceCard from '@/views/device/card/DeviceCard.vue'
import pointCard from '@/views/point/card/PointCard.vue'

import { timestamp } from '@/util/CommonUtils'

export default defineComponent({
    components: {
        baseCard,
        detailCard,
        deviceCard,
        pointCard,
        CollectionTag,
        Edit,
        Sunset,
        Management,
    },
    setup() {
        const route = useRoute()

        // 定义响应式数据
        const reactiveData = reactive({
            id: route.query.id as string,
            active: route.query.active,
            driverTable: {},
            profileTable: {},
            statusTable: {},
            data: {} as any,
            listDeviceData: [] as any[],
        })

        const driver = () => {
            driverByIdApi(reactiveData.id)
                .then((res) => {
                    reactiveData.data = res.data.data
                })
                .catch(() => {
                    // nothing to do
                })
        }

        const device = () => {
            deviceByDriverIdApi(reactiveData.id)
                .then((res) => {
                    reactiveData.listDeviceData = res.data.data

                    // driver
                    const driverIds = Array.from(new Set(reactiveData.listDeviceData.map((device) => device.driverId)))
                    driverByIdsApi(driverIds)
                        .then((res) => {
                            reactiveData.driverTable = res.data.data
                        })
                        .catch(() => {
                            // nothing to do
                        })

                    // profile
                    const profileIds = Array.from(
                        new Set(
                            reactiveData.listDeviceData.reduce((pre, cur) => {
                                pre.push(...cur.profileIds)
                                return pre
                            }, [])
                        )
                    )
                    profileByIdsApi(profileIds)
                        .then((res) => {
                            reactiveData.profileTable = res.data.data
                        })
                        .catch(() => {
                            // nothing to do
                        })
                })
                .catch(() => {
                    // nothing to do
                })

            deviceStatusByDriverIdApi(reactiveData.id)
                .then((res) => {
                    reactiveData.statusTable = res.data.data
                })
                .catch(() => {
                    // nothing to do
                })
        }

        const deviceName = () => {
            return reactiveData.listDeviceData.map((device) => device.name).join(', ')
        }

        const changeActive = (tab) => {
            const query = route.query
            router.push({ query: { ...query, active: tab.props.name } }).catch(() => {
                // nothing to do
            })
        }

        driver()
        device()

        return {
            reactiveData,
            driver,
            device,
            deviceName,
            changeActive,
            timestamp,
        }
    },
})
