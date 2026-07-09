<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  -
  - This program is free software: you can redistribute it and/or modify
  - it under the terms of the GNU Affero General Public License as
  - published by the Free Software Foundation, either version 3 of the
  - License, or (at your option) any later version.
  -
  - This program is distributed in the hope that it will be useful,
  - but WITHOUT ANY WARRANTY; without even the implied warranty of
  - MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  - GNU Affero General Public License for more details.
  -
  - You should have received a copy of the GNU Affero General Public License
  - along with this program.  If not, see <https://www.gnu.org/licenses/>.
  -->
<script lang="ts" setup>
import {computed, onMounted, onUnmounted, ref, watch} from 'vue'
import {useData} from 'vitepress'

const LABELS = {
  zh: {zoom: '放大查看', close: '关闭'},
  en: {zoom: 'Zoom', close: 'Close'}
} as const

// 全站 locale 由 VitePress 注入（zh-CN / en-US），这里归一化为 zh/en
const {lang} = useData()
const t = computed(() => LABELS[(lang.value || '').startsWith('en') ? 'en' : 'zh'])

const open = ref(false)

function show() {
  open.value = true
}

function hide() {
  open.value = false
}

function onKey(e: KeyboardEvent) {
  if (e.key === 'Escape' && open.value) hide()
}

// 全屏时锁定背景滚动；SSR 阶段无 document
watch(open, (v) => {
  if (typeof document === 'undefined') return
  document.body.style.overflow = v ? 'hidden' : ''
})

onMounted(() => window.addEventListener('keydown', onKey))
onUnmounted(() => {
  window.removeEventListener('keydown', onKey)
  if (typeof document !== 'undefined') document.body.style.overflow = ''
})
</script>

<template>
  <!-- open 时 Teleport 到 body 规避 VitePress 祖先 transform 破坏 fixed；关闭时 :disabled 原地内联渲染，同一份 DOM 切换 -->
  <Teleport :disabled="!open" to="body">
    <div
        :aria-label="open ? t.close : undefined"
        :aria-modal="open ? 'true' : undefined"
        :class="{'is-open': open}"
        :role="open ? 'dialog' : undefined"
        class="dc3-frame"
        @click.self="open && hide()"
    >
      <button
          :aria-label="open ? t.close : t.zoom"
          :title="open ? t.close : t.zoom"
          class="dc3-frame__btn"
          type="button"
          @click.stop="open ? hide() : show()"
      >
        <svg v-if="!open" aria-hidden="true" fill="none" height="16" stroke="currentColor" stroke-linecap="round"
             stroke-linejoin="round" stroke-width="2" viewBox="0 0 24 24" width="16">
          <circle cx="11" cy="11" r="7"/>
          <line x1="21" x2="16.65" y1="21" y2="16.65"/>
        </svg>
        <svg v-else aria-hidden="true" fill="none" height="16" stroke="currentColor" stroke-linecap="round"
             stroke-linejoin="round" stroke-width="2" viewBox="0 0 24 24" width="16">
          <line x1="18" x2="6" y1="6" y2="18"/>
          <line x1="6" x2="18" y1="6" y2="18"/>
        </svg>
      </button>
      <div class="dc3-frame__figure">
        <slot/>
      </div>
    </div>
  </Teleport>
</template>

<style>
/* inline (default): zoom button floats at the top-right of the wrapped diagram */
.dc3-frame {
  position: relative;
  display: block;
}

.dc3-frame__btn {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 2;
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--vp-c-divider);
  border-radius: 8px;
  background: var(--vp-c-bg-soft);
  color: var(--vp-c-text-1);
  cursor: pointer;
  opacity: 0.6;
  transition: opacity 0.15s ease, background 0.15s ease;
}

.dc3-frame:hover .dc3-frame__btn,
.dc3-frame__btn:focus-visible {
  opacity: 1;
}

.dc3-frame__btn:hover {
  background: var(--vp-c-bg);
}

/* touch devices have no hover — keep the button legible */
@media (hover: none) {
  .dc3-frame__btn {
    opacity: 0.85;
  }
}

/* fullscreen overlay — visual aligned with the site medium-zoom (rgba(0,0,0,.78)); figure centered, scrollable when taller than viewport */
.dc3-frame.is-open {
  position: fixed;
  inset: 0;
  z-index: 99;
  overflow: auto;
  padding: 24px;
  text-align: center;
  background: rgba(0, 0, 0, 0.78);
  cursor: zoom-out;
}

.dc3-frame.is-open .dc3-frame__figure {
  display: inline-block;
  position: relative;
  z-index: 1;
  width: min(96vw, 1600px);
  max-width: 96vw;
  text-align: left;
  vertical-align: top;
  cursor: default;
}

.dc3-frame.is-open .dc3-frame__btn {
  position: fixed;
  top: 18px;
  right: 18px;
  opacity: 1;
  color: #fff;
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.25);
}

.dc3-frame.is-open .dc3-frame__btn:hover {
  background: rgba(255, 255, 255, 0.2);
}
</style>
