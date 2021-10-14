<template>
    <el-dialog title="数据详情" :visible.sync="detailVisible"
               class="thing-dialog"
               :show-close="false"
               :append-to-body="true"
               :model-append-to-body="false"
               :close-on-click-modal="false"
               :close-on-press-escape="false">
        <pre v-highlightjs><code class="json">{{detailData}}</code></pre>
        <div slot="footer" class="dialog-footer">
            <el-button type="success" size="small" @click="cancel">关闭</el-button>
        </div>
    </el-dialog>
</template>

<script>

    export default {
        name: "point-value-detail",
        props: {
            detailData: {
                type: Object,
                default: () => {
                    return {}
                }
            }
        },
        data() {
            return {
                detailVisible: false
            }
        },
        methods: {
            show() {
                this.detailVisible = true;
            },
            cancel() {
                this.detailVisible = false;
            }
        }
    };
</script>

<style lang="scss">
    @import "~@/components/dialog/styles/thing-dialog.scss";
</style>
