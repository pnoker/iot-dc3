<template>
    <div class="card_base">
        <el-card shadow="hover">
            <slot/>
        </el-card>
    </div>
</template>

<script>
    export default {
        name: "base-card"
    };
</script>

<style lang="scss">
    .card_base {
        padding: 2px 3px;
        border-radius: 5px;
        box-sizing: border-box;

        .el-card {
            width: 100%;
            box-sizing: border-box;
        }

        .el-card__body {
            padding: 10px;
        }

        &:first-child {
            padding-top: 0;
            padding-left: 0;
        }
    }
</style>
