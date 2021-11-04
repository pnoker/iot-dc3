import Vue from 'vue';
import VueRouter from 'vue-router';
import CommonRouter from './common';
import ViewsRouter from './views';
import OperateRouter from './operate';
import {getStore} from "@/util/store";
import {checkTokenValid} from "@/api/token";
import common from '@/util/common';
import NProgress from 'nprogress';
import 'nprogress/nprogress.css';

NProgress.configure({
    easing: 'ease',
    showSpinner: false
});

Vue.use(VueRouter);

const index = new VueRouter({
    routes: [...CommonRouter, ViewsRouter, ...OperateRouter]
});

index.beforeEach((to, from, next) => {
    NProgress.start();
    const meta = to.meta || {};
    if (meta.title) {
        document.title = to.meta.title;
    }

    if (meta.isAuth !== true || from.name === 'login') {
        next();
    } else {
        const token = getStore(common.token_header);
        if (!token) {
            next({path: '/login'});
            return;
        }

        checkTokenValid(token).then(res => {
            if (res.ok) {
                next();
            } else {
                throw new Error(res.message);
            }
        }).catch((e) => {
            console.warn('用户凭证已过期，请重新登录！', e);
            store.dispatch('ClearToken').then(() => next({path: '/login'}));
        });
    }
});

index.afterEach(() => {
    NProgress.done();
});

export default index
