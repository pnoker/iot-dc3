import request from '@/config/axios'

export const driverById = (id) => request({
    url: 'api/v3/manager/driver/id/' + id,
    method: 'get'
});

export const driverList = (driver) => request({
    url: 'api/v3/manager/driver/list',
    method: 'post',
    data: driver
});

export const driverStatus = (driver) => request({
    url: 'api/v3/manager/status/driver',
    method: 'post',
    data: driver
});
