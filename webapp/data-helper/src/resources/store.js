import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

export default new Vuex.Store({
    state: {
        navIndex: '/'
    },
    mutations: {
        handleSelect(state, index) {
            state.navIndex = index;
        }
    }
})
