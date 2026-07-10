/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import type {RouteRecordRaw} from 'vue-router';

/**
 * Lazy-loaded layout component
 */
const Layout = () => import('@/components/layout/Layout.vue');

/**
 * Operate routes configuration
 * Includes detail and edit pages for various entities.
 *
 * Settings detail routes live under a single /settings Layout wrapper so they
 * do not compete with the nested /settings/* list routes defined in
 * settings.ts (which resolve through Settings.vue → sidebar). Relative child
 * paths (api/detail, label/detail, …) are more specific than the empty-path
 * layout child, so Vue Router picks the correct branch.
 */
const routes: Array<RouteRecordRaw> = [
  {
    path: '/driver',
    component: Layout,
    children: [
      {
        name: 'driverDetail',
        path: '/driver/detail',
        meta: {
          icon: 'el-icon-s-promotion',
          title: 'Driver Detail',
        },
        component: () => import('@/views/driver/detail/DriverDetail.vue'),
      },
    ],
  },
  {
    path: '/device',
    component: Layout,
    children: [
      {
        name: 'deviceDetail',
        path: '/device/detail',
        meta: {
          icon: 'el-icon-s-finance',
          title: 'Device Detail',
        },
        component: () => import('@/views/device/detail/DeviceDetail.vue'),
      },
      {
        name: 'deviceEdit',
        path: '/device/edit',
        meta: {
          icon: 'el-icon-s-finance',
          title: 'Device Edit',
        },
        component: () => import('@/views/device/edit/DeviceEdit.vue'),
      },
    ],
  },
  {
    path: '/profile',
    component: Layout,
    children: [
      {
        name: 'profileDetail',
        path: '/profile/detail',
        meta: {
          icon: 'el-icon-s-order',
          title: 'Profile Detail',
        },
        component: () => import('@/views/profile/detail/ProfileDetail.vue'),
      },
      {
        name: 'profileEdit',
        path: '/profile/edit',
        meta: {
          icon: 'el-icon-s-order',
          title: 'Profile Edit',
        },
        component: () => import('@/views/profile/edit/ProfileEdit.vue'),
      },
    ],
  },
  {
    path: '/point',
    component: Layout,
    children: [
      {
        name: 'pointDetail',
        path: '/point/detail',
        meta: {
          icon: 'el-icon-s-data',
          title: 'Point Detail',
        },
        component: () => import('@/views/point/detail/PointDetail.vue'),
      },
    ],
  },
  {
    path: '/settings',
    component: Layout,
    children: [
      {
        name: 'settingsApiDetail',
        path: 'api/detail',
        meta: {
          icon: 'el-icon-link',
          title: 'API Detail',
        },
        component: () => import('@/views/settings/api/detail/ApiDetail.vue'),
      },
      {
        name: 'settingsGroupDetail',
        path: 'group/detail',
        meta: {
          icon: 'el-icon-grid',
          title: 'Group Detail',
        },
        component: () => import('@/views/settings/group/detail/GroupDetail.vue'),
      },
      {
        name: 'settingsLabelDetail',
        path: 'label/detail',
        meta: {
          icon: 'el-icon-collection-tag',
          title: 'Label Detail',
        },
        component: () => import('@/views/settings/label/detail/LabelDetail.vue'),
      },
      {
        name: 'settingsAlarmRuleDetail',
        path: 'alarm/rule/detail',
        meta: {
          icon: 'el-icon-set-up',
          title: 'Alarm Rule Detail',
        },
        component: () => import('@/views/settings/alarm/detail/AlarmDetail.vue'),
        props: {entity: 'rule'},
      },
      {
        name: 'settingsAlarmNotifyDetail',
        path: 'alarm/notify/detail',
        meta: {
          icon: 'el-icon-bell',
          title: 'Alarm Notify Policy Detail',
        },
        component: () => import('@/views/settings/alarm/detail/AlarmDetail.vue'),
        props: {entity: 'notify'},
      },
      {
        name: 'settingsAlarmMessageDetail',
        path: 'alarm/message/detail',
        meta: {
          icon: 'el-icon-message',
          title: 'Alarm Message Template Detail',
        },
        component: () => import('@/views/settings/alarm/detail/AlarmDetail.vue'),
        props: {entity: 'message'},
      },
      {
        name: 'settingsAlarmChannelDetail',
        path: 'alarm/channel/detail',
        meta: {
          icon: 'el-icon-connection',
          title: 'Alarm Notify Channel Detail',
        },
        component: () => import('@/views/settings/alarm/detail/AlarmDetail.vue'),
        props: {entity: 'channel'},
      },
      {
        name: 'settingsAlarmBindDetail',
        path: 'alarm/bind/detail',
        meta: {
          icon: 'el-icon-link',
          title: 'Alarm Channel Binding Detail',
        },
        component: () => import('@/views/settings/alarm/detail/AlarmDetail.vue'),
        props: {entity: 'bind'},
      },
      {
        name: 'settingsAlarmStateDetail',
        path: 'alarm/state/detail',
        meta: {
          icon: 'el-icon-monitor',
          title: 'Alarm Runtime State Detail',
        },
        component: () => import('@/views/settings/alarm/detail/AlarmDetail.vue'),
        props: {entity: 'state'},
      },
      {
        name: 'settingsAlarmHistoryDetail',
        path: 'alarm/history/detail',
        meta: {
          icon: 'el-icon-document-checked',
          title: 'Alarm Delivery History Detail',
        },
        component: () => import('@/views/settings/alarm/detail/AlarmDetail.vue'),
        props: {entity: 'history'},
      },
      {
        name: 'settingsModelConfigDetail',
        path: 'model/config/detail',
        meta: {
          icon: 'el-icon-chat-dot-round',
          title: 'Model Config Detail',
        },
        component: () => import('@/views/settings/agentic/detail/ModelConfigDetail.vue'),
      },
      {
        name: 'settingsModelProviderDetail',
        path: 'model/provider/detail',
        meta: {
          icon: 'el-icon-chat-line-square',
          title: 'Model Provider Detail',
        },
        component: () => import('@/views/settings/agentic/detail/ProviderDetail.vue'),
      },
      {
        name: 'settingsResourceDetail',
        path: 'resource/detail',
        meta: {
          icon: 'el-icon-key',
          title: 'Resource Detail',
        },
        component: () => import('@/views/settings/resource/detail/ResourceDetail.vue'),
      },
      {
        name: 'settingsMenuDetail',
        path: 'menu/detail',
        meta: {
          icon: 'el-icon-menu',
          title: 'Menu Detail',
        },
        component: () => import('@/views/settings/menu/detail/MenuDetail.vue'),
      },
      {
        name: 'settingsUserDetail',
        path: 'user/detail',
        meta: {
          icon: 'el-icon-user',
          title: 'User Detail',
        },
        component: () => import('@/views/settings/user/detail/UserDetail.vue'),
      },
      {
        name: 'settingsRoleDetail',
        path: 'role/detail',
        meta: {
          icon: 'el-icon-user-filled',
          title: 'Role Detail',
        },
        component: () => import('@/views/settings/role/detail/RoleDetail.vue'),
      },
    ],
  },
];

export default routes;
