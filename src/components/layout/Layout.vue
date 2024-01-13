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
    <div class="container">
        <div class="header">
            <el-col :span="4" class="header_item">
                <img class="header_logo" src="/images/logo/logo.png" />
            </el-col>
            <el-col :span="16" class="header_item">
                <el-menu :default-active="handleMenuEnter($route.path)" mode="horizontal" :router="true" class="header_menu">
                    <el-menu-item index="/home">
                        <el-icon>
                            <HomeFilled />
                        </el-icon>
                        首页
                    </el-menu-item>
                    <template v-for="menu in menus">
                        <el-sub-menu v-if="menu.children" :key="menu.name" :index="menu.path">
                            <template #title>{{ menu.meta.title }}</template>
                            <el-menu-item v-for="m in menu.children" :key="m.path" :index="m.path">{{ m.meta.title }}</el-menu-item>
                        </el-sub-menu>
                        <el-menu-item v-else :key="menu.name" :index="menu.path">
                            <el-icon>
                                <component :is="menu.meta.icon"></component>
                            </el-icon>
                            {{ menu.meta.title }}
                        </el-menu-item>
                    </template>
                </el-menu>
            </el-col>
            <el-col :span="4" class="header_item header_user">
                <el-dropdown class="user_avatar" trigger="click" @command="handleCommand">
                    <span class="el-dropdown-link">
                        <el-avatar>
                            <img src="/images/common/avatar.png" />
                        </el-avatar>
                    </span>
                    <template #dropdown>
                        <el-dropdown-menu>
                            <el-dropdown-item command="help">关于</el-dropdown-item>
                            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                        </el-dropdown-menu>
                    </template>
                </el-dropdown>
                <el-badge :value="3" :max="99" class="user_badge" type="primary">
                    <span class="small" @click="handleMessage">管理员</span>
                </el-badge>
            </el-col>
        </div>
        <div class="body">
            <el-scrollbar>
                <router-view />
            </el-scrollbar>
        </div>
    </div>
</template>

<script setup lang="ts">
import router from '@/config/router'
import menu from '@/config/router/views'
import { warning } from '@/utils/MessageUtils'
import { HomeFilled } from '@element-plus/icons-vue'
import { computed } from 'vue'
import { useStore } from 'vuex'

const store = useStore()

const menus = computed(() => {
    const children = menu?.children || []
    return children.filter((view) => view.name !== 'home')
})

const handleMenuEnter = (index: string) => {
    if (index.indexOf('/') === 0) {
        const split = index.split('/')
        if (split.length > 2) {
            return '/' + split[1]
        }
    }
    return index
}

const handleMessage = () => {
    warning('待开发')
}

const handleCommand = (command: string) => {
    if (command === 'logout') {
        store.dispatch('auth/logout').then(() => router.push({ path: '/login' }))
    } else if (command === 'help') {
        window.open('https://doc.dc3.site')
    }
}
</script>

<style lang="scss">
@import '@/components/layout/style.scss';
</style>
