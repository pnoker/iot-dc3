<!--
  Copyright 2016-present the IoT DC3 original author or authors.
  Licensed under the GNU Affero General Public License v3.0.

  Bottom-most hero background: a 2D-canvas faux-perspective "wave dot-matrix ground plane" —
  a field of blue dots receding into the distance and undulating slowly, near-large/far-small,
  far-faint/near-bright. It echoes the tech-feel wave background of the front-end login page,
  but rendered in plain 2D canvas, sharing the stack with the hero's existing converging-particle
  layer and logo layer. It sits at the very bottom (z-index 0); text/logo/particles all float above.
  Solid #1296db at low opacity, never stealing focus. SSR-safe (the canvas is created and injected on the client only).
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

const COLS = 46     // dots across
const ROWS = 24     // depth rows
const SPD = 0.3     // wave speed

function paint(t: number) {
  if (!ctx) return
  ctx.clearRect(0, 0, w, h)
  const cx = w / 2
  // portrait (phone): the hero is vertical with text in the lower half — keep the wave plane up in the logo area, off the text
  const portrait = h > w
  const horizon = (portrait ? 0.06 : 0.32) * h   // height of the horizon (far end)
  const bottom = (portrait ? 0.46 : 1) * h        // bottom edge of the waves (near end)
  const amp = (portrait ? 0.03 : 0.04) * h        // wave height
  for (let j = 0; j < ROWS; j++) {
    const jn = j / (ROWS - 1)          // 0 far → 1 near
    const persp = Math.pow(jn, 1.7)    // unfold quickly up close, crowd toward the horizon far away (perspective)
    const rowY = horizon + (bottom - horizon) * persp
    const scaleX = 0.12 + 0.95 * persp // narrow horizontally in the distance
    const size = 0.5 + 2.4 * persp     // far-small, near-large
    const rowAlpha = 0.10 + 0.42 * persp
    for (let i = 0; i < COLS; i++) {
      const xn = i / (COLS - 1) - 0.5
      // slow undulation from a row wave plus a column wave
      const waveH = amp * (Math.sin(i * 0.18 + t * SPD + j * 0.5) + Math.cos(j * 0.42 + t * SPD))
      const sx = cx + xn * w * scaleX
      const sy = rowY - waveH * persp
      const edge = 1 - Math.min(1, Math.abs(xn) / 0.5)   // fade out at the horizontal edges to avoid a hard border
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
    paint(0)   // respect reduced-motion: paint one static frame
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
