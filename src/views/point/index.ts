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

import { defineComponent, reactive, ref, computed } from 'vue'

import { pointAddApi, pointDeleteApi, getPointList, getPointUpdate } from '@/api/point'
import { getProfileByIds } from '@/api/profile'

import { Order } from '@/config/types'

import skeletonCard from '@/components/card/skeleton/SkeletonCard.vue'
import pointTool from './tool/PointTool.vue'
import pointAddForm from './add/PointAddForm.vue'
import pointCard from './card/PointCard.vue'
import blankCard from '@/components/card/blank/BlankCard.vue'
import { isNull } from '@/utils/utils'
import { failMessage } from '@/utils/NotificationUtils'

export default defineComponent({
    components: {
        skeletonCard,
        pointTool,
        pointAddForm,
        pointCard,
        blankCard,
    },
    props: {
        embedded: {
            type: String,
            default: () => {
                return ''
            },
        },
        pre: {
            type: Boolean,
            default: () => {
                return false
            },
        },
        next: {
            type: Boolean,
            default: () => {
                return false
            },
        },
        profileId: {
            type: String,
            default: () => {
                return ''
            },
        },
        deviceId: {
            type: String,
            default: () => {
                return ''
            },
        },
    },
    emits: ['pre-handle', 'next-handle'],
    setup(props, { emit }) {
        const pointAddFormRef: any = ref<InstanceType<typeof pointAddForm>>()

        // 定义响应式数据
        const reactiveData = reactive({
            loading: true,
            profileTable: {},
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
            if (!isNull(props.profileId)) {
                reactiveData.query = {
                    ...reactiveData.query,
                    profileId: props.profileId,
                }
            }
            if (!isNull(props.deviceId)) {
                reactiveData.query = {
                    ...reactiveData.query,
                    deviceId: props.deviceId,
                }
            }

            getPointList({
                page: reactiveData.page,
                ...reactiveData.query,
            })
                .then((res) => {
                    const data = res.data
                    reactiveData.page.total = data.total
                    reactiveData.listData = data.records

                    // profile
                    const profileIds = Array.from(new Set(reactiveData.listData.map((point) => point.profileId)))
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
                    reactiveData.loading = false
                })
        }

        const search = (params) => {
            if (!isNull(props.profileId)) {
                params = {
                    ...params,
                    profileId: props.profileId,
                }
            }
            if (!isNull(props.deviceId)) {
                params = {
                    ...params,
                    deviceId: props.deviceId,
                }
            }

            reactiveData.query = params
            list()
        }

        const reset = () => {
            let params = {}
            if (!isNull(props.profileId)) {
                params = { profileId: props.profileId }
            }
            if (!isNull(props.deviceId)) {
                params = { deviceId: props.deviceId }
            }

            reactiveData.query = params
            list()
        }

        const showAdd = () => {
            pointAddFormRef.value.show()
        }

        const addThing = (form, done) => {
            pointAddApi(form)
                .then(() => {
                    list()
                    done()
                })
                .catch(() => {
                    // nothing to do
                })
        }

        const disableThing = (id, profileId, done) => {
            getPointUpdate({ id: id, profileId: profileId, enableFlag: 'DISABLE' })
                .then(() => {
                    list()
                    done()
                })
                .catch(() => {
                    // nothing to do
                })
        }

        const enableThing = (id, profileId, done) => {
            getPointUpdate({ id: id, profileId: profileId, enableFlag: 'ENABLE' })
                .then(() => {
                    list()
                    done()
                })
                .catch(() => {
                    // nothing to do
                })
        }

        const deleteThing = (id, done) => {
            pointDeleteApi(id)
                .then((res) => {
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

        const sizeChange = (size) => {
            reactiveData.page.size = size
            list()
        }

        const currentChange = (current) => {
            reactiveData.page.current = current
            list()
        }

        const preHandle = () => {
            emit('pre-handle')
        }

        const nextHandle = () => {
            emit('next-handle')
        }

        list()

        return {
            pointAddFormRef,
            reactiveData,
            hasData,
            search,
            reset,
            showAdd,
            addThing,
            disableThing,
            enableThing,
            deleteThing,
            refresh,
            sort,
            sizeChange,
            currentChange,
            preHandle,
            nextHandle,
        }
    },
})
