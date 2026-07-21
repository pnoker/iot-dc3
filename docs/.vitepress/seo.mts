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

import {existsSync, readFileSync} from 'node:fs'
import {execSync} from 'node:child_process'
import {fileURLToPath} from 'node:url'
import {resolve} from 'node:path'
import type {HeadConfig, TransformContext} from 'vitepress'

const SITE_URL = 'https://docs.dc3.site'
const DOCS_ROOT = fileURLToPath(new URL('..', import.meta.url))
const DEFAULT_IMAGE = `${SITE_URL}/images/logo.png`
const ORGANIZATION_ID = `${SITE_URL}/#organization`
const WEBSITE_ID = `${SITE_URL}/#website`

const LOCALES = {
    zh: {hreflang: 'zh-CN', label: '简体中文', homeLabel: '首页'},
    en: {hreflang: 'en-US', label: 'English', homeLabel: 'Home'}
} as const

const SECTION_LABELS = {
    zh: {
        ai: 'AI 集成',
        architecture: '架构',
        automation: '自动化',
        community: '社区',
        development: '开发',
        drivers: '驱动',
        foundations: '基础',
        frontend: '前端',
        guide: '运维指南',
        introduction: '入门',
        modules: '模块',
        operation: '运维',
        quickstart: '快速开始'
    },
    en: {
        ai: 'AI Integration',
        architecture: 'Architecture',
        automation: 'Automation',
        community: 'Community',
        development: 'Development',
        drivers: 'Drivers',
        foundations: 'Foundations',
        frontend: 'Frontend',
        guide: 'Operations Guide',
        introduction: 'Introduction',
        modules: 'Modules',
        operation: 'Operations',
        quickstart: 'Quickstart'
    }
} as const

const HOME_DESCRIPTIONS = {
    zh: 'IoT DC3 是一个开源、云原生的工业物联网平台，提供多协议设备接入、数据采集、边云协同与 AI 赋能的运维能力。',
    en: 'IoT DC3 is an open-source, cloud-native industrial IoT platform for multi-protocol device connectivity, data acquisition, edge-to-cloud delivery, and AI-assisted operations.'
} as const

// Meta description length cap by locale. CJK glyphs are ~2× the display width of Latin
// chars and get truncated earlier in Baidu/Google SERPs, so Chinese uses a tighter cap.
const DESC_MAX_LENGTH: Record<keyof typeof LOCALES, number> = {zh: 80, en: 155}

// Page-level Open Graph image per section. Reuses the existing PNG assets in public/images
// (sized for social cards) instead of serving the same logo on every page.
type OgImageCfg = {
    file: (locale: keyof typeof LOCALES) => string
    width: number
    height: number
    altZh: string
    altEn: string
}

const SECTION_OG_IMAGES: Record<string, OgImageCfg> = {
    ai: {file: () => 'screenshot-ai.png', width: 1920, height: 1080, altZh: 'IoT DC3 AI 集成：Spring AI Agent 与 MCP 工具调用', altEn: 'IoT DC3 AI integration: Spring AI Agent and MCP tool calls'},
    drivers: {file: () => 'screenshot-device.png', width: 1920, height: 1116, altZh: 'IoT DC3 设备与位号管理', altEn: 'IoT DC3 device and point management'},
    operation: {file: () => 'screenshot-device.png', width: 1920, height: 1116, altZh: 'IoT DC3 设备运维：采集、命令与告警', altEn: 'IoT DC3 device operations: collection, commands and alarms'},
    architecture: {file: l => `architecture-panorama-${l}.png`, width: 2400, height: 1536, altZh: 'IoT DC3 架构全景', altEn: 'IoT DC3 architecture panorama'},
    introduction: {file: () => 'screenshot-overview.png', width: 1920, height: 1080, altZh: 'IoT DC3 平台总览', altEn: 'IoT DC3 platform overview'},
    foundations: {file: () => 'screenshot-overview.png', width: 1920, height: 1080, altZh: 'IoT DC3 物联网技术体系', altEn: 'IoT DC3 IoT technology stack'},
    quickstart: {file: () => 'screenshot-overview.png', width: 1920, height: 1080, altZh: 'IoT DC3 快速开始', altEn: 'IoT DC3 quickstart'},
    guide: {file: () => 'screenshot-overview.png', width: 1920, height: 1080, altZh: 'IoT DC3 运维指南', altEn: 'IoT DC3 operations guide'},
    development: {file: () => 'screenshot-overview.png', width: 1920, height: 1080, altZh: 'IoT DC3 开发指南', altEn: 'IoT DC3 development guide'},
    modules: {file: () => 'screenshot-overview.png', width: 1920, height: 1080, altZh: 'IoT DC3 模块地图', altEn: 'IoT DC3 module map'},
    frontend: {file: () => 'screenshot-overview.png', width: 1920, height: 1080, altZh: 'IoT DC3 前端开发', altEn: 'IoT DC3 frontend'},
    automation: {file: () => 'screenshot-overview.png', width: 1920, height: 1080, altZh: 'IoT DC3 自动化', altEn: 'IoT DC3 automation'},
    community: {file: () => 'screenshot-overview.png', width: 1920, height: 1080, altZh: 'IoT DC3 社区', altEn: 'IoT DC3 community'}
}

const ORGANIZATION = {
    '@type': 'Organization',
    '@id': ORGANIZATION_ID,
    name: 'IoT DC3',
    url: SITE_URL,
    logo: {
        '@type': 'ImageObject',
        url: DEFAULT_IMAGE
    },
    sameAs: [
        'https://github.com/pnoker/iot-dc3',
        'https://gitee.com/pnoker/iot-dc3',
        'https://x.com/IoTDC3'
    ]
}

function getLocale(relativePath: string): keyof typeof LOCALES | null {
    const locale = relativePath.split('/')[0] as keyof typeof LOCALES
    return locale in LOCALES ? locale : null
}

function getLocalizedSourcePath(relativePath: string, locale: keyof typeof LOCALES) {
    const pathWithoutLocale = relativePath.replace(/^(zh|en)\//, '')
    return resolve(DOCS_ROOT, locale, pathWithoutLocale)
}

function getRoutePath(relativePath: string) {
    const route = relativePath
        .replace(/\\/g, '/')
        .replace(/\.md$/, '')
        .replace(/\/index$/, '/')

    return route ? `/${route}` : '/'
}

function getUrl(routePath: string) {
    return new URL(routePath, SITE_URL).href
}

function cleanText(value: string) {
    return value
        .replace(/<!--[\s\S]*?-->/g, ' ')
        .replace(/<pre[\s\S]*?<\/pre>/gi, ' ')
        .replace(/<code[\s\S]*?<\/code>/gi, ' ')
        .replace(/<[^>]+>/g, ' ')
        .replace(/!\[([^\]]*)\]\([^)]*\)/g, '$1')
        .replace(/\[([^\]]+)\]\([^)]*\)/g, '$1')
        .replace(/[\\#>*_`~|]/g, ' ')
        .replace(/\s+/g, ' ')
        // collapse stray spaces introduced between CJK chars when inline markdown
        // (backticks/links) is stripped, e.g. "位号 的 一个" -> "位号的一个"
        .replace(/([一-鿿、，。；：！？）」』])\s+([一-鿿、，。；：！？（「『])/g, '$1$2')
        .trim()
}

function truncate(value: string, maxLength = 160) {
    const characters = Array.from(value)
    if (characters.length <= maxLength) return value
    return `${characters.slice(0, maxLength - 1).join('')}…`
}

function getMarkdownDescription(filePath: string, maxLength = 155) {
    let source: string
    try {
        source = readFileSync(filePath, 'utf8')
    } catch (_) {
        return ''
    }

    const content = source.replace(/^---\r?\n[\s\S]*?\r?\n---\r?\n?/, '')
    const paragraph = content
        .split(/\r?\n\s*\r?\n/)
        .map(block => block.trim())
        .find(block => {
            if (!block || block.startsWith('#') || block.startsWith('```') || block.startsWith(':::')) return false
            if (block.startsWith('|') || block.startsWith('- ') || block.startsWith('* ')) return false
            if (block.startsWith('<')) return false
            return true
        })

    return paragraph ? truncate(cleanText(paragraph), maxLength) : ''
}

function getDescription(context: TransformContext, locale: keyof typeof LOCALES) {
    const max = DESC_MAX_LENGTH[locale]
    const frontmatterDescription = context.pageData.frontmatter.description
    if (typeof frontmatterDescription === 'string' && frontmatterDescription.trim()) {
        return truncate(cleanText(frontmatterDescription), max)
    }

    if (context.pageData.frontmatter.layout === 'home') {
        return HOME_DESCRIPTIONS[locale]
    }

    const extracted = getMarkdownDescription(context.pageData.filePath, max)
    return extracted || HOME_DESCRIPTIONS[locale]
}

function getTitle(context: TransformContext) {
    return context.pageData.title || context.title || 'IoT DC3'
}

// Earliest git author date for a file, used as TechArticle.datePublished (schema.org).
// Cached per build; null when git is unavailable (e.g. shallow CI clone) — falls back to dateModified only.
const firstCommitCache = new Map<string, string | null>()
function getFirstPublished(filePath: string): string | null {
    if (firstCommitCache.has(filePath)) return firstCommitCache.get(filePath) ?? null
    let result: string | null = null
    try {
        const out = execSync(`git log --diff-filter=A --follow --format=%aI -- ${JSON.stringify(filePath)}`, {
            cwd: DOCS_ROOT,
            encoding: 'utf8',
            stdio: ['ignore', 'pipe', 'ignore']
        }).trim()
        // %aI lists newest-first; the earliest add (file's true origin) is the last line.
        const lines = out.split('\n').filter(Boolean)
        const earliest = lines[lines.length - 1]
        if (earliest) result = new Date(earliest).toISOString()
    } catch (_) {
        result = null
    }
    firstCommitCache.set(filePath, result)
    return result
}

function getAvailableLocales(relativePath: string) {
    return (Object.keys(LOCALES) as Array<keyof typeof LOCALES>)
        .filter(locale => existsSync(getLocalizedSourcePath(relativePath, locale)))
}

function getAlternates(relativePath: string) {
    const pathWithoutLocale = relativePath.replace(/^(zh|en)\//, '')
    const available = getAvailableLocales(relativePath)
    const tags: HeadConfig[] = []

    for (const alternateLocale of available) {
        const alternatePath = getRoutePath(`${alternateLocale}/${pathWithoutLocale}`)
        tags.push(['link', {
            rel: 'alternate',
            hreflang: LOCALES[alternateLocale].hreflang,
            href: getUrl(alternatePath)
        }])
    }

    // x-default points at the English version when available (the global fallback for
    // non-Chinese visitors), otherwise at Chinese.
    const defaultLocale = available.includes('en') ? 'en' : 'zh'
    if (available.includes(defaultLocale)) {
        const defaultPath = getRoutePath(`${defaultLocale}/${pathWithoutLocale}`)
        tags.push(['link', {rel: 'alternate', hreflang: 'x-default', href: getUrl(defaultPath)}])
    }

    return tags
}

function getOgImage(relativePath: string, locale: keyof typeof LOCALES) {
    const section = relativePath.replace(/^(zh|en)\//, '').split('/')[0]
    const cfg = SECTION_OG_IMAGES[section]
    if (!cfg) {
        return {
            url: DEFAULT_IMAGE,
            width: 480,
            height: 461,
            alt: locale === 'en' ? 'IoT DC3 industrial IoT platform logo' : 'IoT DC3 工业物联网平台 logo'
        }
    }
    return {
        url: `${SITE_URL}/images/${cfg.file(locale)}`,
        width: cfg.width,
        height: cfg.height,
        alt: locale === 'en' ? cfg.altEn : cfg.altZh
    }
}

function getBreadcrumbs(relativePath: string, locale: keyof typeof LOCALES, title: string) {
    const pathWithoutLocale = relativePath
        .replace(/^(zh|en)\//, '')
        .replace(/\.md$/, '')
        .replace(/\/index$/, '')
    const segments = pathWithoutLocale.split('/').filter(Boolean)
    const items = [{
        '@type': 'ListItem',
        position: 1,
        name: LOCALES[locale].homeLabel,
        item: getUrl(`/${locale}/`)
    }]

    // Locale home: a single breadcrumb entry. Skip the self-referential second item
    // (home -> home) that the generic title branch would otherwise add.
    if (pathWithoutLocale === 'index' || segments.length === 0) {
        return items
    }

    if (segments.length > 1) {
        const section = segments[0] as keyof typeof SECTION_LABELS[typeof locale]
        items.push({
            '@type': 'ListItem',
            position: 2,
            name: SECTION_LABELS[locale][section] ?? section,
            item: getUrl(`/${locale}/${section}/`)
        })
    }

    if (segments.length > 0) {
        items.push({
            '@type': 'ListItem',
            position: items.length + 1,
            name: title,
            item: getUrl(getRoutePath(relativePath))
        })
    }

    return items
}

function getStructuredData(context: TransformContext, locale: keyof typeof LOCALES, canonicalUrl: string, title: string, description: string) {
    const isHome = context.pageData.frontmatter.layout === 'home'
    const pageType = isHome ? 'WebPage' : 'TechArticle'
    const page: Record<string, unknown> = {
        '@type': pageType,
        '@id': `${canonicalUrl}#webpage`,
        url: canonicalUrl,
        name: title,
        description,
        inLanguage: LOCALES[locale].hreflang,
        isPartOf: {'@id': WEBSITE_ID},
        breadcrumb: {'@id': `${canonicalUrl}#breadcrumb`}
    }

    if (!isHome) {
        page.author = {'@id': ORGANIZATION_ID}
        page.publisher = {'@id': ORGANIZATION_ID}
        page.headline = title
        const published = getFirstPublished(context.pageData.filePath)
        if (published) page.datePublished = published
        if (context.pageData.lastUpdated) {
            page.dateModified = new Date(context.pageData.lastUpdated).toISOString()
        }
    }

    const graph: Record<string, unknown>[] = [
        ORGANIZATION,
        {
            '@type': 'WebSite',
            '@id': WEBSITE_ID,
            url: SITE_URL,
            name: 'IoT DC3',
            publisher: {'@id': ORGANIZATION_ID},
            inLanguage: LOCALES[locale].hreflang
        },
        page,
        {
            '@type': 'BreadcrumbList',
            '@id': `${canonicalUrl}#breadcrumb`,
            itemListElement: getBreadcrumbs(context.pageData.relativePath, locale, title)
        }
    ]

    return JSON.stringify({'@context': 'https://schema.org', '@graph': graph}).replace(/</g, '\\u003c')
}

export function transformHead(context: TransformContext): HeadConfig[] {
    const locale = getLocale(context.pageData.relativePath)
    if (context.pageData.isNotFound) {
        return [['meta', {name: 'robots', content: 'noindex,follow'}]]
    }

    // Root language gate (relativePath has no locale prefix, e.g. `index.md`): it
    // JS-redirects to /zh/ or /en/. Keep it noindex but declare hreflang alternates
    // so search engines can cluster the localized homes; description is supplied by the
    // root index.md frontmatter (avoids VitePress's default "A VitePress site").
    if (!locale) {
        return [
            ['meta', {name: 'robots', content: 'noindex,follow'}],
            ['link', {rel: 'canonical', href: getUrl('/')}],
            ['link', {rel: 'alternate', hreflang: 'zh-CN', href: getUrl('/zh/')}],
            ['link', {rel: 'alternate', hreflang: 'en-US', href: getUrl('/en/')}],
            ['link', {rel: 'alternate', hreflang: 'x-default', href: getUrl('/en/')}]
        ]
    }

    const title = getTitle(context)
    const description = getDescription(context, locale)
    const canonicalUrl = getUrl(getRoutePath(context.pageData.relativePath))
    const isHome = context.pageData.frontmatter.layout === 'home'
    const og = getOgImage(context.pageData.relativePath, locale)
    const localeAlternates = getAvailableLocales(context.pageData.relativePath)
        .filter(alternateLocale => alternateLocale !== locale)
        .map(alternateLocale => LOCALES[alternateLocale])

    return [
        ['meta', {name: 'description', content: description}],
        ['meta', {name: 'robots', content: 'index,follow,max-image-preview:large,max-snippet:-1,max-video-preview:-1'}],
        ['meta', {name: 'author', content: 'IoT DC3 Contributors'}],
        ['link', {rel: 'canonical', href: canonicalUrl}],
        ['link', {rel: 'alternate', type: 'text/plain', href: `${SITE_URL}/llms.txt`, title: 'AI-readable site summary'}],
        ...getAlternates(context.pageData.relativePath),
        ['meta', {property: 'og:type', content: isHome ? 'website' : 'article'}],
        ['meta', {property: 'og:site_name', content: 'IoT DC3'}],
        ['meta', {property: 'og:title', content: title}],
        ['meta', {property: 'og:description', content: description}],
        ['meta', {property: 'og:url', content: canonicalUrl}],
        ['meta', {property: 'og:image', content: og.url}],
        ['meta', {property: 'og:image:width', content: String(og.width)}],
        ['meta', {property: 'og:image:height', content: String(og.height)}],
        ['meta', {property: 'og:image:alt', content: og.alt}],
        ['meta', {property: 'og:locale', content: LOCALES[locale].hreflang.replace('-', '_')}],
        ...localeAlternates.map(item => ['meta', {property: 'og:locale:alternate', content: item.hreflang.replace('-', '_')}] as HeadConfig),
        ['meta', {name: 'twitter:card', content: 'summary_large_image'}],
        ['meta', {name: 'twitter:title', content: title}],
        ['meta', {name: 'twitter:description', content: description}],
        ['meta', {name: 'twitter:image', content: og.url}],
        ['script', {type: 'application/ld+json'}, getStructuredData(context, locale, canonicalUrl, title, description)]
    ]
}
