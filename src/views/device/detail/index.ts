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

import { computed, defineComponent, reactive, ref } from 'vue'
import { CollectionTag, Edit, List, Management, Promotion, Sunset } from '@element-plus/icons-vue'

import { useRoute } from 'vue-router'
import router from '@/config/router'

import { deviceByIdApi } from '@/api/device'
import { driverByIdApi } from '@/api/driver'
import { profileByIdsApi } from '@/api/profile'
import { pointDictionaryApi, profileDictionaryApi } from '@/api/dictionary'
import { profileByDeviceIdApi } from '@/api/profile'
import { pointUnitApi, pointByDeviceIdApi, pointValueByDeviceIdApi, pointByIdsApi } from '@/api/point'

import { Dictionary } from '@/config/type/types'

import baseCard from '@/components/card/base/BaseCard.vue'
import detailCard from '@/components/card/detail/DetailCard.vue'
import skeletonCard from '@/components/card/skeleton/SkeletonCard.vue'
import deviceCard from '@/views/device/card/DeviceCard.vue'
import profileCard from '@/views/profile/card/ProfileCard.vue'
import pointCard from '@/views/point/card/PointCard.vue'
import pointValueCard from '@/views/point/value/card/PointValueCard.vue'
import pointValueEditForm from '@/views/point/value/edit/PointValueEditForm.vue'
import pointValueDetail from '@/views/point/value/detail/PointValueDetail.vue'

import { timestamp } from '@/util/CommonUtils'

export default defineComponent({
    name: 'DeviceDetail',
    components: {
        baseCard,
        detailCard,
        skeletonCard,
        deviceCard,
        profileCard,
        pointCard,
        pointValueCard,
        pointValueEditForm,
        pointValueDetail,
        Promotion,
        List,
        Management,
        Edit,
        Sunset,
        CollectionTag,
    },
    props: {
        name: {
            type: String,
            default: '',
        },
    },
    setup() {
        const route = useRoute()

        const pointValueDetailRef: any = ref<InstanceType<typeof pointValueDetail>>()

        // 定义响应式数据
        const reactiveData = reactive({
            id: route.query.id as string,
            active: route.query.active,
            profileLoading: true,
            pointLoading: true,
            pointValueLoading: true,
            data: {} as any,
            driver: {} as any,
            profileDictionary: [] as Dictionary[],
            pointDictionary: [] as Dictionary[],
            profileTable: {},
            pointTable: {},
            deviceTable: {},
            unitTable: {},
            listProfileData: [] as any[],
            listPointData: [] as any[],
            listPointValueData: [] as any[],
            listPointValueHistoryData: {},
            pointValueDetailData: {},
        })

        const hasProfileData = computed(() => {
            return !reactiveData.profileLoading && reactiveData.listProfileData?.length < 1
        })

        const hasPointData = computed(() => {
            return !reactiveData.pointLoading && reactiveData.listPointData?.length < 1
        })

        const hasPointValueData = computed(() => {
            return !reactiveData.pointValueLoading && reactiveData.listPointValueData?.length < 1
        })

        const device = () => {
            deviceByIdApi(reactiveData.id)
                .then((res) => {
                    reactiveData.data = res.data.data
                    reactiveData.deviceTable[reactiveData.data.id] = reactiveData.data.name

                    driverByIdApi(reactiveData.data.driverId)
                        .then((res) => {
                            reactiveData.driver = res.data.data
                        })
                        .catch(() => {
                            // nothing to do
                        })
                })
                .catch(() => {
                    // nothing to do
                })
        }

        const profiles = () => {
            profileByDeviceIdApi(reactiveData.id)
                .then((res) => {
                    reactiveData.listProfileData = res.data.data

                    // profile
                    const profileIds = Array.from(
                        new Set(reactiveData.listProfileData.map((pointValue) => pointValue.id))
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
                .finally(() => {
                    reactiveData.profileLoading = false
                })
        }

        const points = () => {
            pointByDeviceIdApi(reactiveData.id)
                .then((res) => {
                    reactiveData.listPointData = res.data.data

                    // point
                    const pointIds = Array.from(new Set(reactiveData.listPointData.map((pointValue) => pointValue.id)))
                    if (pointIds.length > 0) {
                        pointByIdsApi(pointIds)
                            .then((res) => {
                                reactiveData.pointTable = res.data.data
                            })
                            .catch(() => {
                                // nothing to do
                            })
                    }
                })
                .catch(() => {
                    // nothing to do
                })
                .finally(() => {
                    reactiveData.pointLoading = false
                })
        }

        const pointValues = () => {
            pointValueByDeviceIdApi(reactiveData.id, true)
                .then((res) => {
                    reactiveData.listPointValueData = res.data.data

                    // unit
                    const pointIds = Array.from(
                        new Set(reactiveData.listPointValueData.map((pointValue) => pointValue.pointId))
                    )
                    if (pointIds.length > 0) {
                        pointUnitApi(pointIds)
                            .then((res) => {
                                reactiveData.unitTable = res.data.data
                            })
                            .catch(() => {
                                // nothing to do
                            })
                    }

                    reactiveData.listPointValueData.forEach((pointValue) => {
                        if (pointValue.type === 'string') {
                            reactiveData.listPointValueHistoryData[pointValue.pointId] = []
                        } else if (pointValue.type === 'boolean') {
                            reactiveData.listPointValueHistoryData[pointValue.pointId] = pointValue.children
                                .reverse()
                                .map((pointValue) => (pointValue.value === 'true' ? 1 : 0))
                        } else {
                            reactiveData.listPointValueHistoryData[pointValue.pointId] = pointValue.children
                                .reverse()
                                .map((pointValue) => +pointValue.value)
                        }
                    })
                })
                .catch(() => {
                    // nothing to do
                })
                .finally(() => {
                    reactiveData.pointValueLoading = false
                })
        }

        const profile = () => {
            profileDictionaryApi()
                .then((res) => {
                    reactiveData.profileDictionary = res.data.data
                })
                .catch(() => {
                    // nothing to do
                })
        }

        const point = () => {
            pointDictionaryApi('point')
                .then((res) => {
                    reactiveData.pointDictionary = res.data.data
                })
                .catch(() => {
                    // nothing to do
                })
        }

        const showPointValueDetail = (pointValue) => {
            reactiveData.pointValueDetailData = pointValue
            pointValueDetailRef.value.detailVisible = true
        }

        const updateThing = (pointValue) => {
            console.log('update things', pointValue)
        }

        const changeActive = (tab) => {
            const query = route.query
            router.push({ query: { ...query, active: tab.props.name } }).catch(() => {
                // nothing to do
            })

            switch (tab.props.name) {
                case 'profile':
                    profile()
                    break
                case 'point':
                    point()
                    break
                case 'pointValue':
                    pointValues()
                    break
                default:
                    break
            }
        }

        device()
        profile()
        point()
        pointValues()
        profiles()
        points()

        return {
            pointValueDetailRef,
            reactiveData,
            hasProfileData,
            hasPointData,
            hasPointValueData,
            showPointValueDetail,
            updateThing,
            changeActive,
            timestamp,
        }
    },
})
