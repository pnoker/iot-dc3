<!--
  Copyright 2016-present the IoT DC3 original author or authors.
  Licensed under the GNU Affero General Public License v3.0.

  首页 hero logo：忠实重建 logo.svg —— 它的 54 个圆点、纯色 #1296db、精确位置与半径，
  在 canvas 上画出来和原版 logo 一模一样。然后只让这些点本身「活」起来：
  一道顺时针、向心汇聚的相干波沿三条旋臂流动，带动每个点做小幅切向摆动 + 极轻的流光，
  呼应 logo「漩涡汇聚、万物互联」的形态。不旋转整张图、不整体缩放呼吸、不掺任何游离粒子或辉光雾。
  静止任一帧都等同 logo.svg。SSR 安全（canvas 仅客户端启动）。
-->
<script setup lang="ts">
import {onBeforeUnmount, onMounted, ref} from 'vue'
import {LOGO_POINTS} from './logo-points'

const canvas = ref<HTMLCanvasElement | null>(null)
const root = ref<HTMLDivElement | null>(null)

// logo.svg 原色：纯色 #1296db。波峰处向这个略亮的蓝做微弱流光，谷底回到原色——原色即下限，忠实不脏。
const BASE = [18, 150, 219]    // #1296db
const CREST = [56, 169, 230]   // #38a9e6

// 点到中心的最大半径，用于布局缩放
const LOGO_R = Math.max(...LOGO_POINTS.map(p => Math.hypot(p.x, p.y) + p.r))

interface Node {
  x: number    // 稳态位置（相对中心，viewBox 尺度）
  y: number
  r: number
  ang: number  // 极角
  spiral: number  // 沿旋臂的相位：邻近点相位相近，使波相干流动而非散点噪动
}

let raf = 0
let ctx: CanvasRenderingContext2D | null = null
let nodes: Node[] = []
let cx = 0, cy = 0, scale = 1, dpr = 1
let t0 = 0
let running = false
let reduced = false

function buildNodes() {
  nodes = LOGO_POINTS.map(p => {
    const dist = Math.hypot(p.x, p.y)
    return {
      x: p.x,
      y: p.y,
      r: p.r,
      ang: Math.atan2(p.y, p.x),
      // 角度 + 径向项构成螺旋相位；2.2 圈让旋臂上呈现约两道流光带
      spiral: Math.atan2(p.y, p.x) + (dist / LOGO_R) * Math.PI * 2.2
    }
  })
}

function resize() {
  const el = canvas.value, host = root.value
  if (!el || !host) return
  dpr = Math.min(window.devicePixelRatio || 1, 2)
  const w = host.clientWidth, h = host.clientHeight
  el.width = Math.round(w * dpr)
  el.height = Math.round(h * dpr)
  el.style.width = w + 'px'
  el.style.height = h + 'px'
  ctx = el.getContext('2d')
  if (ctx) ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  cx = w / 2
  cy = h / 2
  // logo 占容器约 92%，留薄边
  scale = (Math.min(w, h) * 0.46) / LOGO_R
}

function paint(t: number) {
  if (!ctx) return
  ctx.clearRect(0, 0, cx * 2, cy * 2)
  const omega = 1.0   // 波速（rad/s），缓慢
  const amp = 6       // 切向摆幅（viewBox 尺度），小幅以保持 logo 清晰
  for (const nd of nodes) {
    // 相干波相位：+t 使波峰向心收拢（汇聚），顺三旋臂流动
    const wave = Math.sin(nd.spiral + t * omega)
    // 切向单位向量（顺时针），点沿切向小幅摆动 → 漩涡流动感，非整图旋转
    const tx = -Math.sin(nd.ang), ty = Math.cos(nd.ang)
    const x = cx + (nd.x + tx * wave * amp) * scale
    const y = cy + (nd.y + ty * wave * amp) * scale
    const rr = nd.r * scale * (1 + 0.03 * wave)
    // 微弱流光：仅波峰侧从原色向亮蓝偏移，谷底保持 #1296db
    const k = 0.16 * Math.max(0, wave)
    const r = BASE[0] + (CREST[0] - BASE[0]) * k
    const g = BASE[1] + (CREST[1] - BASE[1]) * k
    const b = BASE[2] + (CREST[2] - BASE[2]) * k
    ctx.beginPath()
    ctx.fillStyle = `rgb(${r | 0},${g | 0},${b | 0})`
    ctx.arc(x, y, rr, 0, Math.PI * 2)
    ctx.fill()
  }
}

function frame(now: number) {
  if (!ctx || !running) return
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

function onVisibility() {
  if (document.hidden) stop()
  else start()
}

let ro: ResizeObserver | null = null

onMounted(() => {
  reduced = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
  buildNodes()
  resize()
  // reduced：画一帧静态 logo（等同 logo.svg）就停
  if (reduced) {
    paint(0)
    return
  }
  ro = new ResizeObserver(() => {
    resize()
    if (!running) paint(0)
  })
  if (root.value) ro.observe(root.value)
  document.addEventListener('visibilitychange', onVisibility)
  start()
})

onBeforeUnmount(() => {
  stop()
  ro?.disconnect()
  document.removeEventListener('visibilitychange', onVisibility)
})
</script>

<template>
  <div ref="root" class="hero-logo">
    <div class="hero-logo-glow"/>
    <canvas ref="canvas" class="hero-logo-canvas" aria-label="IoT DC3" role="img"/>
  </div>
</template>

<style scoped>
.hero-logo {
  position: relative;
  width: min(520px, 44vw);
  aspect-ratio: 1 / 1;
  margin: 0 auto;
  display: grid;
  place-items: center;
}

/* 极轻的背景辉光，避免大图贴在白底上显得突兀；与点本身无关，不污染 logo */
.hero-logo-glow {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: radial-gradient(circle at 50% 48%,
  rgba(18, 150, 219, 0.10) 0%,
  rgba(18, 150, 219, 0.04) 50%,
  rgba(18, 150, 219, 0) 72%);
}

.hero-logo-canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

@media (max-width: 768px) {
  .hero-logo {
    width: min(320px, 64vw);
  }
}
</style>
