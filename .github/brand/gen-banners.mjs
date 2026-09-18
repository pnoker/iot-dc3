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

// DC3 brand image generator:
//   svg/banner.template.svg (design source, {{TAGLINE}}/{{TECHTAGS}} placeholders) x LANGS language table
//   -> svg/banner.<lang>.svg + png/banner.<lang>.png (@2x, 3200x840, retina sharp)
//   svg/social-preview.svg -> png/social-preview.png (1200x600)
//
// Run: node .github/brand/gen-banners.mjs
// Playwright dependency: this repo is a Java project with no JS deps, so the script
//   resolves from the sibling ../iot-dc3-online/node_modules (its devDependencies include playwright);
//   or point BANNER_PLAYWRIGHT_DIR at another node_modules path.
import {createRequire} from 'node:module'
import {existsSync} from 'node:fs'
import {fileURLToPath} from 'node:url'
import {dirname, join} from 'node:path'

const here = dirname(fileURLToPath(import.meta.url))
const svgDir = join(here, 'svg')
const pngDir = join(here, 'png')

// 各语言文案(与仓库根各 README 的 hero 主标语保持同一译法;SENSE–DECIDE–ACT 为品牌术语,各语言保留原文)
const LANGS = {
    zh: {
        tagline: '多协议 · 云原生 · 开源工业物联网平台',
        techtags: '28+ 协议驱动 · 边云协同 · SENSE–DECIDE–ACT'
    },
    en: {
        tagline: 'Multi-protocol · Cloud-native · Open-source Industrial IoT Platform',
        techtags: '28+ PROTOCOL DRIVERS · EDGE-CLOUD · SENSE–DECIDE–ACT'
    },
    ja: {
        tagline: 'マルチプロトコル · クラウドネイティブ · オープンソース産業 IoT プラットフォーム',
        techtags: '28+ プロトコルドライバー · エッジ・クラウド協働 · SENSE–DECIDE–ACT'
    },
    ko: {
        tagline: '멀티 프로토콜 · 클라우드 네이티브 · 오픈소스 산업용 IoT 플랫폼',
        techtags: '28+ 프로토콜 드라이버 · 엣지-클라우드 · SENSE–DECIDE–ACT'
    },
    es: {
        tagline: 'Multiprotocolo · Nativa de la nube · Plataforma de IoT Industrial de Código Abierto',
        techtags: '28+ CONTROLADORES · BORDE-NUBE · SENSE–DECIDE–ACT'
    },
    ru: {
        tagline: 'Мультипротокольная · Облачно-нативная · Открытая платформа промышленного IoT',
        techtags: '28+ ДРАЙВЕРОВ · EDGE-CLOUD · SENSE–DECIDE–ACT'
    },
    vi: {
        tagline: 'Đa giao thức · Cloud-native · Nền tảng IoT Công nghiệp Mã nguồn Mở',
        techtags: '28+ DRIVER GIAO THỨC · EDGE-CLOUD · SENSE–DECIDE–ACT'
    }
}

function loadPlaywright() {
    const candidates = [
        process.env.BANNER_PLAYWRIGHT_DIR,
        join(here, '..', '..', '..', 'iot-dc3-online', 'node_modules')
    ].filter(Boolean)
    for (const root of candidates) {
        // createRequire 基准要放在包内部(pnpm 符号链接结构下,从 node_modules 目录本身解析会失败)
        const pkg = join(root, 'playwright', 'package.json')
        if (!existsSync(pkg)) continue
        try {
            return createRequire(pkg)('playwright')
        } catch {
            // try next candidate
        }
    }
    throw new Error('playwright 不可用:请在 iot-dc3-online 中安装依赖,或设置 BANNER_PLAYWRIGHT_DIR 指向含 playwright 的 node_modules')
}

const template = await async function () {
    const {readFile} = await import('node:fs/promises')
    return readFile(join(svgDir, 'banner.template.svg'), 'utf-8')
}()

for (const [lang, {tagline, techtags}] of Object.entries(LANGS)) {
    const svg = template.replaceAll('{{TAGLINE}}', tagline).replaceAll('{{TECHTAGS}}', techtags)
    await (await import('node:fs/promises')).writeFile(join(svgDir, `banner.${lang}.svg`), svg)
}

const {chromium} = loadPlaywright()
const browserExecutable = [
    process.env.BANNER_BROWSER_PATH,
    'C:/Program Files/Google/Chrome/Application/chrome.exe',
    'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
    '/usr/bin/google-chrome',
    '/usr/bin/chromium',
    '/usr/bin/chromium-browser'
].filter(Boolean).find(existsSync)
const browser = await chromium.launch(browserExecutable ? {executablePath: browserExecutable} : {})

async function assertTextWithinSafeArea(page, label) {
    const overflows = await page.$$eval('text[data-min-x], text[data-max-x]', nodes => nodes.flatMap(node => {
        const box = node.getBoundingClientRect()
        const left = box.left
        const right = box.right
        const minX = node.dataset.minX === undefined ? Number.NEGATIVE_INFINITY : Number(node.dataset.minX)
        const maxX = node.dataset.maxX === undefined ? Number.POSITIVE_INFINITY : Number(node.dataset.maxX)
        return left < minX - 0.5 || right > maxX + 0.5
            ? [{text: node.textContent, left, right, minX, maxX}]
            : []
    }))
    if (overflows.length > 0) {
        throw new Error(`${label} text exceeds its safe area: ${JSON.stringify(overflows)}`)
    }
}

for (const lang of Object.keys(LANGS)) {
    const page = await browser.newPage({viewport: {width: 1600, height: 420}, deviceScaleFactor: 2})
    await page.goto('file://' + join(svgDir, `banner.${lang}.svg`))
    await assertTextWithinSafeArea(page, `banner.${lang}`)
    await page.screenshot({
        path: join(pngDir, `banner.${lang}.png`),
        clip: {x: 0, y: 0, width: 1600, height: 420}
    })
    await page.close()
    console.log(`generated svg/banner.${lang}.svg / png/banner.${lang}.png`)
}

const socialPage = await browser.newPage({viewport: {width: 1200, height: 600}})
await socialPage.goto('file://' + join(svgDir, 'social-preview.svg'))
await assertTextWithinSafeArea(socialPage, 'social preview')
await socialPage.screenshot({
    path: join(pngDir, 'social-preview.png'),
    clip: {x: 0, y: 0, width: 1200, height: 600}
})
await socialPage.close()
console.log('generated svg/social-preview.svg / png/social-preview.png')

await browser.close()
console.log('done: 7 banner languages and social preview')
