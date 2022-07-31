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

import { defineComponent, computed } from 'vue'
import menu from '@/config/router/views'
import { HomeFilled } from '@element-plus/icons-vue'

import { useStore } from 'vuex'
import router from '@/config/router'
import { warning } from '@/util/MessageUtils'

export default defineComponent({
    components: {
        HomeFilled,
    },
    setup() {
        const store = useStore()

        const menus = computed(() => {
            const children = menu?.children || []
            return children.filter((view) => view.name !== 'home')
        })

        const handleMenuEnter = (index) => {
            if (index.indexOf('/') === 0) {
                const split = index.split('/')
                if (split.length > 2) {
                    return '/' + split[1]
                }
            }
            return index
        }

        const handleMessage = () => {
            warning('待开发')
        }

        const handleCommand = (command) => {
            if (command === 'logout') {
                store.dispatch('auth/logout').then(() => router.push({ path: '/login' }))
            } else if (command === 'help') {
                window.open('https://doc.dc3.site')
            }
        }

        return {
            menus,
            handleMenuEnter,
            handleMessage,
            handleCommand,
        }
    },
})
