const interval = {
    state: {
        pointValueInterval: null
    },
    actions: {
        ClearPointValueInterval({commit}, interval) {
            commit('CLEAR_POINT_VALUE_INTERVAL', interval);
        }
    },
    mutations: {
        CLEAR_POINT_VALUE_INTERVAL: (state, interval) => {
            if (state.pointValueInterval) {
                clearInterval(state.pointValueInterval);
            }
            state.pointValueInterval = interval;
        }
    }
};

export default interval
