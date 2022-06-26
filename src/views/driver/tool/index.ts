/*
 * Copyright (c) 2022. Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { defineComponent, reactive, ref } from "vue"
import { FormInstance, FormRules } from "element-plus"
import { Refresh, RefreshRight, Search, Sort } from "@element-plus/icons-vue"

import { useStore } from "vuex"

export default defineComponent({
    name: "DriverTool",
    props: {
        page: {
            type: Object,
            default: () => {
                return {}
            }
        },
        add: {
            type: Boolean,
            default: () => false
        }
    },
    setup(props, context) {
        const store = useStore()
        const formDataRef = ref<FormInstance>()

        // 图标
        const Icon = {
            Search,
            RefreshRight,
            Refresh,
            Sort,
        }

        // 定义响应式数据
        let formData = reactive({})

        // 定义Form规则
        let formRule = reactive<FormRules>({
            port: [
                {type: "number", message: "端口必须为数字值"}
            ]
        })

        const search = (formInstance: FormInstance) => {
            formInstance.validate((valid) => {
                if (valid) {
                    context.emit("search", formData)
                }
            });
        }

        const reset = (formInstance: FormInstance) => {
            formInstance.resetFields();
            context.emit("reset");
        }

        const refresh = () => {
            context.emit("refresh");
        }

        const sort = () => {
            context.emit("sort");
        }

        const sizeChange = (size) => {
            context.emit("size-change", size);
        }

        const currentChange = (current) => {
            context.emit("current-change", current);
        }

        return {
            store,
            formDataRef,
            formData,
            formRule,
            search,
            reset,
            refresh,
            sort,
            sizeChange,
            currentChange,
            ...Icon
        }
    }
})