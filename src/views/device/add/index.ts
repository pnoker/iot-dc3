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

import { Dictionary } from '@/config/type/types'

import { successMessage } from '@/util/utils'

export default defineComponent({
    name: 'DeviceAddForm',
    props: {
        driverDictionary: {
            type: Array<Dictionary>,
            default: () => {
                return []
            },
        },
        profileDictionary: {
            type: Array<Dictionary>,
            default: () => {
                return []
            },
        },
    },
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
                    message: '请选择所属驱动 | 网关',
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
            show,
            cancel,
            reset,
            addThing,
        }
    },
})
