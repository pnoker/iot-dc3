<template>
    <div class="login-container">
        <div class="login-wrapper-left animated bounceInDown">
            <div class="login-left">
                <img class="img" src="images/logo/logo-white.png">
            </div>
        </div>
        <particles/>
        <div class="login-wrapper-right animated bounceInDown">
            <div class="login-border">
                <div class="login-main">
                    <h4 class="login-title">IoT DC3 管理平台</h4>
                    <el-form ref="formData" class="login-form" status-icon label-width="0" :rules="formRule" :model="formData">
                        <el-form-item prop="tenant">
                            <el-input size="small"
                                      auto-complete="off"
                                      placeholder="请输入租户名"
                                      @keyup.enter.native="handleLogin"
                                      v-model="formData.tenant">
                                <i slot="prefix" class="el-icon-suitcase"/>
                            </el-input>
                        </el-form-item>
                        <el-form-item prop="name">
                            <el-input size="small"
                                      auto-complete="off"
                                      placeholder="请输入用户名"
                                      @keyup.enter.native="handleLogin"
                                      v-model="formData.name">
                                <i slot="prefix" class="el-icon-user"/>
                            </el-input>
                        </el-form-item>
                        <el-form-item prop="password">
                            <el-input size="small"
                                      :type="passwordType"
                                      auto-complete="off"
                                      placeholder="请输入密码"
                                      @keyup.enter.native="handleLogin"
                                      v-model="formData.password">
                                <i class="el-icon-view el-input__icon" slot="suffix" @click="showPassword"/>
                                <i slot="prefix" class="el-icon-lock"/>
                            </el-input>
                        </el-form-item>
                        <el-form-item>
                            <el-button type="primary"
                                       size="small"
                                       @click.native.prevent="handleLogin"
                                       class="login-submit">登录
                            </el-button>
                        </el-form-item>
                    </el-form>
                </div>
            </div>
        </div>
    </div>
</template>

<script src="./scripts/index.js" lang="js"></script>

<style lang="scss">
    @import "~@/components/login/styles/style.scss";
</style>
