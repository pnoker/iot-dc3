import {existsSync, readFileSync} from 'node:fs'
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
        .trim()
}

function truncate(value: string, maxLength = 160) {
    const characters = Array.from(value)
    if (characters.length <= maxLength) return value
    return `${characters.slice(0, maxLength - 1).join('')}…`
}

function getMarkdownDescription(filePath: string) {
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

    return paragraph ? truncate(cleanText(paragraph)) : ''
}

function getDescription(context: TransformContext, locale: keyof typeof LOCALES) {
    const frontmatterDescription = context.pageData.frontmatter.description
    if (typeof frontmatterDescription === 'string' && frontmatterDescription.trim()) {
        return truncate(cleanText(frontmatterDescription))
    }

    if (context.pageData.frontmatter.layout === 'home') {
        return HOME_DESCRIPTIONS[locale]
    }

    const extracted = getMarkdownDescription(context.pageData.filePath)
    return extracted || HOME_DESCRIPTIONS[locale]
}

function getTitle(context: TransformContext) {
    return context.pageData.title || context.title || 'IoT DC3'
}

function getAvailableLocales(relativePath: string) {
    return (Object.keys(LOCALES) as Array<keyof typeof LOCALES>)
        .filter(locale => existsSync(getLocalizedSourcePath(relativePath, locale)))
}

function getAlternates(relativePath: string) {
    const pathWithoutLocale = relativePath.replace(/^(zh|en)\//, '')
    const tags: HeadConfig[] = []

    for (const alternateLocale of getAvailableLocales(relativePath)) {
        const alternatePath = getRoutePath(`${alternateLocale}/${pathWithoutLocale}`)
        tags.push(['link', {
            rel: 'alternate',
            hreflang: LOCALES[alternateLocale].hreflang,
            href: getUrl(alternatePath)
        }])
    }

    if (getAvailableLocales(relativePath).includes('zh')) {
        const defaultPath = getRoutePath(`zh/${pathWithoutLocale}`)
        tags.push(['link', {rel: 'alternate', hreflang: 'x-default', href: getUrl(defaultPath)}])
    }

    return tags
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
    if (context.pageData.isNotFound || !locale) {
        return [['meta', {name: 'robots', content: 'noindex,follow'}]]
    }

    const title = getTitle(context)
    const description = getDescription(context, locale)
    const canonicalUrl = getUrl(getRoutePath(context.pageData.relativePath))
    const isHome = context.pageData.frontmatter.layout === 'home'
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
        ['meta', {property: 'og:image', content: DEFAULT_IMAGE}],
        ['meta', {property: 'og:locale', content: LOCALES[locale].hreflang.replace('-', '_')}],
        ...localeAlternates.map(item => ['meta', {property: 'og:locale:alternate', content: item.hreflang.replace('-', '_')}] as HeadConfig),
        ['meta', {name: 'twitter:card', content: 'summary_large_image'}],
        ['meta', {name: 'twitter:title', content: title}],
        ['meta', {name: 'twitter:description', content: description}],
        ['meta', {name: 'twitter:image', content: DEFAULT_IMAGE}],
        ['script', {type: 'application/ld+json'}, getStructuredData(context, locale, canonicalUrl, title, description)]
    ]
}
