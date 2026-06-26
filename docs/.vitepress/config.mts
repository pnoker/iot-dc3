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
import {withMermaid} from 'vitepress-plugin-mermaid'

// ── Site information architecture: pillar model (5 pillars + community; the "Foundations" pillar will be inserted at slot ② in P1) ──
// A pillar may own multiple legacy directory prefixes (paths) and share a single sidebar; Entry: [code, zh, English].
// code = language-relative path: a single segment is treated as a directory index (→ '/<lang>/<code>/'), multiple segments as a specific page (→ '/<lang>/<code>').
type Entry = readonly [string, string, string]
type Group = { zh: string; en: string; items: ReadonlyArray<Entry> }
type Pillar = {
    navZh: string
    navEn: string
    landing: string                       // landing page code for the nav-bar link
    paths: ReadonlyArray<string>          // sidebar path prefixes owned by this pillar (multiple paths share one sidebar)
    activeMatch?: string                  // nav highlight regex for cross-directory pillars (matches the locale-prefixed path)
    groups: ReadonlyArray<Group>          // sidebar groups; an empty group title → pinned, title-less single column
}

const PILLARS: ReadonlyArray<Pillar> = [
    {
        navZh: '总览', navEn: 'Overview', landing: 'introduction',
        paths: ['introduction', 'quickstart'], activeMatch: '^/(zh|en)/(introduction|quickstart)/',
        groups: [
            {
                zh: '', en: '', items: [
                    ['introduction', '总览', 'Overview'],
                    ['introduction/concepts', '核心概念', 'Core Concepts'],
                    ['introduction/paths', '按角色选择路径', 'Choose Your Path']
                ]
            },
            {
                zh: '对象与数据', en: 'Objects & Data', items: [
                    ['introduction/concepts/profile', '物模型', 'Profile'],
                    ['introduction/concepts/device', '设备', 'Device'],
                    ['introduction/concepts/driver', '驱动', 'Driver'],
                    ['introduction/concepts/point', '位号', 'Point'],
                    ['introduction/concepts/point-value', '位号值', 'Point Value']
                ]
            },
            {
                zh: '能力与边界', en: 'Capabilities & Boundaries', items: [
                    ['introduction/concepts/command', '指令', 'Command'],
                    ['introduction/concepts/event', '事件', 'Event'],
                    ['introduction/concepts/attribute-config', '属性与配置', 'Attribute & Config'],
                    ['introduction/concepts/tenant', '租户', 'Tenant']
                ]
            },
            {
                zh: '快速开始', en: 'Quick Start', items: [
                    ['quickstart', '本地开发', 'Local Development'],
                    ['quickstart/environment', '环境变量', 'Environment Variables'],
                    ['quickstart/first-device', '第一个设备', 'First Device']
                ]
            },
            {
                zh: '附录', en: 'Appendix', items: [
                    ['introduction/glossary', '术语表', 'Glossary']
                ]
            }
        ]
    },

    {
        navZh: '基础', navEn: 'Foundations', landing: 'foundations',
        paths: ['foundations'], activeMatch: '^/(zh|en)/foundations/',
        groups: [
            {
                zh: '', en: '', items: [
                    ['foundations', '物联网技术总览', 'IoT Technology Overview']
                ]
            },
            {
                zh: '感知层', en: 'Perception', items: [
                    ['foundations/sensing', '传感与测量', 'Sensing & Measurement'],
                    ['foundations/identification', '自动识别与定位', 'Auto-ID & Positioning']
                ]
            },
            {
                zh: '网络层', en: 'Network', items: [
                    ['foundations/fieldbus', '工业总线与协议', 'Industrial Buses & Protocols'],
                    ['foundations/iot-protocols', 'IoT 协议与无线网络', 'IoT Protocols & Wireless']
                ]
            },
            {
                zh: '平台层', en: 'Platform', items: [
                    ['foundations/edge-cloud', '边缘与云架构', 'Edge & Cloud Architecture'],
                    ['foundations/data-pipeline', '时序数据与流处理', 'Time-Series & Streaming']
                ]
            },
            {
                zh: '应用层', en: 'Application', items: [
                    ['foundations/aiot', '数据智能与 AIoT', 'Data Intelligence & AIoT']
                ]
            },
            {
                zh: '安全', en: 'Security', items: [
                    ['foundations/security', '物联网安全', 'IoT Security']
                ]
            }
        ]
    },

    {
        navZh: '架构', navEn: 'Architecture', landing: 'architecture',
        paths: ['architecture', 'modules'], activeMatch: '^/(zh|en)/(architecture|modules)/',
        groups: [
            {
                zh: '', en: '', items: [
                    ['architecture', '总览', 'Overview']
                ]
            },
            {
                zh: '服务与协作', en: 'Services & Collaboration', items: [
                    ['architecture/services', '服务与拓扑', 'Services & Topology'],
                    ['architecture/facade-modes', 'Facade 模式', 'Facade Modes']
                ]
            },
            {
                zh: '链路与模型', en: 'Pipelines & Model', items: [
                    ['architecture/data-plane', '数据平面', 'Data Plane'],
                    ['architecture/command-plane', '命令平面', 'Command Plane'],
                    ['architecture/auth-rbac', '鉴权 · 租户 · RBAC', 'Auth, Tenant & RBAC'],
                    ['architecture/domain-model', '领域模型', 'Domain Model']
                ]
            },
            {
                zh: '模块', en: 'Modules', items: [
                    ['architecture/modules', '模块地图', 'Module Map'],
                    ['modules', '模块清单', 'Catalog']
                ]
            }
        ]
    },

    {
        navZh: '接入', navEn: 'Connectivity', landing: 'drivers',
        paths: ['drivers', 'operation/device-onboarding'],
        activeMatch: '^/(zh|en)/(drivers/|operation/device-onboarding)',
        groups: [
            {
                zh: '接入指南', en: 'Onboarding', items: [
                    ['drivers', '驱动总览', 'Drivers'],
                    ['operation/device-onboarding', '设备接入流程', 'Device Onboarding']
                ]
            },
            {
                zh: '工业总线 / PLC', en: 'Industrial Bus / PLC', items: [
                    ['drivers/modbus-tcp', 'Modbus TCP', 'Modbus TCP'],
                    ['drivers/modbus-rtu', 'Modbus RTU', 'Modbus RTU'],
                    ['drivers/opc-ua', 'OPC UA', 'OPC UA'],
                    ['drivers/opc-da', 'OPC DA', 'OPC DA'],
                    ['drivers/plcs7', 'S7 (Siemens)', 'S7 (Siemens)'],
                    ['drivers/melsec', 'MELSEC', 'MELSEC'],
                    ['drivers/fins', 'FINS (Omron)', 'FINS (Omron)'],
                    ['drivers/ethernet-ip', 'EtherNet/IP', 'EtherNet/IP']
                ]
            },
            {
                zh: 'SCADA / 电力 / 计量', en: 'SCADA / Power / Metering', items: [
                    ['drivers/bacnet-ip', 'BACnet/IP', 'BACnet/IP'],
                    ['drivers/iec104', 'IEC 104', 'IEC 104'],
                    ['drivers/dlms', 'DLMS', 'DLMS'],
                    ['drivers/sl651', 'SL651', 'SL651'],
                    ['drivers/snmp', 'SNMP', 'SNMP']
                ]
            },
            {
                zh: '物联网 / 无线', en: 'IoT / Wireless', items: [
                    ['drivers/mqtt', 'MQTT', 'MQTT'],
                    ['drivers/coap', 'CoAP', 'CoAP'],
                    ['drivers/lwm2m', 'LwM2M', 'LwM2M'],
                    ['drivers/http', 'HTTP', 'HTTP'],
                    ['drivers/ble', 'BLE', 'BLE'],
                    ['drivers/zigbee', 'Zigbee', 'Zigbee'],
                    ['drivers/can', 'CAN', 'CAN']
                ]
            },
            {
                zh: '串口 / 通用网络', en: 'Serial / Generic Network', items: [
                    ['drivers/serial', '串口 Serial', 'Serial'],
                    ['drivers/tcp-udp', 'TCP/UDP', 'TCP/UDP']
                ]
            },
            {
                zh: '数据库', en: 'Database', items: [
                    ['drivers/mysql', 'MySQL', 'MySQL'],
                    ['drivers/postgresql', 'PostgreSQL', 'PostgreSQL'],
                    ['drivers/oracle', 'Oracle', 'Oracle'],
                    ['drivers/sqlserver', 'SQL Server', 'SQL Server']
                ]
            },
            {
                zh: '虚拟 / 测试', en: 'Virtual / Testing', items: [
                    ['drivers/virtual', '虚拟 Virtual', 'Virtual'],
                    ['drivers/listening-virtual', '监听虚拟', 'Listening Virtual']
                ]
            },
            {
                zh: '附录', en: 'Appendix', items: [
                    ['drivers/matrix', '驱动能力矩阵', 'Driver Capability Matrix']
                ]
            }
        ]
    },

    {
        navZh: '运维', navEn: 'Operations', landing: 'operation',
        paths: ['operation', 'guide'], activeMatch: '^/(zh|en)/(operation|guide)/',
        groups: [
            {
                zh: '运营', en: 'Operations', items: [
                    ['operation', '概览', 'Overview'],
                    ['operation/data-commands', '数据与命令', 'Data & Commands'],
                    ['operation/alarms', '告警与通知', 'Alarms & Notifications']
                ]
            },
            {
                zh: '部署与运维', en: 'Deployment & Ops', items: [
                    ['guide/usage', '部署模式与镜像源', 'Deployment & Images'],
                    ['guide/observability', '可观测性', 'Observability'],
                    ['guide/logging', '日志规范', 'Logging'],
                    ['guide/troubleshooting', '故障排查', 'Troubleshooting']
                ]
            }
        ]
    },

    {
        navZh: '开发', navEn: 'Develop', landing: 'development',
        paths: ['development', 'ai', 'automation'], activeMatch: '^/(zh|en)/(development|ai|automation)/',
        groups: [
            {
                zh: '开发', en: 'Development', items: [
                    ['development', '概览', 'Overview'],
                    ['development/driver-authoring', '驱动开发', 'Driver Authoring'],
                    ['development/api-documentation', 'API 文档', 'API Documentation'],
                    ['development/testing', '测试', 'Testing'],
                    ['development/changelog', '变更日志', 'Changelog']
                ]
            },
            {
                zh: 'AI 集成', en: 'AI Integration', items: [
                    ['ai', 'AI 概览', 'AI Overview'],
                    ['ai/agentic', 'Agentic 中心', 'Agentic Center'],
                    ['ai/mcp', 'AI Agent / MCP', 'AI Agent / MCP']
                ]
            },
            {
                zh: '自动化', en: 'Automation', items: [
                    ['automation/cli', 'CLI 使用指南', 'CLI Guide']
                ]
            }
        ]
    }
]

// Community: only a nav dropdown + its own sidebar, not counted as a pillar
const COMMUNITY: ReadonlyArray<Entry> = [
    ['community/contributing', '贡献指南', 'Contributing'],
    ['community/code-of-conduct', '行为准则', 'Code of Conduct'],
    ['community/security', '安全策略', 'Security']
]

type Lang = 'zh' | 'en'
type SidebarItem = { text: string; link: string }
type SidebarGroup = { text: string; collapsed?: boolean; items: SidebarItem[] }

// single-segment code → directory index (trailing slash hits index.md); multi-segment code → specific page
const linkOf = (lang: Lang, code: string) => {
    const p = lang === 'en' ? '/en' : '/zh'
    return code.includes('/') ? `${p}/${code}` : `${p}/${code}/`
}

const itemsOf = (lang: Lang, entries: ReadonlyArray<Entry>): SidebarItem[] =>
    entries.map(([code, zh, en]) => ({text: lang === 'en' ? en : zh, link: linkOf(lang, code)}))

function buildSidebar(lang: Lang) {
    const p = lang === 'en' ? '/en' : '/zh'
    const sidebar: Record<string, SidebarGroup[]> = {}
    for (const pillar of PILLARS) {
        const groups: SidebarGroup[] = pillar.groups.map(g => {
            const text = lang === 'en' ? g.en : g.zh
            const items = itemsOf(lang, g.items)
            return text ? {text, collapsed: false, items} : {text: '', items}
        })
        // multiple paths share one sidebar; specific-page paths (multi-segment) register as longer keys, longest prefix wins
        for (const path of pillar.paths) {
            const key = path.includes('/') ? `${p}/${path}` : `${p}/${path}/`
            sidebar[key] = groups
        }
    }
    sidebar[`${p}/community/`] = [{
        text: lang === 'en' ? 'Community' : '社区',
        items: itemsOf(lang, COMMUNITY)
    }]
    return sidebar
}

function buildNav(lang: Lang) {
    const p = lang === 'en' ? '/en' : '/zh'
    const t = lang === 'en'
    const pillars = PILLARS.map(pillar => ({
        text: t ? pillar.navEn : pillar.navZh,
        link: linkOf(lang, pillar.landing),
        ...(pillar.activeMatch ? {activeMatch: pillar.activeMatch} : {})
    }))
    return [
        {text: t ? 'Home' : '首页', link: `${p}/`},
        ...pillars,
        {text: t ? 'Community' : '社区', items: itemsOf(lang, COMMUNITY)}
    ]
}

function uiLabels(lang: Lang) {
    if (lang === 'en') {
        return {
            editLinkText: 'Edit this page on GitHub',
            outlineLabel: 'On this page',
            lastUpdatedText: 'Last updated',
            returnToTop: 'Back to top',
            sidebarMenu: 'Menu',
            darkModeSwitch: 'Appearance',
            lightModeSwitchTitle: 'Switch to light theme',
            darkModeSwitchTitle: 'Switch to dark theme',
            docFooterPrev: 'Previous',
            docFooterNext: 'Next',
            searchButtonText: 'Search',
            searchButtonAria: 'Search',
            searchNoResults: 'No results found',
            searchReset: 'Reset query',
            searchSelect: 'Select',
            searchNavigate: 'Switch',
            searchClose: 'Close'
        }
    }
    return {
        editLinkText: '在 GitHub 上编辑此页',
        outlineLabel: '页面导航',
        lastUpdatedText: '最后更新于',
        returnToTop: '回到顶部',
        sidebarMenu: '菜单',
        darkModeSwitch: '主题',
        lightModeSwitchTitle: '切换到浅色模式',
        darkModeSwitchTitle: '切换到深色模式',
        docFooterPrev: '上一页',
        docFooterNext: '下一页',
        searchButtonText: '搜索文档',
        searchButtonAria: '搜索文档',
        searchNoResults: '无法找到相关结果',
        searchReset: '清除查询条件',
        searchSelect: '选择',
        searchNavigate: '切换',
        searchClose: '关闭'
    }
}

function localeThemeConfig(lang: Lang) {
    const t = uiLabels(lang)
    return {
        nav: buildNav(lang),
        sidebar: buildSidebar(lang),
        editLink: {text: t.editLinkText},
        outline: {level: [2, 3], label: t.outlineLabel},
        lastUpdated: {
            text: t.lastUpdatedText,
            formatOptions: {dateStyle: 'short', timeStyle: 'medium'}
        },
        docFooter: {prev: t.docFooterPrev, next: t.docFooterNext},
        returnToTopLabel: t.returnToTop,
        sidebarMenuLabel: t.sidebarMenu,
        darkModeSwitchLabel: t.darkModeSwitch,
        lightModeSwitchTitle: t.lightModeSwitchTitle,
        darkModeSwitchTitle: t.darkModeSwitchTitle,
        search: {
            provider: 'local',
            options: {
                translations: {
                    button: {
                        buttonText: t.searchButtonText,
                        buttonAriaLabel: t.searchButtonAria
                    },
                    modal: {
                        noResultsText: t.searchNoResults,
                        resetButtonTitle: t.searchReset,
                        footer: {
                            selectText: t.searchSelect,
                            navigateText: t.searchNavigate,
                            closeText: t.searchClose
                        }
                    }
                }
            }
        }
    }
}

// Mermaid rendering: brand-blue theme in light mode; dark mode is forced by the plugin. A unified CJK-capable technical font stack avoids text clipping.
const MERMAID = {
    theme: 'base',
    themeVariables: {
        fontFamily: '"Microsoft YaHei", "PingFang SC", "Noto Sans CJK SC", sans-serif',
        fontSize: '14px',
        // primary node: light teal fill + brand teal border + deep ink text (close to the Architecture card look)
        primaryColor: '#ecfdf6',
        primaryBorderColor: '#0e9f6e',
        primaryTextColor: '#0f2231',
        // edges and secondary elements: restrained neutral blue-gray, no flashiness
        lineColor: '#7c93ab',
        secondaryColor: '#eef4fb',
        secondaryBorderColor: '#3f7fb8',
        secondaryTextColor: '#0f2231',
        tertiaryColor: '#f6f9fc',
        tertiaryBorderColor: '#9bb2c7',
        tertiaryTextColor: '#0f2231',
        // groups/subgraphs: very light fill + dashed-feel border (consistent with the Architecture region)
        clusterBkg: 'rgba(14, 159, 110, 0.05)',
        clusterBorder: '#9cc7b4',
        // edge labels: blend with the card fill, neutral text
        edgeLabelBackground: '#f6f9fc',
        labelBoxBorderColor: '#9bb2c7',
        // notes: warm yellow
        noteBkgColor: '#fff7e6',
        noteBorderColor: '#d8a544',
        noteTextColor: '#5b4a1f',
        // sequence-diagram actor boxes
        actorBkg: '#ecfdf6',
        actorBorder: '#0e9f6e',
        actorTextColor: '#0f2231',
        signalColor: '#5f7a90',
        signalTextColor: '#33485c'
    },
    flowchart: {curve: 'basis', useMaxWidth: true, nodeSpacing: 60, rankSpacing: 66, padding: 24, htmlLabels: false},
    sequence: {
        useMaxWidth: true,
        mirrorActors: false,
        actorMargin: 64,
        boxMargin: 12,
        noteMargin: 12,
        messageMargin: 44
    },
    state: {useMaxWidth: true, padding: 18},
    er: {useMaxWidth: true},
    class: {useMaxWidth: true}
}

// The root path (language gate) always redirects to /zh/ or /en/ by preference: localStorage first, otherwise browser language; the language-selection page is only a fallback for when JS is disabled
const LANG_DETECT = `(function(){var K='dc3-lang',B='/';var p;try{p=localStorage.getItem(K);}catch(e){}var r=location.pathname;if(r.length&&r[r.length-1]!=='/')r=r+'/';var rel=r.indexOf(B)===0?r.slice(B.length):r;var onEn=rel.indexOf('en/')===0;var onZh=rel.indexOf('zh/')===0;if(!onEn&&!onZh){if(!p){p=/^en/i.test(navigator.language)?'en':'zh';try{localStorage.setItem(K,p);}catch(e){}}location.replace(B+p+'/');}})();`

export default withMermaid(defineConfig({
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
            {icon: 'github', link: 'https://github.com/pnoker/iot-dc3'}
        ],

        editLink: {
            pattern: 'https://github.com/pnoker/iot-dc3/edit/release/docs/:path'
        },

        footer: {
            message: 'Released under the AGPL-3.0 License · 基于 AGPL-3.0 协议发布',
            copyright: 'Copyright © 2017-2026 pnoker'
        }
    },

    markdown: {
        lineNumbers: false,
        image: {
            lazyLoading: true
        }
    },

    mermaid: MERMAID,
    mermaidPlugin: {
        class: 'dc3-mermaid'
    }
}))
