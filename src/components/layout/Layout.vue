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
            <el-col class="header_item" :span="4">
                <img class="header_logo" src="/images/logo/logo.png" />
            </el-col>
            <el-col class="header_item" :span="16">
                <el-menu class="header_menu" :default-active="handleMenuEnter($route.path)" mode="horizontal" :router="true">
                    <el-menu-item index="/home">
                        <el-icon>
                            <HomeFilled />
                        </el-icon>
                        首页
                    </el-menu-item>
                    <template v-for="menusItem in menus">
                        <el-sub-menu v-if="menusItem.children" :key="menusItem.path" :index="menusItem.path">
                            <template #title>{{ (menusItem.meta as any).title }}</template>
                            <el-menu-item v-for="item in menusItem.children" :key="item.path" :index="item.path">{{ (menusItem.meta as any).title }}</el-menu-item>
                        </el-sub-menu>
                        <el-menu-item v-else :key="menusItem.path" :index="menusItem.path">
                            <el-icon>
                                <component :is="(menusItem.meta as any).icon"></component>
                            </el-icon>
                            {{ (menusItem.meta as any).title }}
                        </el-menu-item>
                    </template>
                </el-menu>
            </el-col>
            <el-col class="header_item header_user" :span="4">
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
                <el-badge class="user_badge" :value="3" :max="99" type="primary">
                    <span class="user_name" @click="handleMessage">管理员</span>
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
import { warning } from '@/utils/MessageUtil'
import { HomeFilled } from '@element-plus/icons-vue'
import { computed } from 'vue'
import { useStore } from 'vuex'
import { RouteRecordRaw } from 'vue-router'

const store = useStore()

const menus = computed(() => {
    const children: RouteRecordRaw[] = menu?.children || []
    return children.filter((view) => view.path !== '/home')
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
        store.dispatch('auth/logout').then(() => router.push({ name: 'login' }))
    } else if (command === 'help') {
        window.open('https://doc.dc3.site')
    }
}
</script>

<style lang="scss">
@use '@/components/layout/style.scss';
</style>
