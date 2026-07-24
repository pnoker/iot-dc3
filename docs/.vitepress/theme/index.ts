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
import {h, onMounted} from 'vue'
import DefaultTheme from 'vitepress/theme'
import mediumZoom from 'medium-zoom'
import 'medium-zoom/dist/style.css'
import './style.css'
import TopologyDiagram from './components/TopologyDiagram.vue'
import LayeredDiagram from './components/LayeredDiagram.vue'
import DataPlaneDiagram from './components/DataPlaneDiagram.vue'
import CommandPlaneDiagram from './components/CommandPlaneDiagram.vue'
import AuthFlowDiagram from './components/AuthFlowDiagram.vue'
import FourLayersDiagram from './components/FourLayersDiagram.vue'
import HeroLogo from './components/HeroLogo.vue'
import HeroParticles from './components/HeroParticles.vue'
import HeroWaves from './components/HeroWaves.vue'
import VersionSwitcher from './components/VersionSwitcher.vue'
import VersionBanner from './components/VersionBanner.vue'

// The language preference is written back to localStorage after internal navigation (including the top language switcher), so the head inline script can keep it on refresh.
function persistLang(path: string) {
    try {
        localStorage.setItem('dc3-lang', path.indexOf('/en') === 0 ? 'en' : 'zh')
    } catch (e) {
        // silently skip when localStorage is unavailable
    }
}

const theme: Theme = {
    extends: DefaultTheme,

    // Home hero: two background layers (HeroWaves wave dot-matrix at the bottom + full-width converging particles HeroParticles above it, home-hero-before), and the image area is the logo particle animation (home-hero-image)
    Layout() {
        return h(DefaultTheme.Layout, null, {
            'layout-top': () => h(VersionBanner),
            'home-hero-before': () => [h(HeroWaves), h(HeroParticles)],
            'home-hero-image': () => h(HeroLogo),
            'nav-bar-content-after': () => h(VersionSwitcher)
        })
    },

    setup() {
        onMounted(() => {
            mediumZoom('.vp-doc img:not(.no-zoom):not(.dc3-diagram img):not(.dc3-mermaid img)', {
                background: 'rgba(0, 0, 0, 0.78)',
                margin: 24
            })
        })
    },

    enhanceApp({app, router}) {
        app.component('TopologyDiagram', TopologyDiagram)
        app.component('LayeredDiagram', LayeredDiagram)
        app.component('DataPlaneDiagram', DataPlaneDiagram)
        app.component('CommandPlaneDiagram', CommandPlaneDiagram)
        app.component('AuthFlowDiagram', AuthFlowDiagram)
        app.component('FourLayersDiagram', FourLayersDiagram)
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
