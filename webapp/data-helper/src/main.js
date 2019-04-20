import Vue from 'vue'
import App from './App.vue'

import router from './resources/router'
import store from './resources/store'
//Element UI
import ElementUI from 'element-ui';
import locale from 'element-ui/lib/locale/lang/zh-CN'
import 'element-ui/lib/theme-chalk/index.css';
//Axios
import axios from 'axios'
import VueAxios from 'vue-axios'

Vue.config.productionTip = false

Vue.use(ElementUI, {locale});
Vue.use(VueAxios, axios);

new Vue({
    router,
    store,
    render: h => h(App)
}).$mount('#app')