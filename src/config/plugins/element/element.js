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

import { Hide, Histogram, List, Management, Promotion, View } from "@element-plus/icons-vue"
import element from "element-plus"
import "element-plus/dist/index.css"
import './element-variables.less'
import locale from "element-plus/es/locale/lang/zh-cn"

export default (app) => {
    const Icons = [
        Hide,
        Histogram,
        List,
        Management,
        Promotion,
        View
    ]
    Icons.forEach(icon => {
        app.component(icon.name, icon)
    })
    app.use(element, { locale })
}
