import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

export default new Vuex.Store({
    state: {
        nav: '/'
    },
    mutations: {
        handleSelect(state, index) {
            state.nav = index;
        }
    },
    getters:{
        getNav(state){
            return state.nav;
        }
    }
})
