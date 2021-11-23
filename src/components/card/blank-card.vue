<template>
    <div class="blank-card">
        <el-card shadow="hover">
            <slot/>
        </el-card>
    </div>
</template>

<script>
    export default {
        name: "blank-card"
    };
</script>

<style lang="scss">
    .blank-card {
        border-radius: 5px;
        box-sizing: border-box;

        ul {
            list-style: none;

            li {
                font-size: 13px;
                margin-top: 8px;
            }
        }

        .el-card {
            width: 100%;
            box-sizing: border-box;
        }

        .el-card__body {
            padding: 10px 3px;
        }
    }
</style>
