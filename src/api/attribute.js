import request from '@/config/axios'

export const driverAttributeByDriverId = (id) => request({
    url: 'api/v3/manager/driver_attribute/driverId/' + id,
    method: 'get'
});

export const pointAttributeByDriverId = (id) => request({
    url: 'api/v3/manager/point_attribute/driverId/' + id,
    method: 'get'
});
