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

import { defineComponent } from 'vue'
import {
    CircleCheck,
    CircleClose,
    CollectionTag,
    Edit,
    IceCreamSquare,
    Right,
    Sunset,
    SwitchButton,
} from '@element-plus/icons-vue'

import router from '@/config/router'

import { successMessage } from '@/util/utils'
import { copyId, timestamp } from '@/util/CommonUtils'

export default defineComponent({
    name: 'ProfileCard',
    components: {
        Edit,
        Sunset,
        IceCreamSquare,
        CollectionTag,
    },
    props: {
        embedded: {
            type: Boolean,
            default: () => false,
        },
        data: {
            type: Object,
            default: () => {
                return {}
            },
        },
        icon: {
            type: String,
            default: () => 'images/common/profile.png',
        },
    },
    emits: ['delete-thing'],
    setup(props, { emit }) {
        // 图标
        const Icon = {
            SwitchButton,
            CircleCheck,
            Right,
            CircleClose,
        }

        const deleteThing = (id) => {
            emit('delete-thing', id, () => {
                successMessage(null)
            })
        }

        const edit = (id) => {
            router.push({ name: 'profileEdit', query: { id, active: '0' } })
        }

        const detail = (id) => {
            router.push({ name: 'profileDetail', query: { id, active: 'detail' } })
        }

        return {
            deleteThing,
            edit,
            detail,
            copyId,
            timestamp,
            ...Icon,
        }
    },
})
