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

import { defineComponent } from "vue";
import { Bottom, CircleCheck, CircleClose, Edit, List, Location, Sunset, SwitchButton, Top } from "@element-plus/icons-vue"

import router from "@/config/router";

import { successMessage } from "@/util/utils"
import { copyId, timestamp } from "@/util/CommonUtils"

export default defineComponent({
    name: "PointCard",
    components: {
        Edit,
        Sunset,
        List,
        Location,
        Bottom,
        Top
    },
    props: {
        embedded: {
            type: Boolean,
            default: () => {
                return false;
            }
        },
        data: {
            type: Object,
            default: () => {
                return {
                    name: "",
                    enable: "",
                    description: "",
                    createTime: "",
                    updateTime: ""
                };
            }
        },
        profile: {
            type: Object,
            default: () => {
                return {};
            }
        },
        icon: {
            type: String,
            default: "images/common/point.png"
        }
    },
    emits: ["delete-thing"],
    setup(props, { emit }) {
        // 图标
        const Icon = {
            SwitchButton,
            CircleCheck,
            CircleClose
        }

        const type = (type) => {
            if (type === "int") {
                return "整数";
            } else if (type === "double") {
                return "双精度浮点数";
            } else if (type === "float") {
                return "浮点数";
            } else if (type === "long") {
                return "长整型";
            } else if (type === "boolean") {
                return "布尔量";
            } else if (type === "short") {
                return "短整型";
            } else if (type === "byte") {
                return "字节";
            }
        }

        const rw = (rw) => {
            if (rw === 0) {
                return "只读";
            } else if (rw === 1) {
                return "只写";
            } else if (rw === 2) {
                return "读写";
            }
        }

        const deleteThing = (id) => {
            emit("delete-thing", id, () => {
                successMessage(null)
            })
        }

        const edit = (id) => {
            router.push({ name: "pointEdit", query: { id, profileId: props.data.profileId, active: "0" } })
                .catch(() => {
                    // nothing to do
                })
        }

        const detail = (id) => {
            router.push({ name: "pointDetail", query: { id, active: "detail" } })
                .catch(() => {
                    // nothing to do
                })
        }

        return {
            type,
            rw,
            deleteThing,
            edit,
            detail,
            copyId,
            timestamp,
            ...Icon
        }
    }
})