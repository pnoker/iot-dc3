import request from '@/config/axios'

export const deviceAdd = (device) => request({
    url: 'api/v3/manager/device/add',
    method: 'post',
    data: device
});

export const deviceDelete = (id) => request({
    url: 'api/v3/manager/device/delete/' + id,
    method: 'post'
});

export const deviceUpdate = (device) => request({
    url: 'api/v3/manager/device/update',
    method: 'post',
    data: device
});

export const deviceById = (id) => request({
    url: 'api/v3/manager/device/id/' + id,
    method: 'get'
});

export const deviceByDriverId = (driverId) => request({
    url: 'api/v3/manager/device/driverId/' + driverId,
    method: 'get'
});

export const deviceByProfileId = (profileId) => request({
    url: 'api/v3/manager/device/profileId/' + profileId,
    method: 'get'
});

export const deviceList = (device) => request({
    url: 'api/v3/manager/device/list',
    method: 'post',
    data: device
});

// device status
export const deviceStatus = (device) => request({
    url: 'api/v3/manager/status/device',
    method: 'post',
    data: device
});

export const deviceStatusByDriverId = (driverId) => request({
    url: 'api/v3/manager/status/device/driverId/' + driverId,
    method: 'get'
});

export const deviceStatusByProfileId = (profileId) => request({
    url: 'api/v3/manager/status/device/profileId/' + profileId,
    method: 'get'
});
