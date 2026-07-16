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

import {defineConfig} from 'vitepress'
import {Lang, t} from './i18n'

// ── i18n via locales/{lang}.json ──
// All user-facing strings are resolved through t(lang, key).
// Adding a language: create locales/<lang>.json, add to Lang type and VitePress locales block.
// Entry: [code] — code doubles as the i18n key. Code = language-relative path; single segment → directory index.
type Entry = readonly [string]
type Group = { key?: string; items: ReadonlyArray<Entry> }   // key omitted → pinned title-less group
type Pillar = {
    navKey: string
    landing: string
    paths: ReadonlyArray<string>
    activeMatch?: string
    groups: ReadonlyArray<Group>
}

// Pillar order: 总览 → 架构 → 驱动 → 基础 → 开发 → 运维 (after nav 首页)
const PILLARS: ReadonlyArray<Pillar> = [
    {   // ①
        navKey: 'pillar.overview', landing: 'introduction',
        paths: ['introduction', 'quickstart'], activeMatch: '^/(zh|en)/(introduction|quickstart)/',
        groups: [
            {
                key: '',
                items: [['introduction'], ['introduction/concepts'], ['introduction/paths'], ['introduction/technology-stack']]
            },
            {
                key: 'group.objects-data',
                items: [['introduction/concepts/profile'], ['introduction/concepts/device'], ['introduction/concepts/driver'], ['introduction/concepts/point'], ['introduction/concepts/point-value']]
            },
            {
                key: 'group.capabilities-boundaries',
                items: [['introduction/concepts/command'], ['introduction/concepts/event'], ['introduction/concepts/attribute-config'], ['introduction/concepts/tenant']]
            },
            {key: 'group.quickstart', items: [['quickstart'], ['quickstart/environment'], ['quickstart/first-device']]},
            {key: 'group.appendix', items: [['introduction/glossary'], ['introduction/license']]}
        ]
    },
    {   // ②
        navKey: 'pillar.architecture', landing: 'architecture',
        paths: ['architecture', 'modules'], activeMatch: '^/(zh|en)/(architecture|modules)/',
        groups: [
            {key: '', items: [['architecture']]},
            {key: 'group.services-collab', items: [['architecture/services'], ['architecture/facade-modes']]},
            {
                key: 'group.pipelines-model',
                items: [['architecture/data-plane'], ['architecture/command-plane'], ['architecture/auth-rbac'], ['architecture/domain-model']]
            },
            {key: 'group.modules', items: [['architecture/modules'], ['modules']]}
        ]
    },
    {   // ③
        navKey: 'pillar.drivers', landing: 'drivers',
        paths: ['drivers', 'operation/device-onboarding'],
        activeMatch: '^/(zh|en)/(drivers/|operation/device-onboarding)',
        groups: [
            {key: 'group.onboarding', items: [['drivers'], ['operation/device-onboarding']]},
            {
                key: 'group.industrial-bus',
                items: [['drivers/modbus-tcp'], ['drivers/modbus-rtu'], ['drivers/opc-ua'], ['drivers/opc-da'], ['drivers/plcs7'], ['drivers/melsec'], ['drivers/fins'], ['drivers/ethernet-ip']]
            },
            {
                key: 'group.scada-power',
                items: [['drivers/bacnet-ip'], ['drivers/iec104'], ['drivers/dlms'], ['drivers/sl651'], ['drivers/snmp']]
            },
            {
                key: 'group.iot-wireless',
                items: [['drivers/mqtt'], ['drivers/coap'], ['drivers/lwm2m'], ['drivers/http'], ['drivers/ble'], ['drivers/zigbee'], ['drivers/can']]
            },
            {key: 'group.serial-network', items: [['drivers/serial'], ['drivers/tcp-udp']]},
            {
                key: 'group.database',
                items: [['drivers/mysql'], ['drivers/postgresql'], ['drivers/oracle'], ['drivers/sqlserver']]
            },
            {key: 'group.virtual-test', items: [['drivers/virtual'], ['drivers/listening-virtual']]},
            {key: 'group.appendix-drivers', items: [['drivers/matrix']]}
        ]
    },
    {   // ④
        navKey: 'pillar.foundations', landing: 'foundations',
        paths: ['foundations'], activeMatch: '^/(zh|en)/foundations/',
        groups: [
            {key: '', items: [['foundations']]},
            {key: 'group.perception', items: [['foundations/sensing'], ['foundations/identification']]},
            {key: 'group.network', items: [['foundations/fieldbus'], ['foundations/iot-protocols']]},
            {key: 'group.platform', items: [['foundations/edge-cloud'], ['foundations/data-pipeline']]},
            {key: 'group.application', items: [['foundations/aiot']]},
            {key: 'group.security', items: [['foundations/security']]}
        ]
    },
    {   // ⑤
        navKey: 'pillar.develop',
        landing: 'development',
        paths: ['development', 'frontend', 'ai', 'automation'],
        activeMatch: '^/(zh|en)/(development|frontend|ai|automation)/',
        groups: [
            {
                key: 'group.development',
                items: [['development'], ['development/driver-authoring'], ['development/api-documentation'], ['development/testing'], ['development/changelog']]
            },
            {key: 'group.ai-integration', items: [['ai'], ['ai/agentic'], ['ai/mcp']]},
            {key: 'group.frontend', items: [['frontend'], ['frontend/test-debugging']]},
            {key: 'group.automation', items: [['automation/cli']]}
        ]
    },
    {   // ⑥
        navKey: 'pillar.operations', landing: 'operation',
        paths: ['operation', 'guide'], activeMatch: '^/(zh|en)/(operation|guide)/',
        groups: [
            {key: 'group.operations', items: [['operation'], ['operation/data-commands'], ['operation/alarms']]},
            {
                key: 'group.deploy-ops',
                items: [['guide/usage'], ['guide/observability'], ['guide/logging'], ['guide/troubleshooting']]
            }
        ]
    }
]

const COMMUNITY: ReadonlyArray<Entry> = [
    ['community/contributing'],
    ['community/code-of-conduct'],
    ['community/security'],
    ['community/faq']
]

type SidebarItem = { text: string; link: string }
type SidebarGroup = { text: string; collapsed?: boolean; items: SidebarItem[] }

const linkOf = (lang: Lang, code: string) => {
    const p = lang === 'en' ? '/en' : '/zh'
    return code.includes('/') ? `${p}/${code}` : `${p}/${code}/`
}

const itemsOf = (lang: Lang, entries: ReadonlyArray<Entry>): SidebarItem[] =>
    entries.map(([code]) => ({text: t(lang, code), link: linkOf(lang, code)}))

function buildSidebar(lang: Lang) {
    const p = lang === 'en' ? '/en' : '/zh'
    const sidebar: Record<string, SidebarGroup[]> = {}
    for (const pillar of PILLARS) {
        const groups: SidebarGroup[] = pillar.groups.map(g => {
            const title = g.key ? t(lang, g.key) : ''
            const items = itemsOf(lang, g.items)
            return title ? {text: title, collapsed: false, items} : {text: '', items}
        })
        for (const path of pillar.paths) {
            const k = path.includes('/') ? `${p}/${path}` : `${p}/${path}/`
            sidebar[k] = groups
        }
    }
    sidebar[`${p}/community/`] = [{text: t(lang, 'community'), items: itemsOf(lang, COMMUNITY)}]
    return sidebar
}

function buildNav(lang: Lang) {
    const p = lang === 'en' ? '/en' : '/zh'
    const pillars = PILLARS.map(pillar => ({
        text: t(lang, pillar.navKey),
        link: linkOf(lang, pillar.landing),
        ...(pillar.activeMatch ? {activeMatch: pillar.activeMatch} : {})
    }))
    return [
        {text: lang === 'en' ? 'Home' : '首页', link: `${p}/`},
        ...pillars,
        {text: t(lang, 'community'), items: itemsOf(lang, COMMUNITY)}
    ]
}

function uiLabels(lang: Lang) {
    return {
        editLinkText: t(lang, 'ui.editLinkText'),
        outlineLabel: t(lang, 'ui.outlineLabel'),
        lastUpdatedText: t(lang, 'ui.lastUpdatedText'),
        returnToTop: t(lang, 'ui.returnToTop'),
        sidebarMenu: t(lang, 'ui.sidebarMenu'),
        darkModeSwitch: t(lang, 'ui.darkModeSwitch'),
        lightModeSwitchTitle: t(lang, 'ui.lightModeSwitchTitle'),
        darkModeSwitchTitle: t(lang, 'ui.darkModeSwitchTitle'),
        docFooterPrev: t(lang, 'ui.docFooterPrev'),
        docFooterNext: t(lang, 'ui.docFooterNext'),
        searchButtonText: t(lang, 'ui.searchButtonText'),
        searchButtonAria: t(lang, 'ui.searchButtonAria'),
        searchNoResults: t(lang, 'ui.searchNoResults'),
        searchReset: t(lang, 'ui.searchReset'),
        searchSelect: t(lang, 'ui.searchSelect'),
        searchNavigate: t(lang, 'ui.searchNavigate'),
        searchClose: t(lang, 'ui.searchClose'),
        footerMessage: t(lang, 'ui.footerMessage')
    }
}

function localeThemeConfig(lang: Lang) {
    const u = uiLabels(lang)
    return {
        nav: buildNav(lang),
        sidebar: buildSidebar(lang),
        editLink: {text: u.editLinkText},
        footer: {message: u.footerMessage, copyright: 'Copyright © 2017-2026 pnoker'},
        outline: {level: [2, 3], label: u.outlineLabel},
        lastUpdated: {
            text: u.lastUpdatedText,
            formatOptions: {dateStyle: 'short', timeStyle: 'medium'}
        },
        docFooter: {prev: u.docFooterPrev, next: u.docFooterNext},
        returnToTopLabel: u.returnToTop,
        sidebarMenuLabel: u.sidebarMenu,
        darkModeSwitchLabel: u.darkModeSwitch,
        lightModeSwitchTitle: u.lightModeSwitchTitle,
        darkModeSwitchTitle: u.darkModeSwitchTitle,
        search: {
            provider: 'local',
            options: {
                translations: {
                    button: {
                        buttonText: u.searchButtonText,
                        buttonAriaLabel: u.searchButtonAria
                    },
                    modal: {
                        noResultsText: u.searchNoResults,
                        resetButtonTitle: u.searchReset,
                        footer: {
                            selectText: u.searchSelect,
                            navigateText: u.searchNavigate,
                            closeText: u.searchClose
                        }
                    }
                }
            }
        }
    }
}

// The root path (language gate) always redirects to /zh/ or /en/ by preference: localStorage first, otherwise browser language; the language-selection page is only a fallback for when JS is disabled
const LANG_DETECT = `(function(){var K='dc3-lang',B='/';var p;try{p=localStorage.getItem(K);}catch(e){}var r=location.pathname;if(r.length&&r[r.length-1]!=='/')r=r+'/';var rel=r.indexOf(B)===0?r.slice(B.length):r;var onEn=rel.indexOf('en/')===0;var onZh=rel.indexOf('zh/')===0;if(!onEn&&!onZh){if(!p){p=/^en/i.test(navigator.language)?'en':'zh';try{localStorage.setItem(K,p);}catch(e){}}location.replace(B+p+'/');}})();`

export default defineConfig({
    base: '/',
    lang: 'zh-CN',
    title: 'IoT DC3',
    cleanUrls: true,
    lastUpdated: true,

    // maintainer-internal material is excluded from the public site build
    srcExclude: ['superpowers/**'],

    head: [
        ['link', {rel: 'icon', href: '/images/logo.svg', type: 'image/svg+xml'}],
        ['meta', {name: 'theme-color', content: '#1296db'}],
        // Google Analytics (GA4)
        ['script', {async: '', src: 'https://www.googletagmanager.com/gtag/js?id=G-0S37KX68Y3'}],
        ['script', {}, `window.dataLayer = window.dataLayer || [];
function gtag(){dataLayer.push(arguments);}
gtag('js', new Date());
gtag('config', 'G-0S37KX68Y3');`],
        // Baidu Tongji (百度统计)
        ['script', {}, `var _hmt = _hmt || [];
(function() {
var hm = document.createElement("script");
hm.src = "https://hm.baidu.com/hm.js?bd41b7a1501a288245f375eaef8f97cc";
var s = document.getElementsByTagName("script")[0];
s.parentNode.insertBefore(hm, s);
})();`],
        ['script', {}, LANG_DETECT]
    ],

    locales: {
        zh: {
            label: '简体中文',
            lang: 'zh-CN',
            themeConfig: localeThemeConfig('zh')
        },
        en: {
            label: 'English',
            lang: 'en-US',
            themeConfig: localeThemeConfig('en')
        }
    },

    themeConfig: {
        logo: '/images/logo.svg',
        siteTitle: 'IoT DC3',

        socialLinks: [
            {icon: 'github', link: 'https://github.com/pnoker/iot-dc3'},
            {icon: 'gitee', link: 'https://gitee.com/pnoker/iot-dc3'},
            {icon: 'x', link: 'https://x.com/IoTDC3'}
        ],

        editLink: {
            pattern: 'https://github.com/pnoker/iot-dc3/edit/release/docs/:path'
        }
    },

    markdown: {
        lineNumbers: false,
        image: {
            lazyLoading: true
        }
    }
})
