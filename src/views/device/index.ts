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

import { addDevice, deleteDevice, getDeviceList, getDeviceStatus, importDevice, importDeviceTemplate, updateDevice } from '@/api/device'
import { getDriverByIds } from '@/api/driver'

import { Order } from '@/config/entity'

import blankCard from '@/components/card/blank/BlankCard.vue'
import skeletonCard from '@/components/card/skeleton/SkeletonCard.vue'
import { failMessage } from '@/utils/NotificationUtil'
import { isNull } from '@/utils/utils'
import deviceAddForm from './add/DeviceAddForm.vue'
import deviceCard from './card/DeviceCard.vue'
import deviceImportForm from './import/DeviceImportForm.vue'
import deviceTool from './tool/DeviceTool.vue'

export default defineComponent({
    name: 'Device',
    components: {
        blankCard,
        skeletonCard,
        deviceTool,
        deviceAddForm,
        deviceImportForm,
        deviceCard
    },
    props: {
        embedded: {
            type: String,
            default: () => {
                return ''
            }
        },
        driverId: {
            type: String,
            default: () => {
                return ''
            }
        },
        profileId: {
            type: String,
            default: () => {
                return ''
            }
        }
    },
    setup(props) {
        const deviceAddFormRef: any = ref<InstanceType<typeof deviceAddForm>>()
        const deviceImportFormRef: any = ref<InstanceType<typeof deviceImportForm>>()

        // 定义响应式数据
        const reactiveData = reactive({
            loading: true,
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
                orders: [] as Order[]
            }
        })

        const hasData = computed(() => {
            return !reactiveData.loading && reactiveData.listData?.length < 1
        })

        const list = () => {
            if (!isNull(props.driverId)) {
                reactiveData.query = {
                    ...reactiveData.query,
                    driverId: props.driverId
                }
            }
            if (!isNull(props.profileId)) {
                reactiveData.query = {
                    ...reactiveData.query,
                    profileId: props.profileId
                }
            }

            getDeviceList({
                page: reactiveData.page,
                ...reactiveData.query
            })
                .then(res => {
                    const data = res.data
                    reactiveData.page.total = data.total
                    reactiveData.listData = data.records

                    // driver
                    const driverIds = Array.from(new Set(reactiveData.listData.map(device => device.driverId)))
                    getDriverByIds(driverIds)
                        .then(res => {
                            reactiveData.driverTable = res.data
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

            getDeviceStatus({
                page: reactiveData.page,
                ...reactiveData.query
            })
                .then(res => {
                    reactiveData.statusTable = res.data
                })
                .catch(() => {
                    // nothing to do
                })
        }

        const search = params => {
            if (!isNull(props.driverId)) {
                params = {
                    ...params,
                    driverId: props.driverId
                }
            }
            if (!isNull(props.profileId)) {
                params = {
                    ...params,
                    profileId: props.profileId
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
            if (!isNull(props.profileId)) {
                params = { profileId: props.profileId }
            }

            reactiveData.query = params
            list()
        }

        const showAdd = () => {
            deviceAddFormRef.value?.show()
        }

        const addThing = (form, done) => {
            addDevice(form)
                .then(() => {
                    list()
                    done()
                })
                .catch(() => {
                    // nothing to do
                })
        }

        const showImport = () => {
            deviceImportFormRef.value?.show()
        }

        const importTemplate = (form, done) => {
            importDeviceTemplate(form)
                .then(res => {
                    const url = window.URL.createObjectURL(new Blob([res.data.data as BlobPart]))
                    const name = res.headers['content-disposition'].split(';')[1].split('filename=')[1]
                    const link = document.createElement('a')
                    link.href = url
                    link.setAttribute('download', name)
                    document.body.appendChild(link)
                    link.click()

                    done()
                })
                .catch(() => {
                    // nothing to do
                })
        }

        const importThing = (form, done) => {
            importDevice(form)
                .then(() => {
                    list()
                    done()
                })
                .catch(() => {
                    // nothing to do
                })
        }

        const disableThing = (id, driverId, done) => {
            console.log(props)
            updateDevice({ id: id, driverId: driverId, enableFlag: 'DISABLE' })
                .then(() => {
                    list()
                    done()
                })
                .catch(() => {
                    // nothing to do
                })
        }

        const enableThing = (id, driverId, done) => {
            updateDevice({ id: id, driverId: driverId, enableFlag: 'ENABLE' })
                .then(() => {
                    list()
                    done()
                })
                .catch(() => {
                    // nothing to do
                })
        }

        const deleteThing = (id, done) => {
            deleteDevice(id)
                .then(res => {
                    if (res.data.ok) {
                        list()
                        done()
                    } else {
                        failMessage(res.data.message)
                    }
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

        const sizeChange = (size: number) => {
            reactiveData.page.size = size
            list()
        }

        const currentChange = (current: number) => {
            reactiveData.page.current = current
            list()
        }

        list()

        return {
            deviceAddFormRef,
            deviceImportFormRef,
            reactiveData,
            hasData,
            search,
            reset,
            showAdd,
            addThing,
            showImport,
            importTemplate,
            importThing,
            disableThing,
            enableThing,
            deleteThing,
            refresh,
            sort,
            sizeChange,
            currentChange
        }
    }
})
