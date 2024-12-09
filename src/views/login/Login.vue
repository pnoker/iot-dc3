<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -      https://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->

<template>
    <div class="login-container">
        <div class="login-wrapper-left animated bounce-in-down">
            <div class="login-left">
                <img class="img" src="/images/logo/logo-white.png" />
            </div>
        </div>
        <particles />
        <div class="login-wrapper-right animated bounce-in-down">
            <div class="login-right">
                <div class="login-main">
                    <h4 class="login-title">IoT DC3 平台</h4>
                    <el-form class="login-form" ref="formDataRef" status-icon :rules="formRule" :model="reactiveData.formData">
                        <el-form-item prop="tenant">
                            <el-input v-model="reactiveData.formData.tenant" :prefix-icon="Box" placeholder="请输入租户名" auto-complete="off" @keyup.enter="handleLogin">
                            </el-input>
                        </el-form-item>
                        <el-form-item prop="name">
                            <el-input v-model="reactiveData.formData.name" :prefix-icon="User" placeholder="请输入用户名" auto-complete="off" @keyup.enter="handleLogin"></el-input>
                        </el-form-item>
                        <el-form-item prop="password">
                            <el-input
                                v-model="reactiveData.formData.password"
                                :prefix-icon="Lock"
                                :type="reactiveData.passwordType"
                                placeholder="请输入密码"
                                auto-complete="off"
                                @keyup.enter="handleLogin"
                            >
                                <template #append>
                                    <el-button :icon="reactiveData.isHide" @click="showPassword" />
                                </template>
                            </el-input>
                        </el-form-item>
                        <el-form-item>
                            <el-button class="login-submit" type="primary" @click.prevent="handleLogin">登录</el-button>
                        </el-form-item>
                    </el-form>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { reactive, ref, unref } from 'vue'
import { FormInstance, FormRules } from 'element-plus'
import { Box, Lock, User } from '@element-plus/icons-vue'

import { useStore } from 'vuex'

import Particles from '@/components/particles/particles.vue'

const store = useStore()
// 定义表单引用
const formDataRef = ref<FormInstance>()

// 定义响应式数据
const tenant = store.getters['auth/getTenant'] || 'default'
const name = store.getters['auth/getName'] || 'pnoker'
const reactiveData = reactive({
    isHide: 'View',
    passwordType: 'password',
    formData: {
        tenant: tenant,
        name: name,
        password: 'dc3dc3dc3'
    }
})

// 定义表单校验规则
const formRule = reactive<FormRules>({
    tenant: [{ required: true, message: '请输入租户名', trigger: 'blur' }],
    name: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, message: '密码长度最少为6位', trigger: 'blur' }
    ]
})

// 显示、隐藏密码
const showPassword = () => {
    reactiveData.passwordType === '' ? (reactiveData.passwordType = 'password') : (reactiveData.passwordType = '')
    reactiveData.isHide === 'View' ? (reactiveData.isHide = 'Hide') : (reactiveData.isHide = 'View')
}

// 登录
const handleLogin = () => {
    const form = unref(formDataRef)
    form?.validate((valid) => {
        if (valid) store.dispatch('auth/login', reactiveData.formData)
    })
}
</script>

<style lang="scss">
@use '@/views/login/style.scss';
</style>
