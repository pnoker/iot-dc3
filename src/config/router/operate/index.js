import Layout from '@/components/layout/Layout';

export default [
    {
        path: '/home',
        component: Layout,
        children: [
            {
                name: 'dashboard',
                path: '/home/dashboard',
                icon: 'el-icon-s-home',
                meta: {
                    isAuth: true,
                    title: '看板'
                },
                component: () => import('@/views/dashboard/Dashboard')
            },
            {
                name: 'application',
                path: '/home/application',
                icon: 'el-icon-s-home',
                meta: {
                    isAuth: true,
                    title: '应用'
                },
                component: () => import('@/views/application/Application')
            }
        ]
    },
    {
        path: '/driver',
        component: Layout,
        children: [
            {
                name: 'driverDetail',
                path: '/driver/detail',
                icon: 'el-icon-s-promotion',
                meta: {
                    isAuth: true,
                    title: '驱动详情'
                },
                component: () => import('@/views/driver/DriverDetail')
            }
        ]
    },
    {
        path: '/gateway',
        component: Layout,
        children: [
            {
                name: 'gatewayDetail',
                path: '/gateway/detail',
                icon: 'el-icon-s-platform',
                meta: {
                    isAuth: true,
                    title: '网关详情'
                },
                component: () => import('@/views/gateway/GatewayDetail')
            }
        ]
    },
    {
        path: '/device',
        component: Layout,
        children: [
            {
                name: 'deviceDetail',
                path: '/device/detail',
                icon: 'el-icon-s-finance',
                meta: {
                    isAuth: true,
                    title: '设备详情'
                },
                component: () => import('@/views/device/DeviceDetail')
            }, {
                name: 'deviceEdit',
                path: '/device/edit',
                icon: 'el-icon-s-finance',
                meta: {
                    isAuth: true,
                    title: '设备编辑'
                },
                component: () => import('@/views/device/DeviceEdit')
            }
        ]
    },
    {
        path: '/profile',
        component: Layout,
        children: [
            {
                name: 'profileDetail',
                path: '/profile/detail',
                icon: 'el-icon-s-order',
                meta: {
                    isAuth: true,
                    title: '模板详情'
                },
                component: () => import('@/views/profile/ProfileDetail')
            }, {
                name: 'profileEdit',
                path: '/profile/edit',
                icon: 'el-icon-s-order',
                meta: {
                    isAuth: true,
                    title: '模板编辑'
                },
                component: () => import('@/views/profile/ProfileEdit')
            }, {
                name: 'pointDetail',
                path: '/profile/point/detail',
                icon: 'el-icon-s-order',
                meta: {
                    isAuth: true,
                    title: '位号详情'
                },
                component: () => import('@/views/point/PointDetail')
            }, {
                name: 'pointEdit',
                path: '/profile/point/edit',
                icon: 'el-icon-s-order',
                meta: {
                    isAuth: true,
                    title: '位号编辑'
                },
                component: () => import('@/views/point/PointEdit')
            }
        ]
    }
]

