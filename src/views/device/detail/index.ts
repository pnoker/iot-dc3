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

import { computed, defineComponent, reactive, ref } from 'vue'
import { CollectionTag, Edit, List, Management, Promotion, Sunset } from '@element-plus/icons-vue'

import { useRoute } from 'vue-router'
import router from '@/config/router'

import { getDriverById } from '@/api/driver'
import { getProfileByDeviceId } from '@/api/profile'
import { getDeviceById } from '@/api/device'
import { getProfileByIds } from '@/api/profile'

import baseCard from '@/components/card/base/BaseCard.vue'
import detailCard from '@/components/card/detail/DetailCard.vue'
import skeletonCard from '@/components/card/skeleton/SkeletonCard.vue'
import deviceCard from '@/views/device/card/DeviceCard.vue'
import profileCard from '@/views/profile/card/ProfileCard.vue'
import profile from '@/views/profile/Profile.vue'
import pointCard from '@/views/point/card/PointCard.vue'
import point from '@/views/point/Point.vue'
import pointValueCard from '@/views/point/value/card/PointValueCard.vue'
import pointValue from '@/views/point/value/PointValue.vue'
import pointValueEditForm from '@/views/point/value/edit/PointValueEditForm.vue'

import { timestamp } from '@/utils/CommonUtils'

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
        pointValue,
        pointValueEditForm,
        point,
        profile,
        Promotion,
        List,
        Management,
        Edit,
        Sunset,
        CollectionTag,
    },
    props: {
        deviceName: {
            type: String,
            default: '',
        },
    },
    setup() {
        const route = useRoute()

        const profileViewRef: any = ref<InstanceType<typeof profile>>()
        const pointViewRef: any = ref<InstanceType<typeof point>>()
        const pointValueViewRef: any = ref<InstanceType<typeof pointValue>>()

        // 定义响应式数据
        const reactiveData = reactive({
            id: route.query.id as string,
            active: route.query.active,
            profileLoading: true,
            pointLoading: true,
            pointValueLoading: true,
            data: {} as any,
            driver: {} as any,
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

        const profileLength = computed(() => {
            return profileViewRef.value?.reactiveData.page.total || 0
        })

        const pointLength = computed(() => {
            return pointViewRef.value?.reactiveData.page.total || 0
        })

        const hasPointValueData = computed(() => {
            return !reactiveData.pointValueLoading && reactiveData.listPointValueData?.length < 1
        })

        const device = () => {
            getDeviceById(reactiveData.id)
                .then((res) => {
                    reactiveData.data = res.data
                    reactiveData.deviceTable[reactiveData.data.id] = reactiveData.data.deviceName

                    getDriverById(reactiveData.data.driverId)
                        .then((res) => {
                            reactiveData.driver = res.data
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
            getProfileByDeviceId(reactiveData.id)
                .then((res) => {
                    reactiveData.listProfileData = res.data

                    // profile
                    const profileIds = Array.from(new Set(reactiveData.listProfileData.map((pointValue) => pointValue.id)))
                    getProfileByIds(profileIds)
                        .then((res) => {
                            reactiveData.profileTable = res.data
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
                    break
                case 'point':
                    pointViewRef.value?.refresh()
                    break
                case 'pointValue':
                    pointValueViewRef.value?.refresh()
                    break
                default:
                    break
            }
        }

        device()
        profiles()

        return {
            profileViewRef,
            pointViewRef,
            pointValueViewRef,
            reactiveData,
            profileLength,
            pointLength,
            hasPointValueData,
            updateThing,
            changeActive,
            timestamp,
        }
    },
})
