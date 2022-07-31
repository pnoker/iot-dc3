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

import { defineComponent } from 'vue'
import { CircleCheck, CircleClose, Coin, Edit, List, Promotion, Sunset, SwitchButton } from '@element-plus/icons-vue'

import router from '@/config/router'

import { successMessage } from '@/util/utils'
import { copyId, timestamp } from '@/util/CommonUtils'

export default defineComponent({
    name: 'DeviceCard',
    components: {
        Promotion,
        List,
        Coin,
        Edit,
        Sunset,
    },
    props: {
        embedded: {
            type: Boolean,
            default: () => {
                return false
            },
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
        driver: {
            type: Object,
            default: () => {
                return {}
            },
        },
        icon: {
            type: String,
            default: 'images/common/device.png',
        },
    },
    emits: ['delete-thing'],
    setup(props, { emit }) {
        // 图标
        const Icon = {
            SwitchButton,
            CircleCheck,
            CircleClose,
        }

        const status = (id) => {
            return props.statusTable[id]
        }

        const deleteThing = (id) => {
            emit('delete-thing', id, () => {
                successMessage(null)
            })
        }

        const edit = (id, driverId) => {
            router.push({ name: 'deviceEdit', query: { id, driverId, active: '0' } }).catch(() => {
                // nothing to do
            })
        }

        const detail = (id, driverId) => {
            router.push({ name: 'deviceDetail', query: { id, driverId, active: 'detail' } }).catch(() => {
                // nothing to do
            })
        }

        return {
            status,
            deleteThing,
            edit,
            detail,
            copyId,
            timestamp,
            ...Icon,
        }
    },
})
