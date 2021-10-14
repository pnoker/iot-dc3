<template>
    <div class="thing-card thing-card-application">
        <el-card shadow="hover">
            <div class="thing-content">
                <div class="thing-body">
                    <div class="thing-body-content">
                        <div class="thing-body-content-application-icon" @click="show()">
                            <el-avatar shape="square" :size="120" fit="fill" :src="icon"></el-avatar>
                        </div>
                    </div>
                </div>
                <div class="thing-footer">
                    <div class="thing-footer-title">
                        <span>{{name}}</span>
                    </div>
                </div>
            </div>
        </el-card>
    </div>
</template>

<script>
    import {encode} from 'js-base64'

    export default {
        name: "application-card",
        props: {
            name: {
                type: String,
                default: '数据看板'
            },
            icon: {
                type: String,
                default: 'https://fuss10.elemecdn.com/e/5d/4a731a90594a4af544c0c25941171jpeg.jpeg'
            },
            url: {
                type: String,
                default: ''
            }
        },
        methods: {
            show() {
                if (this.url) {
                    this.$router.push({name: 'application', query: {id: encode(this.url)}});
                }
            }
        }
    };
</script>

<style lang="scss">
    @import "~@/components/card/styles/thing-card.scss";

    .thing-card-application {
        width: 140px;
        height: 170px;
        margin-right: 10px;
        margin-bottom: 10px;

        .el-card {
            width: 100%;
            min-width: 140px !important;
            min-height: 170px !important;
            box-sizing: border-box;
        }
    }

    .thing-body-content-application-icon {
        width: 120px;
        cursor: pointer;
    }

</style>
