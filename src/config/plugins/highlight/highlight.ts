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

import hljs from 'highlight.js'
import 'highlight.js/styles/atom-one-dark.css'

export default (app: any) => {
    app.directive('highlight', {
        // Directive has a set of lifecycle hooks:
        // called before bound element's parent component is mounted
        beforeMount(el: any) {
            // on first bind, highlight all targets
            const blocks = el.querySelectorAll('pre code')
            for (let i = 0; i < blocks.length; i++) {
                const item = blocks[i]
                console.log(item)
                hljs.highlightBlock(item)
            }
        },
        // called after the containing component's VNode and the VNodes of its children // have updated
        updated(el: any, binding: any) {
            // after an update, re-fill the content and then highlight
            const targets = el.querySelectorAll('code')
            for (let i = 0; i < targets.length; i += 1) {
                const target = targets[i]
                if (typeof binding.value === 'string') {
                    target.textContent = binding.value
                }
                hljs.highlightBlock(target)
            }
        },
    })
}
