import request from '@/config/axios'

export const profileAdd = (profile) => request({
    url: 'api/v3/manager/profile/add',
    method: 'post',
    data: profile
});

export const profileDelete = (id) => request({
    url: 'api/v3/manager/profile/delete/' + id,
    method: 'post'
});

export const profileUpdate = (profile) => request({
    url: 'api/v3/manager/profile/update',
    method: 'post',
    data: profile
});

export const profileById = (id) => request({
    url: 'api/v3/manager/profile/id/' + id,
    method: 'get'
});

export const profileByDeviceId = (deviceId) => request({
    url: 'api/v3/manager/profile/deviceId/' + deviceId,
    method: 'get'
});

export const profileList = (profile) => request({
    url: 'api/v3/manager/profile/list',
    method: 'post',
    data: profile
});
