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
import {ref, onMounted, onBeforeUnmount, computed} from 'vue'

interface VersionEntry {
    version: string
    url: string
    latest?: boolean
}

interface VersionsFile {
    latest: string
    versions: VersionEntry[]
}

const STORAGE_KEY = 'dc3-doc-versions'
const STORAGE_TTL = 10 * 60 * 1000 // 10 minutes

const currentVersion = ref('')
const versions = ref<VersionEntry[]>([])
const open = ref(false)
const root = ref<HTMLElement | null>(null)

const hasMultiple = computed(() => versions.value.length > 1)

function readCurrentVersion() {
    const meta = document.querySelector('meta[name="dc3-doc-version"]')
    currentVersion.value = meta?.getAttribute('content') || 'dev'
}

async function loadVersions() {
    // Check localStorage cache
    try {
        const cached = localStorage.getItem(STORAGE_KEY)
        if (cached) {
            const parsed = JSON.parse(cached)
            if (Date.now() - parsed.ts < STORAGE_TTL && parsed.data?.versions?.length) {
                versions.value = parsed.data.versions
                return
            }
        }
    } catch (_) {
        // localStorage unavailable or corrupted
    }

    // Fetch from server
    try {
        const resp = await fetch('/versions.json', {cache: 'no-cache'})
        if (resp.ok) {
            const data: VersionsFile = await resp.json()
            versions.value = data.versions || []
            try {
                localStorage.setItem(STORAGE_KEY, JSON.stringify({ts: Date.now(), data}))
            } catch (_) {
                // localStorage unavailable
            }
        }
    } catch (_) {
        // Network error or file not found — switcher shows badge only
    }
}

function toggle() {
    open.value = !open.value
}

function close() {
    open.value = false
}

function onClickOutside(e: MouseEvent) {
    if (root.value && !root.value.contains(e.target as Node)) {
        close()
    }
}

function onKeydown(e: KeyboardEvent) {
    if (e.key === 'Escape') close()
}

onMounted(() => {
    readCurrentVersion()
    loadVersions()
    document.addEventListener('click', onClickOutside)
    document.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
    document.removeEventListener('click', onClickOutside)
    document.removeEventListener('keydown', onKeydown)
})
</script>

<template>
    <div ref="root" class="dc3-version-switcher" :class="{open}">
        <button
            class="version-trigger"
            :aria-expanded="open"
            aria-haspopup="listbox"
            @click="toggle"
        >
            <svg class="version-icon" viewBox="0 0 24 24" width="14" height="14" aria-hidden="true">
                <path
                    fill="currentColor"
                    d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93C7.05 19.44 4 16.08 4 12c0-.61.08-1.21.21-1.78L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41C17.93 5.78 20 8.65 20 12c0 2.08-.81 3.98-2.1 5.39z"
                />
            </svg>
            <span class="version-label">{{ currentVersion }}</span>
            <svg
                class="caret"
                :class="{rotated: open}"
                viewBox="0 0 24 24"
                width="12"
                height="12"
                aria-hidden="true"
            >
                <path fill="currentColor" d="M7 10l5 5 5-5z"/>
            </svg>
        </button>

        <Transition name="dc3-dropdown">
            <ul v-if="open && hasMultiple" class="version-menu" role="listbox">
                <li v-for="v in versions" :key="v.version" role="option">
                    <a
                        :href="v.url"
                        :class="['version-link', {active: v.version === currentVersion}]"
                        :aria-current="v.version === currentVersion ? 'version' : undefined"
                    >
                        <span class="link-label">{{ v.version }}</span>
                        <span v-if="v.latest" class="latest-badge">Latest</span>
                        <svg
                            v-if="v.version === currentVersion"
                            class="check-icon"
                            viewBox="0 0 24 24"
                            width="14"
                            height="14"
                            aria-hidden="true"
                        >
                            <path fill="currentColor" d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
                        </svg>
                    </a>
                </li>
            </ul>
        </Transition>
    </div>
</template>

<style scoped>
.dc3-version-switcher {
    position: relative;
    display: inline-flex;
    align-items: center;
}

.version-trigger {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 0 8px;
    height: 32px;
    border: 1px solid var(--vp-c-divider);
    border-radius: 8px;
    background: var(--vp-c-bg-soft);
    color: var(--vp-c-text-2);
    font-size: 13px;
    font-weight: 500;
    line-height: 1;
    cursor: pointer;
    transition: border-color 0.2s, color 0.2s, background 0.2s;
}

.version-trigger:hover {
    border-color: var(--vp-c-brand-2);
    color: var(--vp-c-brand-2);
}

.dc3-version-switcher.open .version-trigger {
    border-color: var(--vp-c-brand-2);
    color: var(--vp-c-brand-1);
}

.version-icon {
    opacity: 0.7;
}

.version-label {
    font-variant-numeric: tabular-nums;
    letter-spacing: 0.01em;
}

.caret {
    opacity: 0.5;
    transition: transform 0.2s ease;
}

.caret.rotated {
    transform: rotate(180deg);
}

.version-menu {
    position: absolute;
    top: calc(100% + 6px);
    right: 0;
    z-index: 100;
    min-width: 160px;
    padding: 6px;
    margin: 0;
    list-style: none;
    background: var(--vp-c-bg-elv);
    border: 1px solid var(--vp-c-divider);
    border-radius: 10px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.version-link {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 7px 10px;
    border-radius: 6px;
    font-size: 13px;
    font-weight: 500;
    color: var(--vp-c-text-2);
    text-decoration: none;
    transition: background 0.15s, color 0.15s;
}

.version-link:hover {
    background: var(--vp-c-brand-soft);
    color: var(--vp-c-brand-1);
}

.version-link.active {
    color: var(--vp-c-brand-1);
    background: var(--vp-c-brand-soft);
}

.link-label {
    flex: 1;
    font-variant-numeric: tabular-nums;
}

.latest-badge {
    padding: 1px 6px;
    border-radius: 4px;
    font-size: 10px;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: #fff;
    background: var(--vp-c-brand-2);
}

.check-icon {
    color: var(--vp-c-brand-2);
    flex-shrink: 0;
}

/* Dropdown transition */
.dc3-dropdown-enter-active,
.dc3-dropdown-leave-active {
    transition: opacity 0.15s ease, transform 0.15s ease;
}

.dc3-dropdown-enter-from,
.dc3-dropdown-leave-to {
    opacity: 0;
    transform: translateY(-4px);
}
</style>
