import request from '@/config/axios'

export const getStatisticsData = () =>
    request<R>({
        url: "api/v3/manager/statistics",
        method: 'get',
    })

export const getWeatherDeviceList = () =>
    request<R>({
        url: "api/v3/manager/weatherDeviceList",
        method: 'get',
    })