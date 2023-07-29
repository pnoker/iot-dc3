/*
 * Copyright 2016-present the original author or authors.
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

import moment from 'moment'
import 'moment/locale/zh-cn'

moment.locale('zh-cn')

export const log = {
    info(message: string) {
        const now = moment().format('YYYY-MM-DD HH:mm:ss.SSS')
        console.log(now, '%cINFO%c  ---  :', 'color:#F00', message)
    },
    warn(message: string) {
        const now = moment().format('YYYY-MM-DD HH:mm:ss.SSS')
        console.log(now, '%cWARN%c  ---  :', 'color:#F00', message)
    },
    error(message: string) {
        const now = moment().format('YYYY-MM-DD HH:mm:ss.SSS')
        console.log(now, '%cERROR%c  ---  :', 'color:#F00', message)
    },
}
