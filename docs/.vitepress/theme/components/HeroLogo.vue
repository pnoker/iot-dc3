<!--
  Copyright 2016-present the IoT DC3 original author or authors.
  Licensed under the GNU Affero General Public License v3.0.

  Hero logo for the home page: a faithful rebuild of logo.svg — its 54 dots, the solid
  #1296db color, and their exact positions and radii, drawn on a canvas so it matches the
  original logo pixel for pixel. Only the dots themselves come "alive": a clockwise,
  inward-converging coherent wave flows along the three spiral arms, giving each dot a
  gentle tangential sway plus a very faint shimmer — echoing the logo's "swirl converging,
  everything connected" form. The whole image never rotates, never breathes by scaling, and
  carries no stray particles or glow haze. Any still frame equals logo.svg. SSR-safe (the
  canvas only starts on the client).
-->
<script setup lang="ts">
import {onBeforeUnmount, onMounted, ref} from 'vue'
import {LOGO_POINTS} from './logo-points'

const canvas = ref<HTMLCanvasElement | null>(null)
const root = ref<HTMLDivElement | null>(null)

// logo.svg base color: solid #1296db. At wave crests the dots shimmer faintly toward a
// slightly brighter blue; at troughs they return to the base color — the base is the floor, kept clean.
const BASE = [18, 150, 219]    // #1296db
const CREST = [56, 169, 230]   // #38a9e6

// Maximum radius of any dot from the center, used to scale the layout.
const LOGO_R = Math.max(...LOGO_POINTS.map(p => Math.hypot(p.x, p.y) + p.r))

interface Node {
  x: number    // steady-state position (relative to center, in viewBox units)
  y: number
  r: number
  ang: number  // polar angle
  spiral: number  // phase along the spiral arm: neighbors share a similar phase, so the wave flows coherently rather than as scattered noise
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
      // angle + radial term form the spiral phase; 2.2 turns yield roughly two shimmer bands along the arms
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
  // the logo fills about 92% of the container, leaving a thin margin
  scale = (Math.min(w, h) * 0.46) / LOGO_R
}

function paint(t: number) {
  if (!ctx) return
  ctx.clearRect(0, 0, cx * 2, cy * 2)
  const omega = 0.6   // wave speed (rad/s); slow and calm
  const amp = 4.5     // tangential sway (viewBox units); gentle, to keep the logo crisp
  for (const nd of nodes) {
    // coherent wave phase: +t pulls crests inward (converging), flowing along the three spiral arms
    const wave = Math.sin(nd.spiral + t * omega)
    // tangential unit vector (clockwise); each dot sways slightly along it → swirling flow, not whole-image rotation
    const tx = -Math.sin(nd.ang), ty = Math.cos(nd.ang)
    const x = cx + (nd.x + tx * wave * amp) * scale
    const y = cy + (nd.y + ty * wave * amp) * scale
    const rr = nd.r * scale * (1 + 0.022 * wave)
    // faint shimmer: only the crest side shifts from base toward bright blue; troughs stay at #1296db
    const k = 0.13 * Math.max(0, wave)
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
  // reduced: paint one static frame (equal to logo.svg) and stop
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

/* A very faint background glow so the large logo doesn't sit harshly on white; unrelated to the dots, it never pollutes the logo. */
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
