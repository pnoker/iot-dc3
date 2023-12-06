/*
 * Copyright 2016-present the IoT DC3 original author or authors.
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
import { Goblet } from '@element-plus/icons-vue'

import { Attribute } from '@/config/types'

export default defineComponent({
    name: 'PointInfoCard',
    components: {
        Goblet,
    },
    props: {
        data: {
            type: Object,
            default: () => {
                return {
                    attributeName: '',
                    defaultValue: '',
                    remark: '',
                    createTime: '',
                    operateTime: '',
                }
            },
        },
        attributes: {
            type: Array<Attribute>,
            default: () => {
                return []
            },
        },
        icon: {
            type: String,
            default: 'images/common/point-info-disable.png',
        },
    },
    emits: ['select'],
    setup(props, { emit }) {
        const isConfig = computed(() => {
            for (let i = 0; i < props.attributes.length; i++) {
                const attribute = props.attributes[i] as any
                if (props.data[attribute.attributeName].configValue === '') {
                    return false
                }
            }

            return true
        })

        const isSelect = computed(() => {
            if (props.data.shadow === 'always') {
                return 'images/common/point-info.png'
            } else {
                return 'images/common/point-info-disable.png'
            }
        })

        const select = (data) => {
            emit('select', data)
        }

        return {
            isConfig,
            isSelect,
            select,
        }
    },
})
