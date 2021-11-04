import request from '@/config/axios';

export const driverDictionary = () => request({
    url: 'api/v3/manager/dictionary/driver',
    method: 'get'
});

export const profileDictionary = () => request({
    url: 'api/v3/manager/dictionary/profile',
    method: 'get'
});

export const deviceDictionary = () => request({
    url: 'api/v3/manager/dictionary/device',
    method: 'get'
});

export const pointDictionary = (parent) => request({
    url: 'api/v3/manager/dictionary/point/' + parent,
    method: 'get'
});
