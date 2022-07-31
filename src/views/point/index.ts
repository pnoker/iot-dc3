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

import { pointAddApi, pointDeleteApi, pointListApi } from '@/api/point'
import { profileByIdsApi } from '@/api/profile'
import { profileDictionaryApi } from '@/api/dictionary'

import { Dictionary, Order } from '@/config/type/types'

import skeletonCard from '@/components/card/skeleton/SkeletonCard.vue'
import pointTool from './tool/PointTool.vue'
import pointAddForm from './add/PointAddForm.vue'
import pointCard from './card/PointCard.vue'

export default defineComponent({
    components: {
        skeletonCard,
        pointTool,
        pointAddForm,
        pointCard,
    },
    props: {
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
    },
    emits: ['pre-handle', 'next-handle'],
    setup(props, { emit }) {
        const pointAddFormRef: any = ref<InstanceType<typeof pointAddForm>>()

        // 定义响应式数据
        const reactiveData = reactive({
            loading: true,
            profileDictionary: [] as Dictionary[],
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
            pointListApi({
                page: reactiveData.page,
                ...reactiveData.query,
                profileId: props.profileId,
            })
                .then((res) => {
                    const data = res.data.data
                    reactiveData.page.total = data.total
                    reactiveData.listData = data.records

                    // profile
                    const profileIds = Array.from(new Set(reactiveData.listData.map((point) => point.profileId)))
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
            reactiveData.query = {
                ...params,
                profileId: props.profileId,
            }
            list()
        }

        const reset = () => {
            reactiveData.query = {}
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

        const deleteThing = (id, done) => {
            pointDeleteApi(id)
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

        const preHandle = () => {
            emit('pre-handle')
        }

        const nextHandle = () => {
            emit('next-handle')
        }

        profile()
        list()

        return {
            pointAddFormRef,
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
            preHandle,
            nextHandle,
        }
    },
})
