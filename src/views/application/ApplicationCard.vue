<template>
    <div class="things-card things-card-application">
        <el-card shadow="hover">
            <div class="things-card-content">
                <div class="things-card__body">
                    <div class="things-card-body-content">
                        <div class="things-card-body-content-application-icon" @click="show()">
                            <el-avatar shape="square" :size="120" fit="fill" :src="icon"></el-avatar>
                        </div>
                    </div>
                </div>
                <div class="things-card__footer">
                    <div class="things-card-footer-title">
                        <span class="things-card-body-content-application-name">{{name}}</span>
                    </div>
                </div>
            </div>
        </el-card>
    </div>
</template>

<script>
    import {encode} from 'js-base64';

    export default {
        name: "application-card",
        props: {
            name: {
                type: String,
                default: '数据看板'
            },
            icon: {
                type: String,
                default: 'images/app/application.png'
            },
            url: {
                type: String,
                default: ''
            }
        },
        methods: {
            show() {
                if (this.url) {
                    this.$router.push({name: 'application', query: {id: encode(this.url)}})
                        .catch(() => {
                        });
                }
            }
        }
    };
</script>

<style lang="scss">
    @import "~@/components/card/styles/things-card.scss";

    .things-card-application {
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

    .things-card-body-content-application-icon {
        width: 120px;
        cursor: pointer;

        .el-avatar {
            background: transparent;
        }
    }

    .things-card-body-content-application-name {
        display: block;
        width: 120px;
        overflow: hidden;
        white-space: nowrap;
        text-overflow: ellipsis;
    }

</style>
