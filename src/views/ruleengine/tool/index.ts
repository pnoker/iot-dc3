/*
 * Copyright 2016-present the original author or authors.
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

import { Order } from '@/config/types'
import { Plus, Refresh, RefreshRight, Search, Sort, Upload } from '@element-plus/icons-vue'
import { FormInstance, FormRules } from 'element-plus'
import { defineComponent, reactive, ref, unref } from 'vue'

export default defineComponent({
    name: 'RuleengineTool',
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
    emits: ['search', 'reset', 'show-add', 'show-import', 'refresh', 'sort', 'size-change', 'current-change'],
    setup(props, { emit }) {
        // 定义表单引用
        const formDataRef = ref<FormInstance>()

        // 定义响应式数据
        const reactiveData = reactive({
            formData: {} as any,
            rulePage: {
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
            Upload,
            Search,
            RefreshRight,
            Refresh,
            Sort,
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

        const showAdd = () => {
            emit('show-add')
        }

        const showImport = () => {
            emit('show-import')
        }

        const refresh = () => {
            emit('refresh')
        }

        const sort = () => {
            emit('sort')
        }

        const sizeChange = (size: any) => {
            emit('size-change', size)
        }

        const currentChange = (current: any) => {
            emit('current-change', current)
        }

        return {
            formDataRef,
            formRule,
            reactiveData,
            search,
            reset,
            showAdd,
            showImport,
            refresh,
            sort,
            sizeChange,
            currentChange,
            ...Icon,
        }
    },
})
