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

import { successMessage } from '@/util/utils'

export default defineComponent({
    name: 'PointValueEditForm',
    props: {
        formData: {
            type: Object,
            default: () => {
                return {}
            },
        },
    },
    emits: ['update-thing'],
    setup(props, { emit }) {
        // 定义表单引用
        const formDataRef = ref<FormInstance>()

        // 定义响应式数据
        const reactiveData = reactive({
            formVisible: false,
        })

        // 定义表单校验规则
        const formRule = reactive<FormRules>({
            value: [
                {
                    required: true,
                    message: '请输入位号值',
                    trigger: 'blur',
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
        const updateThing = () => {
            const form = unref(formDataRef)
            form?.validate((valid) => {
                if (valid) {
                    emit('update-thing', props.formData, () => {
                        cancel()
                        reset()
                        successMessage(null)
                    })
                }
            })
        }

        return {
            formDataRef,
            reactiveData,
            formRule,
            show,
            cancel,
            reset,
            updateThing,
        }
    },
})
