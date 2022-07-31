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

import { defineComponent, reactive, ref, computed } from 'vue'

import { deviceAddApi, deviceDeleteApi, deviceListApi, deviceStatusApi } from '@/api/device'
import { profileByIdsApi } from '@/api/profile'
import { driverByIdsApi } from '@/api/driver'
import { driverDictionaryApi, profileDictionaryApi } from '@/api/dictionary'

import { Dictionary, Order } from '@/config/type/types'

import blankCard from '@/components/card/blank/BlankCard.vue'
import skeletonCard from '@/components/card/skeleton/SkeletonCard.vue'
import deviceTool from './tool/DeviceTool.vue'
import deviceAddForm from './add/DeviceAddForm.vue'
import deviceCard from './card/DeviceCard.vue'
import { isNull } from '@/util/utils'

export default defineComponent({
    name: 'Device',
    components: {
        blankCard,
        skeletonCard,
        deviceTool,
        deviceAddForm,
        deviceCard,
    },
    props: {
        embedded: {
            type: Boolean,
            default: () => {
                return false
            },
        },
        driverId: {
            type: String,
            default: () => {
                return ''
            },
        },
    },
    setup(props) {
        const deviceAddFormRef: any = ref<InstanceType<typeof deviceAddForm>>()

        // 定义响应式数据
        const reactiveData = reactive({
            loading: true,
            driverDictionary: [] as Dictionary[],
            profileDictionary: [] as Dictionary[],
            driverTable: {},
            profileTable: {},
            statusTable: {},
            listData: [] as any[],
            query: {},
            order: false,
            page: {
                total: 0,
                size: 12,
                current: 1,
                orders: [] as Order[],
            },
        })

        const hasData = computed(() => {
            return !reactiveData.loading && reactiveData.listData?.length < 1
        })

        const list = () => {
            if (!isNull(props.driverId)) {
                reactiveData.query = {
                    ...reactiveData.query,
                    driverId: props.driverId,
                }
            }

            deviceListApi({
                page: reactiveData.page,
                ...reactiveData.query,
            })
                .then((res) => {
                    const data = res.data.data
                    reactiveData.page.total = data.total
                    reactiveData.listData = data.records

                    // driver
                    const driverIds = Array.from(new Set(reactiveData.listData.map((device) => device.driverId)))
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
                            reactiveData.listData.reduce((pre, cur) => {
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
                .finally(() => {
                    reactiveData.loading = false
                })

            deviceStatusApi({
                page: reactiveData.page,
                ...reactiveData.query,
            })
                .then((res) => {
                    reactiveData.statusTable = res.data.data
                })
                .catch(() => {
                    // nothing to do
                })
        }

        const driver = () => {
            driverDictionaryApi()
                .then((res) => {
                    reactiveData.driverDictionary = res.data.data
                })
                .catch(() => {
                    // nothing to do
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

        const search = (params) => {
            if (!isNull(props.driverId)) {
                params = {
                    ...params,
                    driverId: props.driverId,
                }
            }

            reactiveData.query = params
            list()
        }

        const reset = () => {
            let params = {}
            if (!isNull(props.driverId)) {
                params = { driverId: props.driverId }
            }

            reactiveData.query = params
            list()
        }

        const showAdd = () => {
            deviceAddFormRef.value.show()
        }

        const addThing = (form, done) => {
            deviceAddApi(form)
                .then(() => {
                    list()
                    done()
                })
                .catch(() => {
                    // nothing to do
                })
        }

        const deleteThing = (id, done) => {
            deviceDeleteApi(id)
                .then(() => {
                    list()
                    done()
                })
                .catch(() => {
                    // nothing to do
                })
        }

        const refresh = () => {
            list()
        }

        const sort = () => {
            reactiveData.order = !reactiveData.order
            if (reactiveData.order) {
                reactiveData.page.orders = [{ column: 'create_time', asc: true }]
            } else {
                reactiveData.page.orders = [{ column: 'create_time', asc: false }]
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

        driver()
        profile()
        list()

        return {
            deviceAddFormRef,
            reactiveData,
            hasData,
            search,
            reset,
            showAdd,
            addThing,
            deleteThing,
            refresh,
            sort,
            sizeChange,
            currentChange,
        }
    },
})
