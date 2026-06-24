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

// 各栏目页面结构：[code, 中文标题, English]
// code 为空串 → 栏目首页；含 "/" → 跨栏目绝对路径（如 'architecture/modules'）
// 扁平结构（不设 group 文本）：侧栏直接列页面，避免与顶部 nav 标题重复
const SECTIONS: ReadonlyArray<{
    key: string
    items: ReadonlyArray<readonly [string, string, string]>
}> = [
    {key: 'introduction', items: [
        ['', '总览', 'Overview'],
        ['concepts', '核心概念', 'Core Concepts'],
        ['concepts/profile', '物模型', 'Profile'],
        ['concepts/device', '设备', 'Device'],
        ['concepts/driver', '驱动', 'Driver'],
        ['concepts/point', '位号', 'Point'],
        ['concepts/point-value', '位号值', 'Point Value'],
        ['concepts/command', '指令', 'Command'],
        ['concepts/event', '事件', 'Event'],
        ['concepts/attribute-config', '属性与配置', 'Attribute & Config'],
        ['concepts/tenant', '租户', 'Tenant'],
        ['paths', '按角色选择路径', 'Choose Your Path']
    ]},
    {key: 'quickstart', items: [
        ['', '本地开发', 'Local Development'],
        ['environment', '环境变量', 'Environment Variables'],
        ['first-device', '第一个设备', 'First Device']
    ]},
    {key: 'architecture', items: [
        ['', '总览', 'Overview'],
        ['services', '服务与拓扑', 'Services & Topology'],
        ['facade-modes', 'Facade 模式', 'Facade Modes'],
        ['data-plane', '数据平面', 'Data Plane'],
        ['command-plane', '命令平面', 'Command Plane'],
        ['auth-rbac', '鉴权 · 租户 · RBAC', 'Auth, Tenant & RBAC'],
        ['domain-model', '领域模型', 'Domain Model'],
        ['modules', '模块地图', 'Module Map']
    ]},
    {key: 'operation', items: [
        ['', '概览', 'Overview'],
        ['device-onboarding', '设备接入', 'Device Onboarding'],
        ['data-commands', '数据与命令', 'Data & Commands'],
        ['alarms', '告警与通知', 'Alarms & Notifications']
    ]},
    {key: 'development', items: [
        ['', '概览', 'Overview'],
        ['driver-authoring', '驱动开发', 'Driver Authoring'],
        ['api-documentation', 'API 文档', 'API Documentation'],
        ['testing', '测试', 'Testing'],
        ['changelog', '变更日志', 'Changelog']
    ]},
    {key: 'ai', items: [
        ['', '概览', 'Overview'],
        ['agentic', 'Agentic 中心', 'Agentic Center'],
        ['mcp', 'AI Agent / MCP', 'AI Agent / MCP']
    ]},
    {key: 'automation', items: [
        ['', '概览', 'Overview'],
        ['cli', 'CLI 使用指南', 'CLI Guide']
    ]},
    {key: 'guide', items: [
        ['', '概览', 'Overview'],
        ['usage', '部署模式与镜像源', 'Deployment & Images'],
        ['observability', '可观测性', 'Observability'],
        ['logging', '日志规范', 'Logging'],
        ['troubleshooting', '故障排查', 'Troubleshooting']
    ]},
    {key: 'modules', items: [
        ['', '模块清单', 'Catalog'],
        ['architecture/modules', '模块地图', 'Module Map']
    ]},
    {key: 'community', items: [
        ['contributing', '贡献指南', 'Contributing'],
        ['code-of-conduct', '行为准则', 'Code of Conduct'],
        ['security', '安全策略', 'Security']
    ]}
]

type Lang = 'zh' | 'en'

// 各栏目侧栏分组标题（与顶部 nav 对应），[中文, English]
const SECTION_TITLES: Record<string, readonly [string, string]> = {
    introduction: ['介绍', 'Introduction'],
    quickstart: ['快速开始', 'Quick Start'],
    architecture: ['架构', 'Architecture'],
    operation: ['操作指南', 'Operation'],
    development: ['开发', 'Development'],
    ai: ['AI', 'AI'],
    automation: ['自动化', 'Automation'],
    guide: ['部署运维', 'Deployment'],
    modules: ['模块', 'Modules'],
    community: ['社区', 'Community']
}

function buildSidebar(lang: Lang) {
    const p = lang === 'en' ? '/en' : '/zh'
    // 每个栏目侧栏包一层分组标题（栏目名），其下列出页面
    const sidebar: Record<string, Array<{ text: string; items: Array<{ text: string; link: string }> }>> = {}
    for (const s of SECTIONS) {
        const items = s.items.map(([code, zh, en]) => {
            const link = code.includes('/')
                ? `${p}/${code}`
                : `${p}/${s.key}${code ? '/' + code : ''}`
            return {text: lang === 'en' ? en : zh, link}
        })
        const title = SECTION_TITLES[s.key]
        sidebar[`${p}/${s.key}/`] = [{text: lang === 'en' ? title[1] : title[0], items}]
    }
    return sidebar
}

function buildNav(lang: Lang) {
    const p = lang === 'en' ? '/en' : '/zh'
    const t = lang === 'en'
    return [
        {text: t ? 'Home' : '首页', link: `${p}/`},
        {text: t ? 'Introduction' : '介绍', link: `${p}/introduction/`},
        {text: t ? 'Quick Start' : '快速开始', link: `${p}/quickstart/`},
        {text: t ? 'Architecture' : '架构', link: `${p}/architecture/`},
        {text: t ? 'AI' : 'AI', link: `${p}/ai/`},
        {text: t ? 'Automation' : '自动化', link: `${p}/automation/`},
        {text: t ? 'Operation' : '操作指南', link: `${p}/operation/`},
        {text: t ? 'Development' : '开发', link: `${p}/development/`},
        {text: t ? 'Deployment' : '部署运维', link: `${p}/guide/`},
        {
            text: t ? 'Community' : '社区',
            items: [
                {text: t ? 'Contributing' : '贡献指南', link: `${p}/community/contributing`},
                {text: t ? 'Code of Conduct' : '行为准则', link: `${p}/community/code-of-conduct`},
                {text: t ? 'Security' : '安全策略', link: `${p}/community/security`}
            ]
        }
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

// Mermaid 渲染：浅色模式品牌蓝主题；深色模式由插件强制。统一中文技术字体栈避免文字遮挡。
const MERMAID = {
    theme: 'base',
    themeVariables: {
        fontFamily: '"Microsoft YaHei", "PingFang SC", "Noto Sans CJK SC", sans-serif',
        fontSize: '15px',
        primaryColor: '#e3f1fb',
        primaryBorderColor: '#0a6cb2',
        primaryTextColor: '#13212e',
        lineColor: '#4aa3da',
        secondaryColor: '#eef4fb',
        tertiaryColor: '#f5f9fd',
        noteBkgColor: '#fff7e6',
        noteBorderColor: '#d8a544'
    },
    flowchart: {curve: 'basis', useMaxWidth: true, nodeSpacing: 60, rankSpacing: 66, padding: 24, htmlLabels: false},
    sequence: {useMaxWidth: true, mirrorActors: false, actorMargin: 64, boxMargin: 12, noteMargin: 12, messageMargin: 44},
    state: {useMaxWidth: true, padding: 18},
    er: {useMaxWidth: true},
    class: {useMaxWidth: true}
}

// 根路径（语言门）总是按偏好重定向到 /zh/ 或 /en/：localStorage 优先，否则按浏览器语言；语言选择页仅作 JS 禁用的 fallback
const LANG_DETECT = `(function(){var K='dc3-lang',B='/';var p;try{p=localStorage.getItem(K);}catch(e){}var r=location.pathname;if(r.length&&r[r.length-1]!=='/')r=r+'/';var rel=r.indexOf(B)===0?r.slice(B.length):r;var onEn=rel.indexOf('en/')===0;var onZh=rel.indexOf('zh/')===0;if(!onEn&&!onZh){if(!p){p=/^en/i.test(navigator.language)?'en':'zh';try{localStorage.setItem(K,p);}catch(e){}}location.replace(B+p+'/');}})();`

export default withMermaid(defineConfig({
    base: '/',
    cleanUrls: true,
    lastUpdated: true,

    // 维护者内部资料不进入公开站点构建
    srcExclude: ['superpowers/**'],

    head: [
        ['link', {rel: 'icon', href: '/images/logo.svg', type: 'image/svg+xml'}],
        ['meta', {name: 'theme-color', content: '#1296db'}],
        ['script', {}, LANG_DETECT]
    ],

    locales: {
        root: {
            label: 'Languages',
            lang: 'zh-CN',
            themeConfig: {
                nav: [],
                sidebar: []
            }
        },
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
