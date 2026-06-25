<!--
  Copyright 2016-present the IoT DC3 original author or authors.
  Licensed under the GNU Affero General Public License v3.0.

  首页 hero 背景粒子场：铺满整个 .VPHero（含左侧标题文字背景），粒子从四面八方外缘
  沿顺时针螺旋向心汇聚到 logo，呼应「万物互联、节点从四方汇聚成网」。
  canvas 垫在 hero 内容之下（z-index 0、pointer-events:none），文字/logo 浮在其上；
  粒子在文字区做淡、在 logo 周围渐明显——既铺得开又不挡阅读。纯色 #1296db、无辉光雾。
  SSR 安全（canvas 仅客户端创建并注入）。
-->
<script setup lang="ts">
import {onBeforeUnmount, onMounted} from 'vue'

interface P {
  ang: number   // 相对锚点的极角
  dist: number  // 到锚点距离（px）
  seed: number  // 生成时距离，用于淡入
  spin: number  // 角速度（顺时针，rad/s）
  vin: number   // 向心速度（px/s）
  r: number
  alpha: number
}

let raf = 0
let ctx: CanvasRenderingContext2D | null = null
let cv: HTMLCanvasElement | null = null
let hero: HTMLElement | null = null
let dpr = 1, w = 0, h = 0
let ax = 0, ay = 0, ar = 60   // 汇聚锚点（logo 中心）与汇入终点半径
let last = 0, frames = 0
let running = false, reduced = false
let ps: P[] = []

function rand(a: number, b: number) {
  return a + Math.random() * (b - a)
}

// 锚点 = logo 中心相对 hero 的位置；logo 未就绪时回退到 hero 右侧
function measureAnchor() {
  if (!hero) return
  const hr = hero.getBoundingClientRect()
  const logo = hero.querySelector('.hero-logo') as HTMLElement | null
  if (logo) {
    const lr = logo.getBoundingClientRect()
    ax = lr.left - hr.left + lr.width / 2
    ay = lr.top - hr.top + lr.height / 2
    ar = Math.min(lr.width, lr.height) * 0.44
  } else {
    ax = w * 0.74
    ay = h * 0.5
    ar = Math.min(w, h) * 0.22
  }
}

function spawn(): P {
  // 从 hero 四边外缘生成（四面八方）；左侧来的粒子会穿过文字背景汇向右侧 logo
  let x = 0, y = 0
  const m = 24
  const e = Math.random() * 4 | 0
  if (e === 0) { x = rand(0, w); y = -m }
  else if (e === 1) { x = w + m; y = rand(0, h) }
  else if (e === 2) { x = rand(0, w); y = h + m }
  else { x = -m; y = rand(0, h) }
  const dx = x - ax, dy = y - ay
  const d = Math.hypot(dx, dy)
  return {
    ang: Math.atan2(dy, dx),
    dist: d,
    seed: d,
    spin: rand(0.2, 0.45),   // 顺时针
    vin: rand(120, 200),
    r: rand(1.4, 3.6),
    alpha: 0
  }
}

function build() {
  const n = Math.max(40, Math.min(90, Math.round(w * h / 10000)))
  ps = Array.from({length: n}, spawn)
  // 打散初始进度，让粒子一开始就铺在途中各处，而非齐刷刷从边缘出发
  for (const p of ps) p.dist *= rand(0.4, 1)
}

function resize() {
  if (!hero || !cv) return
  dpr = Math.min(window.devicePixelRatio || 1, 2)
  w = hero.clientWidth
  h = hero.clientHeight
  cv.width = Math.round(w * dpr)
  cv.height = Math.round(h * dpr)
  ctx = cv.getContext('2d')
  if (ctx) ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
}

function frame(now: number) {
  if (!running || !ctx) return
  if (!last) last = now
  let dt = (now - last) / 1000
  last = now
  if (dt > 0.05) dt = 0.05   // 切后台回来/掉帧钳制
  if ((frames++ % 12) === 0) measureAnchor()   // 周期性跟随 logo 位置（便宜）
  ctx.clearRect(0, 0, w, h)
  const maxD = Math.hypot(w, h)
  for (const p of ps) {
    p.dist -= p.vin * dt   // 向心汇入
    p.ang += p.spin * dt   // 顺时针旋转
    if (p.dist <= ar) {    // 抵达 logo 外缘 → 融入消失，从边缘重生
      Object.assign(p, spawn())
      continue
    }
    const prox = Math.max(0, Math.min(1, 1 - p.dist / (0.6 * maxD)))  // 近 logo→1
    const inF = Math.max(0, Math.min(1, (p.seed - p.dist) / 100))     // 生成后淡入
    const outF = Math.max(0, Math.min(1, (p.dist - ar) / 70))         // 临近 logo 淡出
    // 文字区（远）淡至 ~0.16，logo 周围（近）升至 ~0.6：铺得开又不挡字
    p.alpha = (0.16 + 0.44 * prox) * inF * outF
    const x = ax + Math.cos(p.ang) * p.dist
    const y = ay + Math.sin(p.ang) * p.dist
    ctx.globalAlpha = p.alpha
    ctx.beginPath()
    ctx.fillStyle = '#1296db'
    ctx.arc(x, y, p.r, 0, Math.PI * 2)
    ctx.fill()
  }
  ctx.globalAlpha = 1
  raf = requestAnimationFrame(frame)
}

function start() {
  if (running) return
  running = true
  raf = requestAnimationFrame(frame)
}

function stop() {
  running = false
  if (raf) cancelAnimationFrame(raf)
  raf = 0
}

function onVis() {
  if (document.hidden) stop()
  else {
    last = 0
    start()
  }
}

let ro: ResizeObserver | null = null

function init() {
  hero = document.querySelector('.VPHero') as HTMLElement | null
  if (!hero) return
  hero.style.position = 'relative'
  // 防重复（HMR / 重挂载）
  hero.querySelectorAll('.hero-particles-canvas').forEach(n => n.remove())
  cv = document.createElement('canvas')
  cv.className = 'hero-particles-canvas'
  cv.style.cssText = 'position:absolute;inset:0;width:100%;height:100%;z-index:1;pointer-events:none'
  hero.insertBefore(cv, hero.firstChild)
  resize()
  measureAnchor()
  if (reduced) return   // 尊重 prefers-reduced-motion：注入但不动
  build()
  ro = new ResizeObserver(() => {
    resize()
    measureAnchor()
  })
  ro.observe(hero)
  document.addEventListener('visibilitychange', onVis)
  start()
}

onMounted(() => {
  reduced = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
  // VPHero 可能比本组件晚一拍进入 DOM，重试若干帧直到出现
  let tries = 0
  const tick = () => {
    if (document.querySelector('.VPHero')) {
      init()
      return
    }
    if (tries++ < 20) requestAnimationFrame(tick)
  }
  tick()
})

onBeforeUnmount(() => {
  stop()
  ro?.disconnect()
  document.removeEventListener('visibilitychange', onVis)
  cv?.remove()
  cv = null
  ctx = null
  hero = null
})
</script>

<template>
  <span class="hero-particles-mount" aria-hidden="true" style="display:none"/>
</template>
