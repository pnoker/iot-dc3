<template>
    <div id="app">
        <el-menu id="nav" :default-active="$store.state.activeIndex" mode="horizontal" :router=true
                 @select="handleSelect">
            <el-menu-item index="/">Home</el-menu-item>
            <el-menu-item index="3" disabled>消息中心</el-menu-item>
            <el-menu-item index="/about">About</el-menu-item>
        </el-menu>
        <router-view/>
    </div>
</template>

<style lang="scss">
    body {
        margin: 0px;
    }

    #app {
        font-family: 'Avenir', Helvetica, Arial, sans-serif;
        -webkit-font-smoothing: antialiased;
        -moz-osx-font-smoothing: grayscale;
        text-align: center;
        color: #2c3e50;
    }

    .el-menu {
        box-shadow: 0 0 5px #888;
        width: 100%;
        position: fixed;
        top: 0px;
    }
</style>

<script>
    export default {
        data() {
            return {};
        },
        created() {
            this.$store.state.activeIndex = '/'
        },
        methods: {
            handleSelect(key, keyPath) {
                console.log(key, keyPath);
            }
        }
    }
</script>
