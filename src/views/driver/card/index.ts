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

import router from "@/config/router"

import { dateFormat, setCopyContent } from "@/util/utils"

export default {
    name: "DriverCard",
    props: {
        icon: {
            type: String,
            default: "images/common/driver.png"
        },
        statusTable: {
            type: Object,
            default: () => {
                return {}
            }
        },
        data: {
            type: Object,
            default: () => {
                return {
                    name: "",
                    serviceName: "",
                    host: "",
                    port: "",
                    enable: "",
                    description: "",
                    createTime: "",
                    updateTime: ""
                }
            }
        },
        footer: {
            type: Boolean,
            default: () => false
        }
    },
    setup(props, context) {
        // 驱动状态
        const status = (id) => props.statusTable[id]

        // 驱动详情
        const detail = (id) => {
            router.push({name: "driverDetail", query: {id, active: "detail"}})
        }

        // 选中驱动
        const select = (data) => {
            context.emit("select-change", data)
        }

        // 复制ID
        const copyId = (content) => {
            setCopyContent(content, true, "驱动ID")
        }

        // 格式化时间
        const timestamp = (timestamp) => {
            return dateFormat(new Date(timestamp))
        }

        return {
            status,
            detail,
            select,
            copyId,
            timestamp
        }
    }
}
;