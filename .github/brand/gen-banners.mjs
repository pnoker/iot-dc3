// DC3 品牌 banner 多语言生成器:
//   banner.template.svg(设计源,占位符 {{TAGLINE}}/{{TECHTAGS}}) × LANGS 语言表
//   → banner.<lang>.svg + banner.<lang>.png(@2x,3200×840,retina 锐利)
//
// 运行: node .github/brand/gen-banners.mjs
// 依赖 playwright:本仓库是 Java 工程、无 JS 依赖,脚本会尝试从兄弟仓库
//   ../iot-dc3-online/node_modules 解析(iot-dc3-online 的 devDependencies 已含 playwright);
//   也可用 BANNER_PLAYWRIGHT_DIR 环境变量指定其他 node_modules 路径。
import {createRequire} from 'node:module'
import {existsSync} from 'node:fs'
import {fileURLToPath} from 'node:url'
import {dirname, join} from 'node:path'

const here = dirname(fileURLToPath(import.meta.url))

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
  return readFile(join(here, 'banner.template.svg'), 'utf-8')
}()

for (const [lang, {tagline, techtags}] of Object.entries(LANGS)) {
  const svg = template.replaceAll('{{TAGLINE}}', tagline).replaceAll('{{TECHTAGS}}', techtags)
  await (await import('node:fs/promises')).writeFile(join(here, `banner.${lang}.svg`), svg)
}

const {chromium} = loadPlaywright()
const browser = await chromium.launch()
for (const lang of Object.keys(LANGS)) {
  const page = await browser.newPage({viewport: {width: 1600, height: 420}, deviceScaleFactor: 2})
  await page.goto('file://' + join(here, `banner.${lang}.svg`))
  await page.screenshot({
    path: join(here, `banner.${lang}.png`),
    clip: {x: 0, y: 0, width: 1600, height: 420}
  })
  await page.close()
  console.log(`generated banner.${lang}.svg / banner.${lang}.png`)
}
await browser.close()
console.log('done: 7 languages')
