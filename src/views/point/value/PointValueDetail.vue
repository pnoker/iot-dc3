<!--
  - Copyright (c) 2022. Pnoker. All Rights Reserved.
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -     http://www.apache.org/licenses/LICENSE-2.0
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->

<template>
    <el-dialog title="数据详情" v-model:visible="detailVisible"
               class="things-dialog"
               :show-close="false"
               :append-to-body="true"
               :model-append-to-body="false"
               :close-on-click-modal="false"
               :close-on-press-escape="false">
        <pre v-highlightjs><code class="json">{{ detailData }}</code></pre>
        <div slot="footer" class="dialog-footer">
            <el-button type="success"  @click="cancel">关闭</el-button>
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
@import "~@/components/dialog/styles/things-dialog.scss";
</style>
