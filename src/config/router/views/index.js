import Layout from '@/components/layout/Layout'

export default {
    path: '/',
    redirect: '/home',
    component: Layout,
    children: [
        {
            name: 'home',
            path: '/home',
            meta: {
                isAuth: true,
                title: '首页'
            },
            component: () => import('@/views/home/Home')
        }, {
            name: 'driver',
            path: '/driver',
            icon: 'el-icon-s-promotion',
            meta: {
                isAuth: true,
                title: '驱动'
            },
            component: () => import('@/views/driver/DriverList')
        }/*, {
            name: 'gateway',
            path: '/gateway',
            icon: 'el-icon-s-platform',
            meta: {
                isAuth: true,
                title: '网关'
            },
            component: () => import('@/views/gateway/GatewayList')
        }*/, {
            name: 'profile',
            path: '/profile',
            icon: 'el-icon-s-order',
            meta: {
                isAuth: true,
                title: '模板'
            },
            component: () => import('@/views/profile/ProfileList')
        }, {
            name: 'device',
            path: '/device',
            icon: 'el-icon-s-finance',
            meta: {
                isAuth: true,
                title: '设备'
            },
            component: () => import('@/views/device/DeviceList')
        }, {
            name: 'pointValue',
            path: '/point_value',
            icon: 'el-icon-s-data',
            meta: {
                isAuth: true,
                title: '数据'
            },
            component: () => import('@/views/point/value/PointValueList')
        }/*, {
            name: 'setting',
            path: '/setting',
            icon: 'el-icon-s-tools',
            meta: {
                isAuth: true,
                title: '设置'
            },
            component: () => import('@/views/home/Home')
        }*/
    ]
}

