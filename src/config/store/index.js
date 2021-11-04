import Vue from 'vue';
import Vuex from 'vuex';
import token from '@/config/store/modules/token';
import interval from '@/config/store/modules/interval';

Vue.use(Vuex);

const index = new Vuex.Store({
    modules: {
        token,
        interval,
    }
});

export default index
