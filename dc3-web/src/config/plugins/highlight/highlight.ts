/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
