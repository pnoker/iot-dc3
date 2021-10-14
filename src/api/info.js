import request from '@/config/axios'

export const driverInfoAdd = (driverInfo) => request({
    url: 'api/v3/manager/driver_info/add',
    method: 'post',
    data: driverInfo
});

export const driverInfoUpdate = (driverInfo) => request({
    url: 'api/v3/manager/driver_info/update',
    method: 'post',
    data: driverInfo
});

export const driverInfoByAttributeIdAndDeviceId = (attributeId, deviceId) => request({
    url: 'api/v3/manager/driver_info/attributeId/' + attributeId + '/deviceId/' + deviceId,
    method: 'get'
});

export const driverInfoByDeviceId = (deviceId) => request({
    url: 'api/v3/manager/driver_info/deviceId/' + deviceId,
    method: 'get'
});

export const pointInfoAdd = (pointInfo) => request({
    url: 'api/v3/manager/point_info/add',
    method: 'post',
    data: pointInfo
});

export const pointInfoUpdate = (pointInfo) => request({
    url: 'api/v3/manager/point_info/update',
    method: 'post',
    data: pointInfo
});

export const pointInfoByDeviceIdAndPointId = (deviceId, pointId) => request({
    url: 'api/v3/manager/point_info/deviceId/' + deviceId + '/pointId/' + pointId,
    method: 'get'
});

export const pointInfoByDeviceId = (deviceId) => request({
    url: 'api/v3/manager/point_info/deviceId/' + deviceId,
    method: 'get'
});
