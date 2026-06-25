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

import type {Theme} from 'vitepress'
import {h} from 'vue'
import DefaultTheme from 'vitepress/theme'
import './style.css'
import Architecture from './components/Architecture.vue'
import HeroLogo from './components/HeroLogo.vue'
import HeroParticles from './components/HeroParticles.vue'
import HeroWaves from './components/HeroWaves.vue'

// 语言偏好在内部导航（含顶部语言切换器）后写回 localStorage，刷新时由 head 内联脚本据此保持。
function persistLang(path: string) {
    try {
        localStorage.setItem('dc3-lang', path.indexOf('/en') === 0 ? 'en' : 'zh')
    } catch (e) {
        // localStorage 不可用时静默跳过
    }
}

const theme: Theme = {
    extends: DefaultTheme,

    // 首页 hero：背景两层（最底波浪点阵 HeroWaves + 其上全幅汇聚粒子 HeroParticles，home-hero-before），图区是 logo 粒子动效（home-hero-image）
    Layout() {
        return h(DefaultTheme.Layout, null, {
            'home-hero-before': () => [h(HeroWaves), h(HeroParticles)],
            'home-hero-image': () => h(HeroLogo)
        })
    },

    enhanceApp({app, router}) {
        app.component('Architecture', Architecture)
        if (typeof window === 'undefined' || !router) {
            return
        }
        persistLang(window.location.pathname)
        const previous = router.onAfterRouteChanged
        router.onAfterRouteChanged = function (to) {
            persistLang(to)
            return previous ? previous.call(this, to) : undefined
        }
    }
}

export default theme
