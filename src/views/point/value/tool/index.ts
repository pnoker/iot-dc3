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

import { defineComponent, reactive, ref, unref } from 'vue'
import { FormInstance, FormRules } from 'element-plus'
import { Plus, RefreshRight, Search, Refresh } from '@element-plus/icons-vue'
import { Dictionary, Order } from '@/config/type/types'
import { deviceDictionaryApi, pointDictionaryApi } from '@/api/dictionary'

export default defineComponent({
    name: 'PointValueTool',
    props: {
        embedded: {
            type: String,
            default: () => {
                return ''
            },
        },
        page: {
            type: Object,
            default: () => {
                return {}
            },
        },
    },
    emits: ['search', 'reset', 'refresh', 'size-change', 'current-change'],
    setup(props, { emit }) {
        // 定义表单引用
        const formDataRef = ref<FormInstance>()

        // 定义响应式数据
        const reactiveData = reactive({
            formData: {} as any,
            deviceQuery: '',
            deviceLoading: false,
            deviceDictionary: [] as Dictionary[],
            devicePage: {
                total: 0,
                size: 5,
                current: 1,
                orders: [] as Order[],
            },
            pointQuery: '',
            pointLoading: false,
            pointDictionary: [] as Dictionary[],
            pointPage: {
                total: 0,
                size: 5,
                current: 1,
                orders: [] as Order[],
            },
        })

        // 定义表单校验规则
        const formRule = reactive<FormRules>({})

        // 图标
        const Icon = {
            Plus,
            RefreshRight,
            Search,
            Refresh,
        }

        const deviceDictionary = () => {
            reactiveData.deviceLoading = true
            deviceDictionaryApi({
                page: reactiveData.devicePage,
                label: reactiveData.deviceQuery,
            })
                .then((res) => {
                    const data = res.data.data
                    reactiveData.devicePage.total = data.total
                    reactiveData.deviceDictionary = data.records
                })
                .catch(() => {
                    // nothing to do
                })
                .finally(() => {
                    reactiveData.deviceLoading = false
                })
        }

        const deviceCurrentChange = (current) => {
            reactiveData.devicePage.current = current
            deviceDictionary()
        }

        const pointDictionary = () => {
            reactiveData.pointLoading = true
            pointDictionaryApi({
                page: reactiveData.pointPage,
                label: reactiveData.pointQuery,
                parentValue2: reactiveData.formData.deviceId,
            })
                .then((res) => {
                    const data = res.data.data
                    reactiveData.pointPage.total = data.total
                    reactiveData.pointDictionary = data.records
                })
                .catch(() => {
                    // nothing to do
                })
                .finally(() => {
                    reactiveData.pointLoading = false
                })
        }

        const pointCurrentChange = (current) => {
            reactiveData.pointPage.current = current
            pointDictionary()
        }

        const pointDictionaryChange = () => {
            reactiveData.formData.pointId = ''
            pointDictionary()
        }

        const deviceDictionaryVisible = (visible: boolean) => {
            if (visible) {
                reactiveData.deviceQuery = ''
                deviceDictionary()
            }
        }

        const pointDictionaryVisible = (visible: boolean) => {
            if (visible) {
                reactiveData.pointQuery = ''
                pointDictionary()
            }
        }

        const search = () => {
            const form = unref(formDataRef)
            form?.validate((valid) => {
                if (valid) {
                    emit('search', reactiveData.formData)
                }
            })
        }

        const reset = () => {
            const form = unref(formDataRef)
            form?.resetFields()
            emit('reset')
        }
        const refresh = () => {
            emit('refresh')
        }
        const sizeChange = (size) => {
            emit('size-change', size)
        }
        const currentChange = (current) => {
            emit('current-change', current)
        }

        return {
            formDataRef,
            reactiveData,
            formRule,
            deviceDictionary,
            deviceCurrentChange,
            pointDictionary,
            pointCurrentChange,
            pointDictionaryChange,
            deviceDictionaryVisible,
            pointDictionaryVisible,
            search,
            reset,
            refresh,
            sizeChange,
            currentChange,
            ...Icon,
        }
    },
})
