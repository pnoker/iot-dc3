import Vue from 'vue'
import Axios from 'vue-axios'
import axios from '@/config/axios'
import router from '@/config/router'
import store from '@/config/store'
import '@/config/plugins/index'

import App from './App'

Vue.use(Axios, axios);

Vue.config.productionTip = false;

new Vue({
    router,
    store,
    render: h => h(App)
}).$mount('#app');
