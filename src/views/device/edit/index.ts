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
import { pointByDeviceIdApi } from '@/api/point'

import { Dictionary } from '@/config/type/types'

import skeletonCard from '@/components/card/skeleton/SkeletonCard.vue'
import pointInfoCard from '@/views/point/info/PointInfoCard.vue'
import { isNull } from '@/util/utils'

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
            driverId: route.query.driverId as string,
            active: +(route.query.active || 0),
            loading: true,
            driverDictionary: [] as Dictionary[],
            profileDictionary: [] as Dictionary[],
            driverTable: {},
            profileTable: {},
            pointTable: {},
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

        const device = () => {
            const id = route.query.id as string
            deviceByIdApi(id)
                .then((res) => {
                    reactiveData.deviceFormData = res.data.data
                    changeDriver(reactiveData.driverId)
                    changePoint()
                    reactiveData.oldDeviceFormData = { ...res.data.data }
                })
                .catch(() => {
                    // nothing to do
                })
        }

        const driver = () => {
            driverDictionaryApi('')
                .then((res) => {
                    reactiveData.driverDictionary = res.data.data
                    reactiveData.driverTable = reactiveData.driverDictionary.reduce((pre, cur) => {
                        pre[cur.value] = cur.label
                        return pre
                    }, {})
                })
                .catch(() => {
                    // nothing to do
                })
        }

        const profile = () => {
            profileDictionaryApi('')
                .then((res) => {
                    reactiveData.profileDictionary = res.data.data
                    reactiveData.profileTable = reactiveData.profileDictionary.reduce((pre, cur) => {
                        pre[cur.value] = cur.label
                        return pre
                    }, {})
                })
                .catch(() => {
                    // nothing to do
                })
        }

        const point = () => {
            pointByDeviceIdApi(reactiveData.id)
                .then((res1) => {
                    reactiveData.pointTable = res1.data.data.reduce((pre, cur) => {
                        pre[cur.id] = cur.name
                        return pre
                    }, {})

                    pointAttributeByDriverIdApi(reactiveData.driverId)
                        .then((res2) => {
                            reactiveData.pointAttributes = res2.data.data
                            reactiveData.pointAttributeTable = reactiveData.pointAttributes.reduce((pre, cur) => {
                                pre[cur.id] = cur.name
                                return pre
                            }, {})

                            reactiveData.pointInfoData = res1.data.data.map((point) => {
                                const pointInfo = {
                                    id: point.id,
                                    name: point.name,
                                    shadow: 'hover',
                                }

                                res2.data.data.forEach((attribute) => {
                                    pointInfo[attribute.name] = {
                                        id: null,
                                        value: '',
                                    }
                                })
                                return pointInfo
                            })
                        })
                        .catch(() => {
                            // nothing to do
                        })
                })
                .catch(() => {
                    // nothing to do
                })
                .finally(() => {
                    reactiveData.loading = false
                })
        }

        const changeDriver = (driverId) => {
            driverAttributeByDriverIdApi(driverId)
                .then((res) => {
                    reactiveData.driverAttributes = res.data.data
                    reactiveData.driverAttributeTable = res.data.data.reduce((pre, cur) => {
                        pre[cur.id] = cur.name
                        return pre
                    }, {})

                    const formData = {}
                    res.data.data.forEach((attribute) => {
                        formData[attribute.name] = {
                            id: null,
                            value: '',
                        }
                    })
                    reactiveData.driverFormData = JSON.parse(JSON.stringify(formData))
                    reactiveData.oldDriverFormData = JSON.parse(JSON.stringify(formData))

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
                })
                .catch(() => {
                    // nothing to do
                })
        }

        const changePoint = () => {
            pointAttributeByDriverIdApi(reactiveData.driverId)
                .then((res) => {
                    reactiveData.pointAttributes = res.data.data
                    reactiveData.pointAttributeTable = reactiveData.pointAttributes.reduce((pre, cur) => {
                        pre[cur.id] = cur.name
                        return pre
                    }, {})

                    pointInfoByDeviceIdApi(reactiveData.id)
                        .then((res) => {
                            res.data.data.forEach((info) => {
                                reactiveData.pointInfoData = reactiveData.pointInfoData.map((pointInfo) => {
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

        const changePointInfo = (row) => {
            reactiveData.pointAttributes.forEach((attribute) => {
                if (!row[attribute.name]) {
                    row[attribute.name] = {
                        id: null,
                        value: '',
                    }
                }
            })
            reactiveData.pointFormData = JSON.parse(JSON.stringify(row))

            reactiveData.pointInfoData.forEach((pointInfo) => {
                pointInfo.shadow = 'hover'
                if (row.id === pointInfo.id) {
                    pointInfo.shadow = 'always'
                }
            })
        }

        const deviceUpdate = () => {
            const form = unref(deviceFormRef)
            form?.validate((valid) => {
                if (valid) {
                    deviceUpdateApi(reactiveData.deviceFormData)
                        .then((res) => {
                            changeDriver(reactiveData.driverId)
                            changePoint()
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
                    const formData = {}
                    reactiveData.driverAttributes.forEach((attribute) => {
                        const driverInfo = {
                            id: reactiveData.driverFormData[attribute.name].id,
                            driverAttributeId: attribute.id,
                            deviceId: reactiveData.id,
                            value: reactiveData.driverFormData[attribute.name].value,
                        }

                        driverInfo.id
                            ? driverInfoUpdateApi(driverInfo)
                                  .then((res) => loadFormData(formData, res, this))
                                  .catch(() => {
                                      // nothing to do
                                  })
                            : driverInfoAddApi(driverInfo)
                                  .then((res) => loadFormData(formData, res, this))
                                  .catch(() => {
                                      // nothing to do
                                  })

                        function loadFormData(formData, res, that) {
                            formData[attribute.name] = {
                                id: res.data.data.id,
                                value: res.data.data.value,
                            }
                            that.oldDriverFormData = JSON.parse(JSON.stringify(formData))
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
                                  .then((res) => loadTableData(res, attribute, this))
                                  .catch(() => {
                                      // nothing to do
                                  })
                            : pointInfoAddApi(pointInfo)
                                  .then((res) => loadTableData(res, attribute, this))
                                  .catch(() => {
                                      // nothing to do
                                  })

                        function loadTableData(res, attribute, that) {
                            that.pointInfoData = that.pointInfoData.map((tableData) => {
                                if (tableData.id === that.pointFormData.id) {
                                    tableData[attribute.name] = {
                                        id: res.data.data.id,
                                        value: res.data.data.value,
                                    }
                                }
                                return tableData
                            })
                        }
                    })
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
            if (reactiveData.active > 2) {
                router.push({ name: 'device' }).catch(() => {
                    // nothing to do
                })
            } else {
                changeActive(reactiveData.active)
            }
        }

        const deviceReset = () => {
            reactiveData.deviceFormData = { ...reactiveData.oldDeviceFormData }
        }

        const driverInfoReset = () => {
            reactiveData.driverFormData = JSON.parse(JSON.stringify(reactiveData.oldDriverFormData))
        }

        const pointInfoReset = () => {
            const form = unref(pointFormRef)
            form?.resetFields()
        }

        const changeActive = (step) => {
            const query = route.query
            router.push({ query: { ...query, active: step } }).catch(() => {
                // nothing to do
            })
        }

        driver()
        profile()
        device()
        point()

        return {
            deviceFormRef,
            driverFormRef,
            pointFormRef,
            deviceFormRule,
            reactiveData,
            hasPointFormData,
            changePointInfo,
            pointUpdate,
            pre,
            next,
            deviceReset,
            driverInfoReset,
            pointInfoReset,
            changeActive,
            ...Icon,
        }
    },
})
