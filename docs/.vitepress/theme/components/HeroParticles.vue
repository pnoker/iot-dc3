<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  -
  - This program is free software: you can redistribute it and/or modify
  - it under the terms of the GNU Affero General Public License as
  - published by the Free Software Foundation, either version 3 of the
  - License, or (at your option) any later version.
  -
  - This program is distributed in the hope that it will be useful,
  - but WITHOUT ANY WARRANTY; without even the implied warranty of
  - MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  - GNU Affero General Public License for more details.
  -
  - You should have received a copy of the GNU Affero General Public License
  - along with this program.  If not, see <https://www.gnu.org/licenses/>.
  -->
<script lang="ts" setup>
import {onBeforeUnmount, onMounted} from 'vue'

interface P {
  ang: number   // polar angle relative to the anchor
  dist: number  // distance to the anchor (px)
  seed: number  // spawn distance, used for fade-in
  spin: number  // angular velocity (clockwise, rad/s)
  vin: number   // inward velocity (px/s)
  r: number
  alpha: number
}

let raf = 0
let ctx: CanvasRenderingContext2D | null = null
let cv: HTMLCanvasElement | null = null
let hero: HTMLElement | null = null
let dpr = 1, w = 0, h = 0
let ax = 0, ay = 0, ar = 60   // convergence anchor (logo center) and the final merge radius
let last = 0, frames = 0
let running = false, reduced = false
let ps: P[] = []

function rand(a: number, b: number) {
  return a + Math.random() * (b - a)
}

// anchor = the logo center relative to the hero; falls back to the right side of the hero before the logo is ready
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
  // portrait (phone): the hero is vertical with the logo in the upper half — merge in from a ring around the logo, focused on the upper half, without a long trip across the text
  if (h > w) {
    const d = rand(1.5, 2.4) * ar
    return {
      ang: rand(0, Math.PI * 2),
      dist: d,
      seed: d,
      spin: rand(0.2, 0.4),
      vin: rand(32, 60),
      r: rand(1.4, 3.2),
      alpha: 0
    }
  }
  // landscape (desktop): spawn from the hero's four outer edges (all directions); particles from the left cross the text background on their way to the logo on the right
  let x = 0, y = 0
  const m = 24
  const e = Math.random() * 4 | 0
  if (e === 0) {
    x = rand(0, w);
    y = -m
  } else if (e === 1) {
    x = w + m;
    y = rand(0, h)
  } else if (e === 2) {
    x = rand(0, w);
    y = h + m
  } else {
    x = -m;
    y = rand(0, h)
  }
  const dx = x - ax, dy = y - ay
  const d = Math.hypot(dx, dy)
  return {
    ang: Math.atan2(dy, dx),
    dist: d,
    seed: d,
    spin: rand(0.14, 0.3),   // clockwise
    vin: rand(70, 120),
    r: rand(1.4, 3.6),
    alpha: 0
  }
}

function build() {
  const n = h > w ? 14 : Math.max(32, Math.min(70, Math.round(w * h / 12000)))
  ps = Array.from({length: n}, spawn)
  // scatter the initial progress so particles start spread along the route rather than all marching from the edge at once
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
  if (dt > 0.05) dt = 0.05   // clamp after returning from background / dropped frames
  if ((frames++ % 12) === 0) measureAnchor()   // periodically follow the logo position (cheap)
  ctx.clearRect(0, 0, w, h)
  const maxD = Math.hypot(w, h)
  for (const p of ps) {
    p.dist -= p.vin * dt   // converge inward
    p.ang += p.spin * dt   // rotate clockwise
    if (p.dist <= ar) {    // reached the logo's outer edge → merge and vanish, respawn from the edge
      Object.assign(p, spawn())
      continue
    }
    const prox = Math.max(0, Math.min(1, 1 - p.dist / (0.6 * maxD)))  // near the logo → 1
    const inF = Math.max(0, Math.min(1, (p.seed - p.dist) / 100))     // fade in after spawn
    const outF = Math.max(0, Math.min(1, (p.dist - ar) / 70))         // fade out as it nears the logo
    // text area (far) fades to ~0.16, around the logo (near) rises to ~0.6: spread out yet not blocking text
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
  // guard against duplicates (HMR / remount)
  hero.querySelectorAll('.hero-particles-canvas').forEach(n => n.remove())
  cv = document.createElement('canvas')
  cv.className = 'hero-particles-canvas'
  cv.style.cssText = 'position:absolute;inset:0;width:100%;height:100%;z-index:1;pointer-events:none'
  hero.insertBefore(cv, hero.firstChild)
  resize()
  measureAnchor()
  if (reduced) return   // respect prefers-reduced-motion: inject but do not animate
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
  // the VPHero may enter the DOM a beat later than this component; retry for a few frames until it appears
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
  <span aria-hidden="true" class="hero-particles-mount" style="display:none"/>
</template>
