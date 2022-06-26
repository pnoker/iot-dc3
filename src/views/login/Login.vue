<!--
  - Copyright (c) 2022. Pnoker. All Rights Reserved.
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -     http://www.apache.org/licenses/LICENSE-2.0
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->

<template>
    <div class="login-container">
        <div class="login-wrapper-left animated bounceInDown">
            <div class="login-left">
                <img class="img" src="images/logo/logo-white.png"/>
            </div>
        </div>
        <particles/>
        <div class="login-wrapper-right animated bounceInDown">
            <div class="login-border">
                <div class="login-main">
                    <h4 class="login-title">DC3 IoT 平台</h4>
                    <el-form class="login-form"
                             ref="formDataRef"
                             status-icon
                             :rules="formRule"
                             :model="formData"
                    >
                        <el-form-item prop="tenant">
                            <el-input
                                :prefix-icon="Box"
                                auto-complete="off"
                                placeholder="请输入租户名"
                                @keyup.enter="handleLogin(formDataRef)"
                                v-model="formData.tenant"
                            >
                            </el-input>
                        </el-form-item>
                        <el-form-item prop="name">
                            <el-input
                                :prefix-icon="User"
                                auto-complete="off"
                                placeholder="请输入用户名"
                                @keyup.enter="handleLogin(formDataRef)"
                                v-model="formData.name"
                            >
                            </el-input>
                        </el-form-item>
                        <el-form-item prop="password">
                            <el-input
                                :prefix-icon="Lock"
                                :type="common.passwordType"
                                auto-complete="off"
                                placeholder="请输入密码"
                                @keyup.enter="handleLogin(formDataRef)"
                                v-model="formData.password"
                            >
                                <template #append>
                                    <el-button :icon="common.isHide" @click="showPassword"/>
                                </template>
                            </el-input>
                        </el-form-item>
                        <el-form-item>
                            <el-button
                                type="primary"
                                @click.prevent="handleLogin(formDataRef)"
                                class="login-submit"
                            >登录
                            </el-button>
                        </el-form-item>
                    </el-form>
                </div>
            </div>
        </div>
    </div>
</template>

<script lang="ts">
import Login from "@/views/login"

export default Login
</script>

<style lang="less">
@import "~@/views/login/style.less";
</style>
