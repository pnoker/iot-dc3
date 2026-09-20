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

// 各语言文案(与仓库根各 README 的 hero 主标语保持同一译法;口号为 dc3.site 官网 canonical slogan)
const LANGS = {
    zh: {
        tagline: '连接物理世界与 AI · 面向 Physical AI 的开源工业物联网 Runtime',
        techtags: '36 协议驱动 · MCP 工具网关 · 安全 · 可控 · 可追溯'
    },
    en: {
        tagline: 'Connect the Physical World to AI · Open-source Industrial IoT Runtime for Physical AI',
        techtags: '36 PROTOCOL DRIVERS · MCP TOOL GATEWAY · SAFE · CONTROLLABLE · TRACEABLE'
    },
    ja: {
        tagline: '物理世界と AI をつなぐ · Physical AI ためのオープンソース産業 IoT Runtime',
        techtags: '36 プロトコルドライバー · MCP ツールゲートウェイ · 安全・制御可能・追跡可能'
    },
    ko: {
        tagline: '물리 세계와 AI를 연결 · Physical AI를 위한 오픈소스 산업용 IoT Runtime',
        techtags: '36 프로토콜 드라이버 · MCP 툴 게이트웨이 · 안전 · 통제 가능 · 추적 가능'
    },
    es: {
        tagline: 'Conecta el mundo físico con la IA · Industrial IoT Runtime de código abierto para Physical AI',
        techtags: '36 CONTROLADORES · PUERTA DE ENLACE MCP · SEGURO · CONTROLABLE · TRAZABLE'
    },
    ru: {
        tagline: 'Соединяем физический мир с ИИ · Открытый Industrial IoT Runtime для Physical AI',
        techtags: '36 ДРАЙВЕРОВ · MCP-ШЛЮЗ ИНСТРУМЕНТОВ · БЕЗОПАСНО · УПРАВЛЯЕМО · ПРОСЛЕЖИВАЕМО'
    },
    vi: {
        tagline: 'Kết nối thế giới vật lý với AI · Industrial IoT Runtime mã nguồn mở cho Physical AI',
        techtags: '36 DRIVER GIAO THỨC · CỔNG CÔNG CỤ MCP · AN TOÀN · KIỂM SOÁT · TRUY VẾT'
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
