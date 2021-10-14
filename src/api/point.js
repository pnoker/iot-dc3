import request from '@/config/axios'

// point
export const pointAdd = (point) => request({
    url: 'api/v3/manager/point/add',
    method: 'post',
    data: point
});

export const pointDelete = (id) => request({
    url: 'api/v3/manager/point/delete/' + id,
    method: 'post'
});

export const pointUpdate = (point) => request({
    url: 'api/v3/manager/point/update',
    method: 'post',
    data: point
});

export const pointById = (id) => request({
    url: 'api/v3/manager/point/id/' + id,
    method: 'get'
});

export const pointList = (point) => request({
    url: 'api/v3/manager/point/list',
    method: 'post',
    data: point
});

export const pointUnit = (pointIds) => request({
    url: 'api/v3/manager/point/unit',
    method: 'post',
    data: pointIds
});

export const pointByProfileId = (profileId) => request({
    url: 'api/v3/manager/point/profileId/' + profileId,
    method: 'get'
});

export const pointByDeviceId = (deviceId) => request({
    url: 'api/v3/manager/point/deviceId/' + deviceId,
    method: 'get'
});

// point value
export const pointValueByDeviceId = (deviceId, history) => request({
    url: 'api/v3/data/point_value/latest/device_id/' + deviceId,
    method: 'get',
    params: {history}
});

export const pointValueList = (pointValue) => request({
    url: 'api/v3/data/point_value/list',
    method: 'post',
    data: pointValue
});
