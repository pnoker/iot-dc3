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

import { defineComponent, reactive, ref, unref, computed } from 'vue'
import { FormInstance, FormRules } from 'element-plus'
import { Back, Check, Edit, RefreshLeft, Right } from '@element-plus/icons-vue'

import { useRoute } from 'vue-router'
import router from '@/config/router'

import { getDriverDictionary, getProfileDictionary } from '@/api/dictionary'
import { getDeviceById, updateDevice } from '@/api/device'
import { getDriverAttributeByDriverId, getPointAttributeByDriverId } from '@/api/attribute'
import { addDriverInfo, getDriverInfoByDeviceId, updateDriverInfo, addPointInfo, getPointInfoByDeviceId, updatePointInfo } from '@/api/info'

import { Dictionary, Order } from '@/config/entity'

import skeletonCard from '@/components/card/skeleton/SkeletonCard.vue'
import pointInfoCard from '@/views/point/info/PointInfoCard.vue'
import { isNull } from '@/utils/utils'
import { getDriverById } from '@/api/driver'
import { getProfileByIds } from '@/api/profile'
import { getPointByDeviceId } from '@/api/point'

export default defineComponent({
    name: 'DeviceEdit',
    components: {
        skeletonCard,
        pointInfoCard
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
            Check
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
                orders: [] as Order[]
            },
            profileQuery: '',
            profileDictionary: [] as Dictionary[],
            profilePage: {
                total: 0,
                size: 5,
                current: 1,
                orders: [] as Order[]
            }
        })

        // 定义表单校验规则
        const deviceFormRule = reactive<FormRules>({
            deviceName: [
                {
                    required: true,
                    message: '请输入设备名称',
                    trigger: 'blur'
                },
                {
                    min: 2,
                    max: 32,
                    message: '请输入 2~32 位字长的设备名称',
                    trigger: 'blur'
                },
                {
                    pattern: /^[A-Za-z0-9\u4e00-\u9fa5][A-Za-z0-9\u4e00-\u9fa5-_]*$/,
                    message: '请输入正确格式的设备名称'
                }
            ],
            driverId: [
                {
                    required: true,
                    message: '请选择所属驱动',
                    trigger: 'change'
                }
            ],
            profileIds: [
                {
                    required: true,
                    message: '请选择关联模板',
                    trigger: 'change'
                }
            ],
            enableFlag: [
                {
                    required: true,
                    message: '请选择使能',
                    trigger: 'change'
                }
            ],
            remark: [
                {
                    max: 300,
                    message: '最多输入300个字符',
                    trigger: 'blur'
                }
            ]
        })

        const hasPointFormData = computed(() => {
            return !isNull(reactiveData.pointFormData)
        })

        const driverDictionary = () => {
            getDriverDictionary({
                page: reactiveData.driverPage,
                label: reactiveData.driverQuery
            })
                .then((res) => {
                    const data = res.data
                    reactiveData.driverPage.total = data.total
                    reactiveData.driverDictionary = data.records
                })
                .catch(() => {
                    // nothing to do
                })
        }

        const driverCurrentChange = (current: number) => {
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
            getProfileDictionary({
                page: reactiveData.profilePage,
                label: reactiveData.profileQuery
            })
                .then((res) => {
                    const data = res.data
                    reactiveData.profilePage.total = data.total
                    reactiveData.profileDictionary = data.records
                })
                .catch(() => {
                    // nothing to do
                })
        }

        const profileCurrentChange = (current: number) => {
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
            getDeviceById(reactiveData.id)
                .then((res) => {
                    reactiveData.deviceFormData = res.data
                    reactiveData.oldDeviceFormData = { ...res.data }

                    getDriverById(reactiveData.deviceFormData.driverId).then((res) => {
                        const driver = res.data
                        reactiveData.driverDictionary.push({
                            label: driver.driverName,
                            value: driver.id
                        } as Dictionary)
                    })

                    getProfileByIds(reactiveData.deviceFormData.profileIds).then((res) => {
                        const profiles = res.data
                        for (const key in profiles) {
                            const profile = profiles[key]
                            reactiveData.profileDictionary.push({
                                label: profile.profileName,
                                value: profile.id
                            } as Dictionary)
                        }
                    })

                    changeAttribute(reactiveData.deviceFormData.driverId)
                })
                .catch(() => {
                    // nothing to do
                })
        }

        const changeAttribute = (driverId: string) => {
            if (isNull(driverId)) {
                return
            }

            getDriverAttributeByDriverId(driverId)
                .then((res) => {
                    reactiveData.driverAttributes = res.data
                    reactiveData.driverAttributeTable = reactiveData.driverAttributes.reduce((pre, cur) => {
                        pre[cur.id] = cur.attributeName
                        return pre
                    }, {})

                    const driverFormData = {}
                    reactiveData.driverAttributes.forEach((attribute) => {
                        driverFormData[attribute.attributeName] = {
                            id: null,
                            configValue: ''
                        }
                    })
                    reactiveData.driverFormData = JSON.parse(JSON.stringify(driverFormData))
                    reactiveData.oldDriverFormData = JSON.parse(JSON.stringify(driverFormData))

                    driverInfo()
                })
                .catch(() => {
                    // nothing to do
                })

            getPointAttributeByDriverId(driverId)
                .then((res) => {
                    reactiveData.pointAttributes = res.data
                    reactiveData.pointAttributeTable = reactiveData.pointAttributes.reduce((pre, cur) => {
                        pre[cur.id] = cur.attributeName
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
            getDriverInfoByDeviceId(reactiveData.id)
                .then((res) => {
                    const formData = reactiveData.driverFormData
                    res.data.forEach((info: { driverAttributeId: string | number; id: any; configValue: any }) => {
                        formData[reactiveData.driverAttributeTable[info.driverAttributeId]] = {
                            id: info.id,
                            configValue: info.configValue
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
            getPointByDeviceId(reactiveData.id)
                .then((res) => {
                    reactiveData.pointInfoData = res.data.map((point: { id: any; pointName: any }) => {
                        const pointInfo = {
                            id: point.id,
                            pointName: point.pointName,
                            shadow: 'hover'
                        }

                        reactiveData.pointAttributes.forEach((attribute) => {
                            pointInfo[attribute.attributeName] = {
                                id: null,
                                configValue: ''
                            }
                        })
                        return pointInfo
                    })

                    getPointInfoByDeviceId(reactiveData.id)
                        .then((res) => {
                            res.data.forEach((info: { pointId: any; pointAttributeId: string | number; id: any; configValue: any }) => {
                                reactiveData.pointInfoData.forEach((pointInfo) => {
                                    if (pointInfo.id === info.pointId) {
                                        pointInfo[reactiveData.pointAttributeTable[info.pointAttributeId]] = {
                                            id: info.id,
                                            configValue: info.configValue
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
                    updateDevice(reactiveData.deviceFormData)
                        .then((res) => {
                            reactiveData.oldDeviceFormData = { ...res.data }
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
                            id: reactiveData.driverFormData[attribute.attributeName].id,
                            driverAttributeId: attribute.id,
                            deviceId: reactiveData.id,
                            configValue: reactiveData.driverFormData[attribute.attributeName].configValue
                        }

                        driverInfo.id
                            ? updateDriverInfo(driverInfo)
                                  .then(() => loadFormData(driverInfo))
                                  .catch(() => {
                                      // nothing to do
                                  })
                            : addDriverInfo(driverInfo)
                                  .then(() => loadFormData(driverInfo))
                                  .catch(() => {
                                      // nothing to do
                                  })

                        function loadFormData(res: { id: any; driverAttributeId?: any; deviceId?: string; configValue: any }) {
                            driverFormData[attribute.attributeName] = {
                                id: res.id,
                                configValue: res.configValue
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
                            id: reactiveData.pointFormData[attribute.attributeName].id,
                            pointAttributeId: attribute.id,
                            deviceId: reactiveData.id,
                            pointId: reactiveData.pointFormData.id,
                            configValue: reactiveData.pointFormData[attribute.attributeName].configValue
                        }

                        pointInfo.id
                            ? updatePointInfo(pointInfo)
                                  .then(() => loadFormData(pointInfo))
                                  .catch(() => {
                                      // nothing to do
                                  })
                            : addPointInfo(pointInfo)
                                  .then(() => loadFormData(pointInfo))
                                  .catch(() => {
                                      // nothing to do
                                  })

                        function loadFormData(res: { id: any; pointAttributeId?: any; deviceId?: string; pointId?: any; configValue: any }) {
                            reactiveData.pointInfoData.forEach((pointInfo) => {
                                if (pointInfo.id === reactiveData.pointFormData.id) {
                                    pointInfo[attribute.attributeName] = {
                                        id: res.id,
                                        configValue: res.configValue
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

        const selectPoint = (row: { [x: string]: { id: null; configValue: string }; id: any }) => {
            reactiveData.pointAttributes.forEach((attribute) => {
                if (!row[attribute.attributeName]) {
                    row[attribute.attributeName] = {
                        id: null,
                        configValue: ''
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
            router.push({ name: 'device' }).catch(() => {
                // nothing to do
            })
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

        const changeActive = (step: number) => {
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
            driverUpdate,
            pointUpdate,
            selectPoint,
            pre,
            next,
            done,
            deviceReset,
            driverInfoReset,
            pointInfoReset,
            changeActive,
            ...Icon
        }
    }
})
