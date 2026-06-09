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

export default defineConfig({
    lang: 'zh-CN',
    title: 'IoT DC3',
    description: '面向工业物联网的分布式设备接入、数据采集与 AI 运营平台',
    base: '/iot-dc3/',
    cleanUrls: true,
    lastUpdated: true,

    head: [
        ['link', {rel: 'icon', href: '/iot-dc3/images/logo-blue.zh.png'}],
        ['meta', {name: 'theme-color', content: '#3c8772'}]
    ],

    themeConfig: {
        logo: '/images/logo-blue.zh.png',
        siteTitle: 'IoT DC3',

        nav: [
            {text: '首页', link: '/'},
            {text: '快速开始', link: '/quickstart/'},
            {text: '操作手册', link: '/operation/'},
            {text: '使用指南', link: '/guide/'},
            {text: '架构', link: '/architecture/'},
            {text: '开发', link: '/development/'},
            {text: '模块', link: '/modules/'},
            {text: 'Superpowers', link: '/superpowers/'},
            {
                text: '社区',
                items: [
                    {text: '贡献指南', link: '/community/contributing'},
                    {text: '行为准则', link: '/community/code-of-conduct'},
                    {text: '安全策略', link: '/community/security'}
                ]
            }
        ],

        sidebar: {
            '/quickstart/': [
                {
                    text: '快速开始',
                    items: [
                        {text: '本地开发', link: '/quickstart/'},
                        {text: '环境变量', link: '/quickstart/environment'}
                    ]
                }
            ],
            '/guide/': [
                {
                    text: '使用指南',
                    items: [
                        {text: '概览', link: '/guide/'},
                        {text: '镜像与部署', link: '/guide/usage'},
                        {text: '日志规范', link: '/guide/logging'},
                        {text: '故障排查', link: '/guide/troubleshooting'}
                    ]
                }
            ],
            '/operation/': [
                {
                    text: '操作手册',
                    items: [
                        {text: '概览', link: '/operation/'},
                        {text: '核心概念', link: '/operation/concepts'},
                        {text: '设备接入流程', link: '/operation/device-onboarding'},
                        {text: '数据与命令', link: '/operation/data-commands'},
                        {text: 'AI 辅助运营', link: '/operation/agentic'}
                    ]
                }
            ],
            '/architecture/': [
                {
                    text: '架构',
                    items: [
                        {text: '概览', link: '/architecture/'},
                        {text: '模块与依赖', link: '/architecture/modules'}
                    ]
                }
            ],
            '/development/': [
                {
                    text: '开发',
                    items: [
                        {text: '概览', link: '/development/'},
                        {text: '驱动开发', link: '/development/driver-authoring'},
                        {text: 'API 文档', link: '/development/api-documentation'},
                        {text: '测试', link: '/development/testing'},
                        {text: '变更日志', link: '/development/changelog'}
                    ]
                }
            ],
            '/modules/': [
                {
                    text: '模块',
                    items: [
                        {text: '模块清单', link: '/modules/'}
                    ]
                }
            ],
            '/superpowers/': [
                {
                    text: 'Superpowers',
                    items: [
                        {text: '概览', link: '/superpowers/'},
                        {text: '文档机制', link: '/superpowers/documentation/'}
                    ]
                },
                {
                    text: '设计方案',
                    items: [
                        {text: '方案索引', link: '/superpowers/design/'},
                        {text: '物模型', link: '/superpowers/design/thing-model'},
                        {text: '指令与事件属性', link: '/superpowers/design/command-event-attribute-config'},
                        {text: '位号命令链路', link: '/superpowers/design/point-command'},
                        {text: '设备与驱动超时', link: '/superpowers/design/device-driver-timeout'},
                        {text: '自定义指令调用', link: '/superpowers/design/command-call'},
                        {text: '事件上报', link: '/superpowers/design/event-report'},
                        {text: '实体告警统一表', link: '/superpowers/design/entity-alarm'},
                        {text: '规则告警链路优化', link: '/superpowers/design/rule-alarm-optimization'}
                    ]
                },
                {
                    text: '分析与策略',
                    items: [
                        {text: '分析索引', link: '/superpowers/analysis/'},
                        {text: '协议库对比分析', link: '/superpowers/analysis/iot-dc3-vs-iot-communication'},
                        {text: '策略索引', link: '/superpowers/strategy/'},
                        {text: 'AI 定位草案', link: '/superpowers/strategy/ai-positioning'}
                    ]
                },
                {
                    text: '计划与归档',
                    items: [
                        {text: '待办池', link: '/superpowers/backlog/'},
                        {text: '旧版操作手册', link: '/superpowers/legacy-operation/'},
                        {text: '旧版驱动截图', link: '/superpowers/legacy-operation/driver'},
                        {text: '旧版模板截图', link: '/superpowers/legacy-operation/template'},
                        {text: '旧版设备截图', link: '/superpowers/legacy-operation/device'},
                        {text: '旧版数据截图', link: '/superpowers/legacy-operation/data'}
                    ]
                }
            ],
            '/community/': [
                {
                    text: '社区',
                    items: [
                        {text: '贡献指南', link: '/community/contributing'},
                        {text: '行为准则', link: '/community/code-of-conduct'},
                        {text: '安全策略', link: '/community/security'}
                    ]
                }
            ]
        },

        socialLinks: [
            {icon: 'github', link: 'https://github.com/pnoker/iot-dc3'}
        ],

        editLink: {
            pattern: 'https://github.com/pnoker/iot-dc3/edit/release/docs/:path',
            text: '在 GitHub 上编辑此页'
        },

        footer: {
            message: '基于 AGPL-3.0 协议发布',
            copyright: 'Copyright © 2017-2026 pnoker'
        },

        search: {
            provider: 'local',
            options: {
                translations: {
                    button: {
                        buttonText: '搜索文档',
                        buttonAriaLabel: '搜索文档'
                    },
                    modal: {
                        noResultsText: '无法找到相关结果',
                        resetButtonTitle: '清除查询条件',
                        footer: {
                            selectText: '选择',
                            navigateText: '切换',
                            closeText: '关闭'
                        }
                    }
                }
            }
        },

        docFooter: {
            prev: '上一页',
            next: '下一页'
        },

        outline: {
            level: [2, 3],
            label: '页面导航'
        },

        lastUpdated: {
            text: '最后更新于',
            formatOptions: {
                dateStyle: 'short',
                timeStyle: 'medium'
            }
        },

        returnToTopLabel: '回到顶部',
        sidebarMenuLabel: '菜单',
        darkModeSwitchLabel: '主题',
        lightModeSwitchTitle: '切换到浅色模式',
        darkModeSwitchTitle: '切换到深色模式'
    },

    markdown: {
        lineNumbers: false,
        image: {
            lazyLoading: true
        }
    }
})
