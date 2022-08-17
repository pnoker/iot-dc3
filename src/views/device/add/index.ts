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

import { Dictionary, Order } from '@/config/type/types'

import { successMessage } from '@/util/utils'
import { driverDictionaryApi, profileDictionaryApi } from '@/api/dictionary'

export default defineComponent({
    name: 'DeviceAddForm',
    emits: ['add-thing'],
    setup(props, { emit }) {
        // 定义表单引用
        const formDataRef = ref<FormInstance>()

        // 定义响应式数据
        const reactiveData = reactive({
            formData: {
                multi: false,
            } as any,
            formVisible: false,
            driverQuery: '',
            driverDictionary: [] as Dictionary[],
            driverPage: {
                total: 0,
                size: 5,
                current: 1,
                orders: [] as Order[],
            },
            profileQuery: '',
            profileDictionary: [] as Dictionary[],
            profilePage: {
                total: 0,
                size: 5,
                current: 1,
                orders: [] as Order[],
            },
        })

        // 定义表单校验规则
        const formRule = reactive<FormRules>({
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
            description: [
                {
                    max: 300,
                    message: '最多输入300个字符',
                    trigger: 'blur',
                },
            ],
        })

        const driverDictionary = () => {
            driverDictionaryApi({
                page: reactiveData.driverPage,
                label: reactiveData.driverQuery,
            })
                .then((res) => {
                    const data = res.data.data
                    reactiveData.driverPage.total = data.total
                    reactiveData.driverDictionary = data.records
                })
                .catch(() => {
                    // nothing to do
                })
        }

        const driverCurrentChange = (current) => {
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
            profileDictionaryApi({
                page: reactiveData.profilePage,
                label: reactiveData.profileQuery,
            })
                .then((res) => {
                    const data = res.data.data
                    reactiveData.profilePage.total = data.total
                    reactiveData.profileDictionary = data.records
                })
                .catch(() => {
                    // nothing to do
                })
        }

        const profileCurrentChange = (current) => {
            reactiveData.driverPage.current = current
            profileDictionary()
        }

        const profileDictionaryVisible = (visible: boolean) => {
            if (visible) {
                reactiveData.profileQuery = ''
                profileDictionary()
            }
        }

        const show = () => {
            reactiveData.formVisible = true
        }
        const cancel = () => {
            reactiveData.formVisible = false
        }
        const reset = () => {
            const form = unref(formDataRef)
            form?.resetFields()
        }
        const addThing = () => {
            const form = unref(formDataRef)
            form?.validate((valid) => {
                if (valid) {
                    emit('add-thing', reactiveData.formData, () => {
                        cancel()
                        reset()
                        successMessage(null)
                    })
                }
            })
        }

        return {
            formDataRef,
            formRule,
            reactiveData,
            driverDictionary,
            driverCurrentChange,
            driverDictionaryVisible,
            profileDictionary,
            profileCurrentChange,
            profileDictionaryVisible,
            show,
            cancel,
            reset,
            addThing,
        }
    },
})
