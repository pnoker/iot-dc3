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

import { reactive, ref, computed, defineComponent } from 'vue'

import { addProfile, deleteProfile, getProfileList, updateProfile } from '@/api/profile'

import { Order } from '@/config/types'

import profileTool from '@/views/profile/tool/ProfileTool.vue'
import blankCard from '@/components/card/blank/BlankCard.vue'
import profileAddForm from '@/views/profile/add/ProfileAddForm.vue'
import skeletonCard from '@/components/card/skeleton/SkeletonCard.vue'
import profileCard from '@/views/profile/card/ProfileCard.vue'
import { isNull } from '@/utils/utils'
import { failMessage } from '@/utils/NotificationUtils'

export default defineComponent({
    components: {
        blankCard,
        skeletonCard,
        profileTool,
        profileAddForm,
        profileCard,
    },
    props: {
        embedded: {
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
    setup(props) {
        const profileAddFormRef: any = ref<InstanceType<typeof profileAddForm>>()

        // 定义响应式数据
        const reactiveData = reactive({
            loading: true,
            pointTable: {},
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
            if (!isNull(props.deviceId)) {
                reactiveData.query = {
                    ...reactiveData.query,
                    deviceId: props.deviceId,
                }
            }

            getProfileList({
                page: reactiveData.page,
                ...reactiveData.query,
            })
                .then((res) => {
                    const data = res.data
                    reactiveData.page.total = data.total
                    reactiveData.listData = data.records
                })
                .catch(() => {
                    // nothing to do
                })
                .finally(() => {
                    reactiveData.loading = false
                })
        }

        const search = (params) => {
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
            if (!isNull(props.deviceId)) {
                params = { deviceId: props.deviceId }
            }

            reactiveData.query = params
            list()
        }
        const showAdd = () => {
            profileAddFormRef.value.show()
        }

        const addThing = (form, done) => {
            addProfile(form).then(() => {
                list()
                done()
            })
        }

        const disableThing = (id, done) => {
            updateProfile({ id: id, enableFlag: 'DISABLE' }).then(() => {
                list()
                done()
            })
        }

        const enableThing = (id, done) => {
            updateProfile({ id: id, enableFlag: 'ENABLE' }).then(() => {
                list()
                done()
            })
        }

        const deleteThing = (id, done) => {
            deleteProfile(id)
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

        list()

        return {
            profileAddFormRef,
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
        }
    },
})
