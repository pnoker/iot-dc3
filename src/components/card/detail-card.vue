<template>
    <div class="card_detail">
        <el-card shadow="hover">
            <div slot="header">
                <span class="detail_header">{{title}}</span>
            </div>
            <div class="text item detail_content">
                <div class="detail_content_button">
                    <el-button size="small" icon="el-icon-back">返回</el-button>
                </div>
                <div class="detail_content_description">
                    <title-card title="设备状态">
                        <span class="detail-content-value">在线</span>
                    </title-card>
                    <title-card title="设备在线率">
                        <span class="detail-content-value">99%</span>
                    </title-card>
                    <title-card title="设备故障率">
                        <span class="detail-content-value">10%</span>
                    </title-card>
                    <title-card title="7日数据量">
                        <span class="detail-content-value">10000个</span>
                    </title-card>
                    <title-card title="设备在线时长">
                        <span class="detail-content-value">100天</span>
                    </title-card>
                    <title-card title="设备离线时长">
                        <span class="detail-content-value">10小时</span>
                    </title-card>
                </div>
            </div>
        </el-card>
    </div>
</template>

<script>
    import TitleCard from "@/components/card/title-card";

    export default {
        name: "detail-card",
        components: {TitleCard},
        data() {
            return {
                center: {lng: 0, lat: 0},
                zoom: 3,
            }
        },
        props: {
            title: {
                type: String,
                default: ''
            }
        },
        methods: {
            handler({BMap, map}) {
                console.log(BMap, map)
                this.center.lng = 116.404
                this.center.lat = 39.915
                this.zoom = 15
            }
        }
    };
</script>

<style lang="scss">
    .card_detail {
        padding: 2px 3px;
        border-radius: 5px;
        box-sizing: border-box;

        .detail_header {
            font-size: 14px;
            font-weight: bold;
        }

        .detail_content {
            display: flex;
            flex-direction: column;

            .detail_content_button {
                display: flex;
                margin-bottom: 10px;
            }

            .detail_content_description {
                display: flex;
                justify-content: space-between;
                align-items: center;

                .detail-content-value {
                    text-align: center;
                    font-weight: bold;
                    font-size: xx-large;
                    color: #67c23a;
                }
            }
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
