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

import { defineComponent, reactive, ref, unref } from 'vue'
import { FormInstance, FormRules } from 'element-plus'
import { Plus, Refresh, RefreshRight, Search, Sort } from '@element-plus/icons-vue'

import { useStore } from 'vuex'

export default defineComponent({
    name: 'DriverTool',
    props: {
        page: {
            type: Object,
            default: () => {
                return {}
            }
        },
        add: {
            type: Boolean,
            default: () => false
        }
    },
    emits: ['search', 'reset', 'refresh', 'sort', 'size-change', 'current-change'],
    setup(props, { emit }) {
        const store = useStore()

        // 定义表单引用
        const formDataRef = ref<FormInstance>()

        // 定义响应式数据
        const reactiveData = reactive({
            formData: {} as any
        })

        // 定义表单校验规则
        const formRule = reactive<FormRules>({
            port: [{ type: 'number', message: '端口必须为数字值' }]
        })

        // 图标
        const Icon = {
            Plus,
            Search,
            RefreshRight,
            Refresh,
            Sort
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

        const sort = () => {
            emit('sort')
        }

        const sizeChange = (size: number) => {
            emit('size-change', size)
        }

        const currentChange = (current: number) => {
            emit('current-change', current)
        }

        return {
            store,
            formDataRef,
            formRule,
            reactiveData,
            search,
            reset,
            refresh,
            sort,
            sizeChange,
            currentChange,
            ...Icon
        }
    }
})
