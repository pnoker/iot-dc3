<template>
    <div class="thing-card thing-card-dashboard">
        <el-card shadow="hover">
            <div class="thing-card-content">
                <div class="thing-card__body">
                    <div class="thing-card-body__content">
                        <div class="thing-card-body-content-dashboard-snapshot" @click="show()">
                            <img :src="snapshot">
                        </div>
                    </div>
                </div>
                <div class="thing-card__footer">
                    <div class="thing-card-footer-title">
                        <span class="thing-card-body-content-dashboard-name">{{name}}</span>
                    </div>
                </div>
            </div>
        </el-card>
    </div>
</template>

<script>
    import {encode} from 'js-base64'

    export default {
        name: "dashboard-card",
        props: {
            name: {
                type: String,
                default: '数据看板'
            },
            snapshot: {
                type: String,
                default: 'images/dashboard/dashboard.jpg'
            },
            url: {
                type: String,
                default: ''
            }
        },
        methods: {
            show() {
                if (this.url) {
                    this.$router.push({name: 'dashboard', query: {id: encode(this.url)}});
                }
            }
        }
    };
</script>

<style lang="scss">
    @import "~@/components/card/styles/thing-card.scss";

    .thing-card-dashboard {
        width: 300px;
        height: 210px;
        margin-right: 10px;
        margin-bottom: 10px;

        .el-card {
            width: 100%;
            min-width: 300px;
            min-height: 210px !important;
            box-sizing: border-box;
        }
    }

    .thing-card-body-content-dashboard-snapshot {
        width: 280px;
        min-width: 200px;
        max-width: 280px;
        cursor: pointer;

        img {
            width: 280px;
            min-width: 200px;
            max-width: 280px;
        }
    }

    .thing-card-body-content-dashboard-name {
        display: block;
        width: 280px;
        overflow: hidden;
        white-space: nowrap;
        text-overflow: ellipsis;
    }
</style>
