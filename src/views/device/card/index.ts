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

import { CircleCheck, CircleClose, Coin, Edit, List, Promotion, Sunset, SwitchButton } from '@element-plus/icons-vue'
import { computed, defineComponent } from 'vue'

import router from '@/config/router'

import { copy, timestamp } from '@/utils/CommonUtils'
import { successMessage } from '@/utils/NotificationUtils'

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
        status: {
            type: String,
            default: () => {
                return ''
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
    emits: ['disable-thing', 'enable-thing', 'delete-thing'],
    setup(props, { emit }) {
        // 图标
        const Icon = {
            SwitchButton,
            CircleCheck,
            CircleClose,
        }

        const disableThing = () => {
            emit('disable-thing', props.data.id, props.data.driverId, () => {
                successMessage()
            })
        }

        const enableThing = () => {
            emit('enable-thing', props.data.id, props.data.driverId, () => {
                successMessage()
            })
        }

        const deleteThing = () => {
            emit('delete-thing', props.data.id, () => {
                successMessage()
            })
        }

        const edit = () => {
            router.push({ name: 'deviceEdit', query: { id: props.data.id, active: '0' } }).catch(() => {
                // nothing to do
            })
        }

        const detail = () => {
            router.push({ name: 'deviceDetail', query: { id: props.data.id, active: 'detail' } }).catch(() => {
                // nothing to do
            })
        }

        return {
            disableThing,
            enableThing,
            deleteThing,
            edit,
            detail,
            copyId: copy,
            timestamp,
            ...Icon,
        }
    },
})
