<!--
  Copyright 2016-present the IoT DC3 original author or authors.
  Licensed under the GNU Affero General Public License v3.0.

  首页 hero 最底层背景：2D Canvas 伪透视「波浪点阵地平面」——一片向远端延伸、缓慢起伏的
  蓝色点阵，近大远小、远淡近显，呼应前端登录页那种科技感波浪背景，但用纯 2D canvas 实现，
  与 hero 现有的汇聚粒子层 / logo 层同一技术栈。垫在最底层（z-index 0），文字/logo/汇聚都在其上。
  纯色 #1296db、低透明度，不抢主体。SSR 安全（canvas 仅客户端创建注入）。
-->
<script setup lang="ts">
import {onBeforeUnmount, onMounted} from 'vue'

let raf = 0
let ctx: CanvasRenderingContext2D | null = null
let cv: HTMLCanvasElement | null = null
let hero: HTMLElement | null = null
let dpr = 1, w = 0, h = 0
let t0 = 0
let running = false, reduced = false

const COLS = 46     // 横向点数
const ROWS = 24     // 纵深行数
const SPD = 0.5     // 波速

function paint(t: number) {
  if (!ctx) return
  ctx.clearRect(0, 0, w, h)
  const cx = w / 2
  const horizon = h * 0.32     // 地平线（远端）所在高度
  const amp = h * 0.045        // 波高
  for (let j = 0; j < ROWS; j++) {
    const jn = j / (ROWS - 1)          // 0 远 → 1 近
    const persp = Math.pow(jn, 1.7)    // 近处快速展开到底部、远处挤向地平线（透视）
    const rowY = horizon + (h - horizon) * persp
    const scaleX = 0.12 + 0.95 * persp // 远处横向收窄
    const size = 0.5 + 2.4 * persp     // 远小近大
    const rowAlpha = 0.10 + 0.42 * persp
    for (let i = 0; i < COLS; i++) {
      const xn = i / (COLS - 1) - 0.5
      // 行波 + 列波叠加的缓慢起伏
      const waveH = amp * (Math.sin(i * 0.18 + t * SPD + j * 0.5) + Math.cos(j * 0.42 + t * SPD))
      const sx = cx + xn * w * scaleX
      const sy = rowY - waveH * persp
      const edge = 1 - Math.min(1, Math.abs(xn) / 0.5)   // 横向边缘淡出，避免硬边
      const a = rowAlpha * edge
      if (a <= 0.01) continue
      ctx.globalAlpha = a
      ctx.beginPath()
      ctx.fillStyle = '#1296db'
      ctx.arc(sx, sy, size, 0, Math.PI * 2)
      ctx.fill()
    }
  }
  ctx.globalAlpha = 1
}

function frame(now: number) {
  if (!running || !ctx) return
  if (!t0) t0 = now
  paint((now - t0) / 1000)
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
  else start()
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

let ro: ResizeObserver | null = null

function init() {
  hero = document.querySelector('.VPHero') as HTMLElement | null
  if (!hero) return
  hero.style.position = 'relative'
  hero.querySelectorAll('.hero-waves-canvas').forEach(n => n.remove())
  cv = document.createElement('canvas')
  cv.className = 'hero-waves-canvas'
  cv.style.cssText = 'position:absolute;inset:0;width:100%;height:100%;z-index:0;pointer-events:none'
  hero.insertBefore(cv, hero.firstChild)
  resize()
  if (reduced) {
    paint(0)   // 尊重 reduced-motion：画一帧静态
    return
  }
  ro = new ResizeObserver(resize)
  ro.observe(hero)
  document.addEventListener('visibilitychange', onVis)
  start()
}

onMounted(() => {
  reduced = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
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
  <span class="hero-waves-mount" aria-hidden="true" style="display:none"/>
</template>
