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
import {ref, onMounted, computed} from 'vue'
import {useData} from 'vitepress'

const {lang} = useData()

const isLatest = ref(true)
const currentVersion = ref('')
const latestUrl = ref('')

const isZh = computed(() => lang.value.startsWith('zh'))

onMounted(() => {
    const metaLatest = document.querySelector('meta[name="dc3-doc-is-latest"]')
    isLatest.value = metaLatest?.getAttribute('content') !== 'false'

    const metaVersion = document.querySelector('meta[name="dc3-doc-version"]')
    currentVersion.value = metaVersion?.getAttribute('content') || ''

    const metaLatestUrl = document.querySelector('meta[name="dc3-doc-latest-url"]')
    latestUrl.value = metaLatestUrl?.getAttribute('content') || '/'
})
</script>

<template>
    <div v-if="!isLatest" class="dc3-version-banner">
        <svg class="banner-icon" viewBox="0 0 24 24" width="16" height="16" aria-hidden="true">
            <path
                fill="currentColor"
                d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"
            />
        </svg>
        <span class="banner-text">
            <template v-if="isZh">
                你正在查看 <strong>{{ currentVersion }}</strong> 的文档（已归档）。查看
                <a :href="latestUrl">最新版本</a>。
            </template>
            <template v-else>
                You are viewing archived documentation for <strong>{{ currentVersion }}</strong>.
                View the <a :href="latestUrl">latest version</a>.
            </template>
        </span>
    </div>
</template>

<style scoped>
.dc3-version-banner {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 7px 16px;
    font-size: 13px;
    line-height: 1.4;
    color: #fff;
    background: var(--vp-c-brand-1);
}

.banner-icon {
    flex-shrink: 0;
    opacity: 0.85;
}

.banner-text strong {
    font-weight: 700;
}

.banner-text a {
    color: #fff;
    font-weight: 600;
    text-decoration: underline;
    text-underline-offset: 2px;
}

.banner-text a:hover {
    opacity: 0.85;
}
</style>
