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

import { defineComponent, reactive, ref, unref } from 'vue'
import { FormInstance, FormRules } from 'element-plus'
import { Box, Lock, User } from '@element-plus/icons-vue'

import { useStore } from 'vuex'

import Particles from '@/components/particles/particles.vue'

export default defineComponent({
    name: 'Login',
    components: { Particles },
    setup() {
        const store = useStore()

        // 定义表单引用
        const formDataRef = ref<FormInstance>()

        // 图标
        const Icon = {
            Box,
            User,
            Lock,
        }

        // 定义响应式数据
        const reactiveData = reactive({
            isHide: 'View',
            passwordType: 'password',
            formData: {
                tenant: 'default',
                name: 'pnoker',
                password: 'dc3dc3dc3',
            },
        })

        // 定义表单校验规则
        const formRule = reactive<FormRules>({
            tenant: [{ required: true, message: '请输入租户名', trigger: 'blur' }],
            name: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
            password: [
                { required: true, message: '请输入密码', trigger: 'blur' },
                { min: 6, message: '密码长度最少为6位', trigger: 'blur' },
            ],
        })

        // 显示、隐藏密码
        const showPassword = () => {
            reactiveData.passwordType === ''
                ? (reactiveData.passwordType = 'password')
                : (reactiveData.passwordType = '')
            reactiveData.isHide === 'View' ? (reactiveData.isHide = 'Hide') : (reactiveData.isHide = 'View')
        }

        // 登录
        const handleLogin = () => {
            const form = unref(formDataRef)
            form?.validate((valid) => {
                if (valid) store.dispatch('auth/login', reactiveData.formData)
            })
        }

        return {
            formDataRef,
            reactiveData,
            formRule,
            showPassword,
            handleLogin,
            ...Icon,
        }
    },
})
