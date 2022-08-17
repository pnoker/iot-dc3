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

import { defineComponent, reactive, ref, unref, computed } from 'vue'
import { FormInstance, FormRules } from 'element-plus'
import { Back, Check, Edit, RefreshLeft, Right } from '@element-plus/icons-vue'

import { useRoute } from 'vue-router'
import router from '@/config/router'

import { driverDictionaryApi, profileDictionaryApi } from '@/api/dictionary'
import { deviceByIdApi, deviceUpdateApi } from '@/api/device'
import { driverAttributeByDriverIdApi, pointAttributeByDriverIdApi } from '@/api/attribute'
import {
    driverInfoAddApi,
    driverInfoByDeviceIdApi,
    driverInfoUpdateApi,
    pointInfoAddApi,
    pointInfoByDeviceIdApi,
    pointInfoUpdateApi,
} from '@/api/info'

import { Dictionary, Order } from '@/config/type/types'

import skeletonCard from '@/components/card/skeleton/SkeletonCard.vue'
import pointInfoCard from '@/views/point/info/PointInfoCard.vue'
import { isNull } from '@/util/utils'
import { driverByIdApi } from '@/api/driver'
import { profileByIdsApi } from '@/api/profile'
import { pointByDeviceIdApi } from '@/api/point'

export default defineComponent({
    name: 'DeviceEdit',
    components: {
        skeletonCard,
        pointInfoCard,
    },
    setup() {
        const route = useRoute()

        // 定义表单引用
        const deviceFormRef = ref<FormInstance>()
        const driverFormRef = ref<FormInstance>()
        const pointFormRef = ref<FormInstance>()

        // 图标
        const Icon = {
            Edit,
            RefreshLeft,
            Right,
            Back,
            Check,
        }

        // 定义响应式数据
        const reactiveData = reactive({
            id: route.query.id as string,
            active: +(route.query.active || 0),
            loading: true,
            oldDeviceFormData: {},
            deviceFormData: {} as any,
            driverAttributes: [] as any[],
            driverAttributeTable: {},
            oldDriverFormData: {},
            driverFormData: {} as any,
            pointAttributes: [] as any[],
            pointAttributeTable: {},
            oldPointFormData: {},
            pointFormData: {} as any,
            pointInfoData: [] as any[],
            driverQuery: '',
            driverDictionary: [] as Dictionary[],
            driverPage: {
                total: 0,
                size: 5,
                current: 1,
                orders: [] as Order[],
            },
            profileQuery: '',
            profileDictionary: [] as Dictionary[],
            profilePage: {
                total: 0,
                size: 5,
                current: 1,
                orders: [] as Order[],
            },
        })

        // 定义表单校验规则
        const deviceFormRule = reactive<FormRules>({
            name: [
                {
                    required: true,
                    message: '请输入设备名称',
                    trigger: 'blur',
                },
                {
                    min: 2,
                    max: 32,
                    message: '请输入 2~32 位字长的设备名称',
                    trigger: 'blur',
                },
                {
                    pattern: /^[A-Za-z0-9\u4e00-\u9fa5][A-Za-z0-9\u4e00-\u9fa5-_]*$/,
                    message: '请输入正确格式的设备名称',
                },
            ],
            driverId: [
                {
                    required: true,
                    message: '请选择所属驱动',
                    trigger: 'change',
                },
            ],
            profileIds: [
                {
                    required: true,
                    message: '请选择关联模板',
                    trigger: 'change',
                },
            ],
            multi: [
                {
                    required: true,
                    message: '请选择存储类型',
                    trigger: 'change',
                },
            ],
            enable: [
                {
                    required: true,
                    message: '请选择使能',
                    trigger: 'change',
                },
            ],
            description: [
                {
                    max: 300,
                    message: '最多输入300个字符',
                    trigger: 'blur',
                },
            ],
        })

        const hasPointFormData = computed(() => {
            return !isNull(reactiveData.pointFormData)
        })

        const driverDictionary = () => {
            driverDictionaryApi({
                page: reactiveData.driverPage,
                label: reactiveData.driverQuery,
            })
                .then((res) => {
                    const data = res.data.data
                    reactiveData.driverPage.total = data.total
                    reactiveData.driverDictionary = data.records
                })
                .catch(() => {
                    // nothing to do
                })
        }

        const driverCurrentChange = (current) => {
            reactiveData.driverPage.current = current
            driverDictionary()
        }

        const driverDictionaryVisible = (visible: boolean) => {
            if (visible) {
                reactiveData.driverQuery = ''
                driverDictionary()
            }
        }

        const profileDictionary = () => {
            profileDictionaryApi({
                page: reactiveData.profilePage,
                label: reactiveData.profileQuery,
            })
                .then((res) => {
                    const data = res.data.data
                    reactiveData.profilePage.total = data.total
                    reactiveData.profileDictionary = data.records
                })
                .catch(() => {
                    // nothing to do
                })
        }

        const profileCurrentChange = (current) => {
            reactiveData.driverPage.current = current
            profileDictionary()
        }

        const profileDictionaryVisible = (visible: boolean) => {
            if (visible) {
                reactiveData.profileQuery = ''
                profileDictionary()
            }
        }

        const device = () => {
            deviceByIdApi(reactiveData.id)
                .then((res) => {
                    reactiveData.deviceFormData = res.data.data
                    reactiveData.oldDeviceFormData = { ...res.data.data }

                    driverByIdApi(reactiveData.deviceFormData.driverId).then((res) => {
                        const driver = res.data.data
                        reactiveData.driverDictionary.push({
                            label: driver.name,
                            value: driver.id,
                        } as Dictionary)
                    })

                    profileByIdsApi(reactiveData.deviceFormData.profileIds).then((res) => {
                        const profiles = res.data.data
                        for (const key in profiles) {
                            const profile = profiles[key]
                            reactiveData.profileDictionary.push({
                                label: profile.name,
                                value: profile.id,
                            } as Dictionary)
                        }
                    })

                    changeAttribute(reactiveData.deviceFormData.driverId)
                })
                .catch(() => {
                    // nothing to do
                })
        }

        const changeAttribute = (value?) => {
            if (isNull(value)) {
                return
            }

            driverAttributeByDriverIdApi(value)
                .then((res) => {
                    reactiveData.driverAttributes = res.data.data
                    reactiveData.driverAttributeTable = reactiveData.driverAttributes.reduce((pre, cur) => {
                        pre[cur.id] = cur.name
                        return pre
                    }, {})

                    const driverFormData = {}
                    reactiveData.driverAttributes.forEach((attribute) => {
                        driverFormData[attribute.name] = {
                            id: null,
                            value: '',
                        }
                    })
                    reactiveData.driverFormData = JSON.parse(JSON.stringify(driverFormData))
                    reactiveData.oldDriverFormData = JSON.parse(JSON.stringify(driverFormData))

                    driverInfo()
                })
                .catch(() => {
                    // nothing to do
                })

            pointAttributeByDriverIdApi(value)
                .then((res) => {
                    reactiveData.pointAttributes = res.data.data
                    reactiveData.pointAttributeTable = reactiveData.pointAttributes.reduce((pre, cur) => {
                        pre[cur.id] = cur.name
                        return pre
                    }, {})

                    pointInfo()
                })
                .catch(() => {
                    // nothing to do
                })
                .finally(() => {
                    reactiveData.loading = false
                })
        }

        const driverInfo = () => {
            driverInfoByDeviceIdApi(reactiveData.id)
                .then((res) => {
                    const formData = {}
                    res.data.data.forEach((info) => {
                        formData[reactiveData.driverAttributeTable[info.driverAttributeId]] = {
                            id: info.id,
                            value: info.value,
                        }
                    })

                    reactiveData.driverFormData = JSON.parse(JSON.stringify(formData))
                    reactiveData.oldDriverFormData = JSON.parse(JSON.stringify(formData))
                })
                .catch(() => {
                    // nothing to do
                })
        }

        const pointInfo = () => {
            pointByDeviceIdApi(reactiveData.id)
                .then((res) => {
                    reactiveData.pointInfoData = res.data.data.map((point) => {
                        const pointInfo = {
                            id: point.id,
                            name: point.name,
                            shadow: 'hover',
                        }

                        reactiveData.pointAttributes.forEach((attribute) => {
                            pointInfo[attribute.name] = {
                                id: null,
                                value: '',
                            }
                        })
                        return pointInfo
                    })

                    pointInfoByDeviceIdApi(reactiveData.id)
                        .then((res) => {
                            res.data.data.forEach((info) => {
                                reactiveData.pointInfoData.forEach((pointInfo) => {
                                    if (pointInfo.id === info.pointId) {
                                        pointInfo[reactiveData.pointAttributeTable[info.pointAttributeId]] = {
                                            id: info.id,
                                            value: info.value,
                                        }
                                    }
                                    return pointInfo
                                })
                            })
                        })
                        .catch(() => {
                            // nothing to do
                        })
                })
                .catch(() => {
                    // nothing to do
                })
        }

        const deviceUpdate = () => {
            const form = unref(deviceFormRef)
            form?.validate((valid) => {
                if (valid) {
                    deviceUpdateApi(reactiveData.deviceFormData)
                        .then((res) => {
                            reactiveData.oldDeviceFormData = { ...res.data.data }
                        })
                        .catch(() => {
                            // nothing to do
                        })
                }
            })
        }

        const driverUpdate = () => {
            const form = unref(driverFormRef)
            form?.validate((valid) => {
                if (valid) {
                    const driverFormData = {}
                    reactiveData.driverAttributes.forEach((attribute) => {
                        const driverInfo = {
                            id: reactiveData.driverFormData[attribute.name].id,
                            driverAttributeId: attribute.id,
                            deviceId: reactiveData.id,
                            value: reactiveData.driverFormData[attribute.name].value,
                        }

                        driverInfo.id
                            ? driverInfoUpdateApi(driverInfo)
                                  .then((res) => loadFormData(res))
                                  .catch(() => {
                                      // nothing to do
                                  })
                            : driverInfoAddApi(driverInfo)
                                  .then((res) => loadFormData(res))
                                  .catch(() => {
                                      // nothing to do
                                  })

                        function loadFormData(res) {
                            driverFormData[attribute.name] = {
                                id: res.data.data.id,
                                value: res.data.data.value,
                            }
                            reactiveData.oldDriverFormData = JSON.parse(JSON.stringify(driverFormData))
                        }
                    })
                }
            })
        }

        const pointUpdate = () => {
            const form = unref(pointFormRef)
            form?.validate((valid) => {
                if (valid) {
                    reactiveData.pointAttributes.forEach((attribute) => {
                        const pointInfo = {
                            id: reactiveData.pointFormData[attribute.name].id,
                            pointAttributeId: attribute.id,
                            deviceId: reactiveData.id,
                            pointId: reactiveData.pointFormData.id,
                            value: reactiveData.pointFormData[attribute.name].value,
                        }

                        pointInfo.id
                            ? pointInfoUpdateApi(pointInfo)
                                  .then((res) => loadFormData(res))
                                  .catch(() => {
                                      // nothing to do
                                  })
                            : pointInfoAddApi(pointInfo)
                                  .then((res) => loadFormData(res))
                                  .catch(() => {
                                      // nothing to do
                                  })

                        function loadFormData(res) {
                            reactiveData.pointInfoData.forEach((pointInfo) => {
                                if (pointInfo.id === reactiveData.pointFormData.id) {
                                    pointInfo[attribute.name] = {
                                        id: res.data.data.id,
                                        value: res.data.data.value,
                                    }
                                    reactiveData.oldPointFormData = JSON.parse(JSON.stringify(pointInfo))
                                }
                                return pointInfo
                            })
                        }
                    })
                }
            })
        }

        const selectPoint = (row) => {
            reactiveData.pointAttributes.forEach((attribute) => {
                if (!row[attribute.name]) {
                    row[attribute.name] = {
                        id: null,
                        value: '',
                    }
                }
            })
            reactiveData.pointFormData = JSON.parse(JSON.stringify(row))
            reactiveData.oldPointFormData = JSON.parse(JSON.stringify(row))

            reactiveData.pointInfoData.forEach((pointInfo) => {
                pointInfo.shadow = 'hover'
                if (row.id === pointInfo.id) {
                    pointInfo.shadow = 'always'
                }
            })
        }

        const pre = () => {
            let step = 1
            if (reactiveData.active === 2 && reactiveData.driverAttributes?.length < 1) {
                step = 2
            }
            reactiveData.active -= step
            changeActive(reactiveData.active)
        }

        const next = () => {
            if (reactiveData.active === 0) {
                deviceUpdate()
            }
            if (reactiveData.active === 1) {
                driverUpdate()
            }

            let step = 1
            if (reactiveData.active === 0 && reactiveData.driverAttributes?.length < 1) {
                step = 2
            }
            reactiveData.active += step
            changeActive(reactiveData.active)
        }

        const done = () => {
            router.push({ name: 'device' })
        }

        const deviceReset = () => {
            reactiveData.deviceFormData = JSON.parse(JSON.stringify(reactiveData.oldDeviceFormData))
        }

        const driverInfoReset = () => {
            reactiveData.driverFormData = JSON.parse(JSON.stringify(reactiveData.oldDriverFormData))
        }

        const pointInfoReset = () => {
            reactiveData.pointFormData = JSON.parse(JSON.stringify(reactiveData.oldPointFormData))
        }

        const changeActive = (step) => {
            const query = route.query
            router.push({ query: { ...query, active: step } }).catch(() => {
                // nothing to do
            })
        }

        device()

        return {
            deviceFormRef,
            driverFormRef,
            pointFormRef,
            deviceFormRule,
            reactiveData,
            hasPointFormData,
            driverDictionary,
            driverCurrentChange,
            driverDictionaryVisible,
            profileDictionary,
            profileCurrentChange,
            profileDictionaryVisible,
            changeAttribute,
            pointUpdate,
            selectPoint,
            pre,
            next,
            done,
            deviceReset,
            driverInfoReset,
            pointInfoReset,
            changeActive,
            ...Icon,
        }
    },
})
