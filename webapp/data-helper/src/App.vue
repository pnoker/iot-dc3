<template>
    <div class="container">
        <el-menu class="nav"
                 :default-active="$store.state.navIndex"
                 mode="horizontal"
                 :router=true
                 @select="handleSelect">
            <el-menu-item index="/">首页</el-menu-item>
            <el-menu-item index="3" disabled>消息中心</el-menu-item>
            <el-menu-item index="/about">关于</el-menu-item>
        </el-menu>
        <div class="body">
            <el-scrollbar style="height: 100%;">
                <router-view/>
            </el-scrollbar>
        </div>
    </div>
</template>

<style lang="scss">
    body {
        margin: 0px;
    }

    .container {
        font-family: 'Avenir', Helvetica, Arial, sans-serif;
        -webkit-font-smoothing: antialiased;
        -moz-osx-font-smoothing: grayscale;
        text-align: center;
        color: #2c3e50;
    }

    .nav {
        width: 100%;
        position: fixed;
        top: 0px;
    }

    .body {
        position: absolute;
        width: 100%;
        top: 60px;
        bottom: 5px;

        .el-scrollbar__wrap {
            overflow-x: hidden;
        }
    }
</style>

<script>
    export default {
        data() {
            return {};
        },
        created() {
        },
        methods: {
            handleSelect(key) {
                this.$store.commit('handleSelect', key);
            }
        }
    }
</script>
