<template>
    <div class="error">
        <div class="img"
             style=" background-image: url('/images/error/500.svg');">
        </div>
        <div class="content">
            <h1>500</h1>
            <div class="desc">抱歉，服务器出错了</div>
            <div class="actions">
                <router-link :to="{path:'/'}">
                    <el-button type="primary" plain>返回首页</el-button>
                </router-link>
            </div>
        </div>
    </div>
</template>

<style lang="scss" scoped>
    @import "./style.scss";
</style>
