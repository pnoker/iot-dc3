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

import type {App, DirectiveBinding} from 'vue';
import hljs from 'highlight.js';
import 'highlight.js/styles/atom-one-dark.css';

/**
 * Highlight.js plugin configuration
 * Provides syntax highlighting for code blocks using highlight.js
 *
 * @param app Vue application instance
 */
export default function setupHighlight(app: App): void {
  app.directive('highlight', {
    /**
     * Called before bound element's parent component is mounted
     * Highlights all code blocks within the element
     *
     * @param el The element the directive is bound to
     */
    beforeMount(el: HTMLElement) {
      const blocks = el.querySelectorAll('pre code');
      blocks.forEach((block) => {
        if (block instanceof HTMLElement) {
          hljs.highlightBlock(block);
        }
      });
    },

    /**
     * Called after containing component's VNode and its children's VNodes have updated
     * Updates code content and re-highlights
     *
     * @param el The element the directive is bound to
     * @param binding The directive binding object
     */
    updated(el: HTMLElement, binding: DirectiveBinding) {
      const targets = el.querySelectorAll('code');
      targets.forEach((target) => {
        if (target instanceof HTMLElement) {
          if (typeof binding.value === 'string') {
            target.textContent = binding.value;
          }
          hljs.highlightBlock(target);
        }
      });
    },
  });
}
