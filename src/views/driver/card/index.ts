/*
 * Copyright 2022 Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { computed, defineComponent } from 'vue'
import {
    CircleCheck,
    CircleClose,
    Connection,
    Edit,
    Monitor,
    Promotion,
    Sunset,
    SwitchButton,
} from '@element-plus/icons-vue'

import router from '@/config/router'
import { copyId, timestamp } from '@/util/CommonUtils'

export default defineComponent({
    name: 'DriverCard',
    components: {
        Promotion,
        Edit,
        Sunset,
        Connection,
        Monitor,
    },
    props: {
        icon: {
            type: String,
            default: 'images/common/driver.png',
        },
        statusTable: {
            type: Object,
            default: () => {
                return {}
            },
        },
        data: {
            type: Object,
            default: () => {
                return {}
            },
        },
        footer: {
            type: Boolean,
            default: () => false,
        },
    },
    emits: ['select-change'],
    setup(props, { emit }) {
        // 图标
        const Icon = {
            SwitchButton,
            CircleCheck,
            CircleClose,
        }

        // 驱动详情
        const detail = () => {
            const id = props.data.id
            if (id) router.push({ name: 'driverDetail', query: { id, active: 'detail' } })
        }

        // 选中驱动
        const select = () => {
            emit('select-change', props.data)
        }

        // 驱动状态
        const status = computed(() => {
            const id = props.data.id
            return id && props.statusTable[id] ? props.statusTable[id] : ''
        })

        return {
            status,
            detail,
            select,
            copyId,
            timestamp,
            ...Icon,
        }
    },
})
