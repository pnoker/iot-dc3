import axios from 'axios';
import router from '@/config/router';
import store from '@/config/store';

//跨域请求，允许保存cookie
axios.defaults.withCredentials = true;

//返回其他状态码
axios.defaults.validateStatus = function (status) {
    return status >= 200 && status <= 500;
};

//HTTP Request拦截
axios.interceptors.request.use(config => {
    config.headers['Content-Type'] = 'application/json';
    return config;
}, error => {
    console.error('Request interceptors:', error);
    return Promise.reject(error)
});

//HTTP Response拦截
axios.interceptors.response.use(res => {
    const ok = res.data.ok || false, status = res.status || 200, message = res.data.message || 'Internal Server Error!';

    if (!ok) {
        if (status === 401) {
            store.dispatch('ClearToken').then(() => router.push({path: '/login'}));
        }
        return Promise.reject(new Error(message));
    }

    return res.data;
}, error => {
    console.error('Response interceptors:', error);
    return Promise.reject(error);
});

export default axios
