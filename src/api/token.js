import request from '@/config/axios'

/**
 * 通过用户名获取 Salt
 *
 * @param name 用户名
 * @returns {AxiosPromise}
 */
export const generateSalt = (name) => request({
    url: 'api/v3/token/salt',
    method: 'post',
    data: {name}
});

/**
 * 登录
 *
 * @param login {tenant, name, salt, password}
 * @returns {AxiosPromise}
 */
export const generateToken = (login) => request({
    url: `api/v3/token/generate`,
    method: 'post',
    data: login
});

/**
 * 注销
 *
 * @param name 用户名
 * @returns {AxiosPromise}
 */
export const cancelToken = (name) => request({
    url: 'api/v3/token/cancel',
    method: 'post',
    data: {name}
});

/**
 * 校验 Token
 *
 * @param login {name, salt, token}
 * @returns {AxiosPromise}
 */
export const checkTokenValid = (login) => request({
    url: 'api/v3/token/check',
    method: 'post',
    data: login
});
