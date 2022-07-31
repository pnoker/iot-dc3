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
import { Edit, RefreshLeft, Right } from '@element-plus/icons-vue'

import router from '@/config/router'
import { useRoute } from 'vue-router'

import { pointByIdApi, pointUpdateApi } from '@/api/point'

import pointList from '@/views/point/Point.vue'

export default defineComponent({
    components: { pointList },
    setup() {
        const route = useRoute()

        // 定义表单引用
        const formDataRef = ref<FormInstance>()

        // 图标
        const Icon = {
            Edit,
            RefreshLeft,
            Right,
        }

        // 定义响应式数据
        const reactiveData = reactive({
            id: route.query.id,
            active: +(route.query.active || 0),
            oldPointFormData: {},
            pointFormData: {} as any,
        })

        // 定义表单校验规则
        const pointFormRule = reactive<FormRules>({
            name: [
                {
                    required: true,
                    message: '请输入位号名称',
                    trigger: 'blur',
                },
                {
                    min: 2,
                    max: 32,
                    message: '请输入 2~32 位字长的位号名称',
                    trigger: 'blur',
                },
                {
                    pattern: /^[A-Za-z0-9\u4e00-\u9fa5][A-Za-z0-9\u4e00-\u9fa5-_]*$/,
                    message: '请输入正确格式的位号名称',
                },
            ],
            enable: [
                {
                    required: true,
                    message: '请选择使能',
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

        const point = () => {
            const id = route.query.id as string
            pointByIdApi(id)
                .then((res) => {
                    reactiveData.pointFormData = res.data.data
                    reactiveData.oldPointFormData = { ...res.data.data }
                })
                .catch(() => {
                    // nothing to do
                })
        }
        const pointUpdate = () => {
            const form = unref(formDataRef)
            form?.validate((valid) => {
                if (valid) {
                    pointUpdateApi(reactiveData.pointFormData)
                        .then((res) => {
                            reactiveData.oldPointFormData = { ...res.data.data }
                        })
                        .catch(() => {
                            // nothing to do
                        })
                }
            })
        }
        const pre = () => {
            reactiveData.active--
            changeActive(reactiveData.active)
        }
        const next = () => {
            reactiveData.active++
            if (reactiveData.active > 0) {
                router.push({ name: 'profileEdit', query: { id: route.query.profileId, active: '1' } }).catch(() => {
                    // nothing to do
                })
            } else {
                changeActive(reactiveData.active)
            }
        }
        const pointReset = () => {
            reactiveData.pointFormData = { ...reactiveData.oldPointFormData }
        }
        const changeActive = (step) => {
            const query = route.query
            router.push({ query: { ...query, active: step } }).catch(() => {
                // nothing to do
            })
        }

        point()

        return {
            formDataRef,
            pointFormRule,
            reactiveData,
            pointUpdate,
            pre,
            next,
            pointReset,
            changeActive,
            ...Icon,
        }
    },
})
