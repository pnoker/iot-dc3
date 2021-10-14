import md5 from 'js-md5';

import {getStore, removeCookies, removeStore, setCookies, setStore} from '@/util/store'
import {cancelToken, generateSalt, generateToken} from '@/api/token'

import common from "@/util/common";

const token = {
    state: {},
    actions: {
        GenerateSalt({commit}, name) {
            return new Promise((resolve, reject) => {
                generateSalt(name).then(res => {
                    resolve(res.data);
                }).catch(error => {
                    reject(error);
                })
            });
        },
        GenerateToken({commit}, form) {
            let login = {
                tenant: form.tenant,
                name: form.name,
                salt: form.salt,
                password: md5(md5(form.password) + form.salt)
            };
            return new Promise((resolve, reject) => {
                generateToken(login).then(res => {
                    const data = res.data;
                    commit('SET_TOKEN',
                        {
                            tenant: login.tenant,
                            name: login.name,
                            salt: login.salt,
                            token: data
                        }
                    );
                    resolve();
                }).catch(error => {
                    reject(error);
                });
            });
        },
        ClearToken({commit}) {
            return new Promise((resolve) => {
                let token = getStore(common.token_header);
                if (token && token.name) {
                    cancelToken(token.name);
                }
                commit('REMOVE_TOKEN');
                resolve();
            });
        }
    },
    mutations: {
        SET_TOKEN: (state, token) => {
            setCookies(common.token_header, token);
            setStore(common.token_header, token);
        },
        REMOVE_TOKEN: () => {
            removeCookies(common.token_header);
            removeStore(common.token_header);
        }
    }
};

export default token
