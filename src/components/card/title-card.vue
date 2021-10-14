<template>
    <div class="card_title">
        <el-card shadow="hover">
            <div slot="header">
                <span class="title_header">{{title}}</span>
            </div>
            <slot/>
        </el-card>
    </div>
</template>

<script>
    export default {
        name: "title-card",
        props: {
            title: {
                type: String,
                default: ''
            }
        }
    };
</script>

<style lang="scss">
    .card_title {
        padding: 2px 3px;
        border-radius: 5px;
        box-sizing: border-box;

        .title_header {
            font-size: 14px;
            font-weight: bold;
        }

        .el-card {
            width: 100%;
            box-sizing: border-box;
        }

        .el-card__header {
            padding: 10px;
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
