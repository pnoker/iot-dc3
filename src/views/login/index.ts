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

import { reactive, ref } from "vue"
import { FormInstance, FormRules } from "element-plus"
import { Box, Lock, User } from "@element-plus/icons-vue"

import Particles from "@/components/particles/particles.vue"
import { useStore } from "vuex"

export default {
    name: "Login",
    components: {Particles},
    setup() {
        const store = useStore()
        const formDataRef = ref<FormInstance>()

        // 图标
        const Icon = {
            Box,
            User,
            Lock
        }

        // 定义响应式数据
        let common = reactive({
            isHide: "View",
            passwordType: "password",
        })
        let formData = reactive({
            tenant: "default",
            name: "pnoker",
            password: "dc3dc3dc3",
        })

        // 定义Form规则
        let formRule = reactive<FormRules>({
            tenant: [{required: true, message: "请输入租户名", trigger: "blur"}],
            name: [{required: true, message: "请输入用户名", trigger: "blur"}],
            password: [
                {required: true, message: "请输入密码", trigger: "blur"},
                {min: 6, message: "密码长度最少为6位", trigger: "blur"},
            ],
        })

        // 显示、隐藏密码
        const showPassword = () => {
            common.passwordType === ""
                ? (common.passwordType = "password")
                : (common.passwordType = "")
            common.isHide === "View"
                ? (common.isHide = "Hide")
                : (common.isHide = "View")
        }

        // 登录
        const handleLogin = (formInstance: FormInstance) => {
            formInstance.validate((valid) => {
                if (valid) store.dispatch("auth/login", formData)
            })
        }

        return {
            store,
            formDataRef,
            common,
            formData,
            formRule,
            showPassword,
            handleLogin,
            ...Icon
        }
    }
}