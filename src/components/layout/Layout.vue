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
    <div class="container">
        <div class="header">
            <el-col :span="4" class="header_item">
                <img class="header_logo" src="images/logo/logo.png">
            </el-col>
            <el-col :span="16" class="header_item">
                <el-menu class="header_menu"
                         :default-active="this.handleMenuEnter(this.$route.path)"
                         mode="horizontal"
                         :router=true>
                    <el-menu-item index="/home">
                        <el-icon>
                            <HomeFilled/>
                        </el-icon>
                        首页
                    </el-menu-item>
                    <template v-for="menu in menus">
                        <el-sub-menu :index="menu.path" v-if="menu.children">
                            <template v-slot:title>{{ menu.meta.title }}</template>
                            <el-menu-item v-for="m in menu.children" :index="m.path" :key="m.path">{{ m.meta.title }}</el-menu-item>
                        </el-sub-menu>
                        <el-menu-item :index="menu.path" v-else>
                            <el-icon>
                                <component :is="menu.meta.icon"></component>
                            </el-icon>
                            {{ menu.meta.title }}
                        </el-menu-item>
                    </template>
                </el-menu>
            </el-col>
            <el-col :span="4" class="header_item header_user">
                <el-dropdown class="user_avatar" @command="handleCommand">
                    <span class="el-dropdown-link">
                        <el-avatar>
                            <img src="images/common/avatar.png">
                        </el-avatar>
                    </span>
                    <el-dropdown-menu slot="dropdown">
                        <el-dropdown-item command="help">关于</el-dropdown-item>
                        <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                    </el-dropdown-menu>
                </el-dropdown>
                <el-badge :value="3" :max="99" class="user_badge" type="primary">
                    <span class="small" @click="handleMessage">管理员</span>
                </el-badge>
            </el-col>
        </div>
        <div class="body">
            <el-scrollbar>
                <router-view/>
            </el-scrollbar>
        </div>
    </div>
</template>

<script>
import menu from "@/config/router/views";
import {HomeFilled} from "@element-plus/icons-vue"

export default {
    data() {
        return {
            menus: menu.children.filter(view => view.name !== "home")
        }
    },
    components: {HomeFilled},
    methods: {
        handleMenuEnter(index) {
            if (index.indexOf("/") === 0) {
                let split = index.split("/");
                if (split.length > 2) {
                    return "/" + split[1];
                }
            }
            return index;
        },
        handleMessage() {
            this.$message("待开发");
        },
        handleCommand(command) {
            if (command === "logout") {
                this.$store.dispatch("ClearToken").then(() => this.$router.push("/login"))
                    .catch(() => {
                    });
            } else if (command === "help") {
                window.open("https://doc.dc3.site");
            }
        }
    }
}
</script>

<style lang="scss">
body {
    margin: 0;
    min-width: 1280px;
    min-height: 768px;
}

.container {
    color: #2c3e50;
    -moz-osx-font-smoothing: grayscale;
    -webkit-font-smoothing: antialiased;
    font-family: "Avenir", Helvetica, Arial, sans-serif;
}

.header {
    top: 0;
    width: 100%;
    height: 60px;
    display: flex;
    border-bottom: 1px solid #dcdfe6;

    .header_item {
        height: 100%;
    }

    .header_logo {
        height: 60px;
        margin-left: 10px;
    }

    .header_menu {
        display: flex;
        justify-content: center;
        border-bottom: none !important;
    }

    .header_menu .el-menu-item {
        font-size: 15px;
    }

    .header_user {
        display: flex;
        justify-content: flex-end;
        padding-right: 10px;

        .user_avatar {
            top: 10px;
            cursor: pointer;
        }

        .user_badge {
            top: 25px;
            cursor: pointer;
            margin-right: 40px;
        }

        .small {
            color: #909399;
            font-size: 14px;
            margin: 20px 10px;
        }
    }
}

.body {
    top: 60px;
    right: 0;
    left: 0;
    bottom: 0;
    padding: 5px 0 5px 0;
    position: absolute;
    background: #f0f2f5;
}
</style>


