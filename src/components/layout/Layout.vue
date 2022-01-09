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
                    <el-menu-item index="/home"><span class="el-icon-s-home"/>首页</el-menu-item>
                    <template v-for="menu in menus">
                        <el-submenu :index="menu.path" v-if="menu.children">
                            <template slot="title">{{menu.meta.title}}</template>
                            <el-menu-item v-for="m in menu.children" :index="m.path" :key="m.path">{{m.meta.title}}</el-menu-item>
                        </el-submenu>
                        <el-menu-item :index="menu.path" v-else>
                            <span :class="menu.icon"/>
                            {{menu.meta.title}}
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
            <GeminiScrollbar>
                <router-view/>
            </GeminiScrollbar>
        </div>
    </div>
</template>

<script>
    import menu from "@/config/router/views";

    export default {
        data() {
            return {
                menus: menu.children.filter(view => view.name !== "home")
            };
        },
        methods: {
            handleMenuEnter(index) {
                if (index.indexOf('/') === 0) {
                    let split = index.split('/');
                    if (split.length > 2) {
                        return '/' + split[1];
                    }
                }
                return index;
            },
            handleMessage() {
                this.$message('待开发');
            },
            handleCommand(command) {
                if (command === 'logout') {
                    this.$store.dispatch('ClearToken').then(() => this.$router.push('/login'))
                        .catch(() => {
                        });
                } else if (command === 'help') {
                    window.open('https://doc.dc3.site');
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
        font-family: 'Avenir', Helvetica, Arial, sans-serif;
    }

    .header {
        top: 0;
        width: 100%;
        height: 60px;
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


