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

import { defineComponent, reactive, ref, computed } from 'vue'
import { Connection, Edit, Management, Monitor, Position, Promotion, Sunset } from '@element-plus/icons-vue'

import { useRoute } from 'vue-router'
import router from '@/config/router'

import { getDriverById } from '@/api/driver'

import blankCard from '@/components/card/blank/BlankCard.vue'
import baseCard from '@/components/card/base/BaseCard.vue'
import detailCard from '@/components/card/detail/DetailCard.vue'
import skeletonCard from '@/components/card/skeleton/SkeletonCard.vue'
import driverTool from '@/views/driver/tool/DriverTool.vue'
import deviceList from '@/views/device/Device.vue'
import driverCard from '@/views/driver/card/DriverCard.vue'
import deviceCard from '@/views/device/card/DeviceCard.vue'
import device from '@/views/device/Device.vue'
import pointCard from '@/views/point/card/PointCard.vue'

import { timestamp } from '@/utils/CommonUtils'

export default defineComponent({
    name: 'DriverDetail',
    components: {
        blankCard,
        baseCard,
        detailCard,
        skeletonCard,
        driverTool,
        deviceList,
        driverCard,
        deviceCard,
        device,
        pointCard,
        Position,
        Promotion,
        Edit,
        Sunset,
        Management,
        Connection,
        Monitor,
    },
    setup() {
        const route = useRoute()

        const deviceViewRef: any = ref<InstanceType<typeof device>>()

        // 定义响应式数据
        const reactiveData = reactive({
            id: route.query.id as string,
            active: route.query.active,
            data: {} as any,
        })

        const deviceLength = computed(() => {
            return deviceViewRef.value?.reactiveData.page.total || 0
        })

        // 加载驱动数据
        const driver = () => {
            getDriverById(reactiveData.id).then((res) => {
                reactiveData.data = res.data.data
            })
        }

        // 切换Tab
        const changeActive = (tab) => {
            reactiveData.active = tab.props.name
            const query = route.query
            switch (reactiveData.active) {
                case 'detail':
                    driver()
                    break
                case 'device':
                    deviceViewRef.value?.refresh()
                    break
                case 'model':
                    // to do something
                    break
                case 'event':
                    // to do something
                    break
                default:
                    break
            }
            router.push({ query: { ...query, active: tab.props.name } })
        }

        driver()

        return {
            deviceViewRef,
            reactiveData,
            deviceLength,
            changeActive,
            timestamp,
        }
    },
})
