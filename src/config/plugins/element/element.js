import Vue from 'vue';

import Element from 'element-ui';
import 'element-ui/lib/theme-chalk/index.css';
import './element-variables.scss';

import locale from 'element-ui/lib/locale/lang/zh-CN';
import GeminiScrollbar from 'vue-gemini-scrollbar';
import Sparkline from 'vue-sparklines';

Vue.use(Element, {locale});
Vue.use(GeminiScrollbar);
Vue.use(Sparkline);
