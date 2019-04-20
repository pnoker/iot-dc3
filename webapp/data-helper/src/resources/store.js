import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

export default new Vuex.Store({
    state: {
        navIndex: '/'
    },
    mutations: {
        changeNavSelect(state, index) {
            state.navIndex = index;
        }
    }
})
