/*
 * Copyright (c) 2022. Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE dc3;
USE dc3;

-- ----------------------------
-- Table structure for dc3_tenant
-- ----------------------------
drop table if exists dc3_tenant;
create table dc3_tenant
(
    id            bigint unsigned auto_increment primary key not null comment '主键ID',
    tenant_name   varchar(128) default ''                   not null comment '租户名称',
    tenant_code   varchar(128) default ''                   not null comment '租户编号',
    enable_flag   tinyint(4) default 1 not null comment '使能标识',
    remark        varchar(512) default ''                   not null comment '描述',
    creator_id    bigint       default 0                    not null comment '创建者ID',
    creator_name  varchar(128) default ''                   not null comment '创建者名称',
    create_time   datetime     default CURRENT_TIMESTAMP(0) not null comment '创建时间',
    operator_id   bigint       default 0                    not null comment '操作者ID',
    operator_name varchar(128) default ''                   not null comment '操作者名称',
    update_time   datetime     default CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP (0) not null comment '操作时间',
    deleted       tinyint(4) default 0 not null comment '逻辑删除标识,0:未删除,1:已删除',
    INDEX         idx_tenant_name (tenant_name) USING BTREE,
    INDEX         idx_tenant_code (tenant_code) USING BTREE
) engine = InnoDB
  character set = utf8
  collate = utf8_general_ci
    comment = '租户表';

-- ----------------------------
-- Table structure for dc3_driver
-- ----------------------------
drop table if exists dc3_driver;
create table dc3_driver
(
    id               bigint unsigned auto_increment primary key not null comment '主键ID',
    driver_name      varchar(128) default ''                   not null comment '驱动名称',
    driver_code      varchar(128) default ''                   not null comment '驱动编号',
    service_name     varchar(128) default ''                   not null comment '驱动服务名称',
    service_host     varchar(128) default ''                   not null comment '服务主机',
    service_port     int(10) default 0 not null comment '服务端口',
    driver_type_flag tinyint(4) default 0 not null comment '驱动类型标识',
    enable_flag      tinyint(4) default 1 not null comment '使能标识',
    tenant_id        bigint       default 0                    not null comment '租户ID',
    remark           varchar(512) default ''                   not null comment '描述',
    creator_id       bigint       default 0                    not null comment '创建者ID',
    creator_name     varchar(128) default ''                   not null comment '创建者名称',
    create_time      datetime     default CURRENT_TIMESTAMP(0) not null comment '创建时间',
    operator_id      bigint       default 0                    not null comment '操作者ID',
    operator_name    varchar(128) default ''                   not null comment '操作者名称',
    update_time      datetime     default CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP (0) not null comment '操作时间',
    deleted          tinyint(4) default 0 not null comment '逻辑删除标识,0:未删除,1:已删除',
    INDEX            idx_driver_name (driver_name) USING BTREE,
    INDEX            idx_driver_code (driver_code) USING BTREE
) engine = InnoDB
  character set = utf8
  collate = utf8_general_ci
    comment = '协议驱动表';

-- ----------------------------
-- Table structure for dc3_driver_attribute
-- ----------------------------
drop table if exists dc3_driver_attribute;
create table dc3_driver_attribute
(
    id                  bigint unsigned auto_increment primary key not null comment '主键ID',
    display_name        varchar(128) default ''                   not null comment '显示名称',
    attribute_name      varchar(128) default ''                   not null comment '属性名称',
    attribute_type_flag tinyint(4) default 0 not null comment '属性类型标识',
    default_value       varchar(128) default ''                   not null comment '默认值',
    driver_id           bigint       default 0                    not null comment '驱动ID',
    enable_flag         tinyint(4) default 1 not null comment '使能标识',
    tenant_id           bigint       default 0                    not null comment '租户ID',
    remark              varchar(512) default ''                   not null comment '描述',
    creator_id          bigint       default 0                    not null comment '创建者ID',
    creator_name        varchar(128) default ''                   not null comment '创建者名称',
    create_time         datetime     default CURRENT_TIMESTAMP(0) not null comment '创建时间',
    operator_id         bigint       default 0                    not null comment '操作者ID',
    operator_name       varchar(128) default ''                   not null comment '操作者名称',
    update_time         datetime     default CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP (0) not null comment '操作时间',
    deleted             tinyint(4) default 0 not null comment '逻辑删除标识,0:未删除,1:已删除',
    INDEX               idx_driver_id (driver_id) USING BTREE
) engine = InnoDB
  character set = utf8
  collate = utf8_general_ci
    comment = '连接配置信息表';

-- ----------------------------
-- Table structure for dc3_point_attribute
-- ----------------------------
drop table if exists dc3_point_attribute;
create table dc3_point_attribute
(
    id                  bigint unsigned auto_increment primary key not null comment '主键ID',
    display_name        varchar(128) default ''                   not null comment '显示名称',
    attribute_name      varchar(128) default ''                   not null comment '属性名称',
    attribute_type_flag tinyint(4) default 0 not null comment '属性类型标识',
    default_value       varchar(128) default ''                   not null comment '默认值',
    driver_id           bigint       default 0                    not null comment '驱动ID',
    enable_flag         tinyint(4) default 1 not null comment '使能标识',
    tenant_id           bigint       default 0                    not null comment '租户ID',
    remark              varchar(512) default ''                   not null comment '描述',
    creator_id          bigint       default 0                    not null comment '创建者ID',
    creator_name        varchar(128) default ''                   not null comment '创建者名称',
    create_time         datetime     default CURRENT_TIMESTAMP(0) not null comment '创建时间',
    operator_id         bigint       default 0                    not null comment '操作者ID',
    operator_name       varchar(128) default ''                   not null comment '操作者名称',
    update_time         datetime     default CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP (0) not null comment '操作时间',
    deleted             tinyint(4) default 0 not null comment '逻辑删除标识,0:未删除,1:已删除',
    INDEX               idx_driver_id (driver_id) USING BTREE
) engine = InnoDB
  character set = utf8
  collate = utf8_general_ci
    comment = '模板配置信息表';

-- ----------------------------
-- Table structure for dc3_profile
-- ----------------------------
drop table if exists dc3_profile;
create table dc3_profile
(
    id                 bigint unsigned auto_increment primary key not null comment '主键ID',
    profile_name       varchar(128) default ''                   not null comment '模板名称',
    profile_code       varchar(128) default ''                   not null comment '模板编号',
    profile_share_flag tinyint(4) default 0 not null comment '模板共享类型标识',
    profile_type_flag  tinyint(4) default 0 not null comment '模板类型标识',
    group_id           bigint       default 0                    not null comment '分组ID',
    enable_flag        tinyint(4) default 1 not null comment '使能标识',
    tenant_id          bigint       default 0                    not null comment '租户ID',
    remark             varchar(512) default ''                   not null comment '描述',
    creator_id         bigint       default 0                    not null comment '创建者ID',
    creator_name       varchar(128) default ''                   not null comment '创建者名称',
    create_time        datetime     default CURRENT_TIMESTAMP(0) not null comment '创建时间',
    operator_id        bigint       default 0                    not null comment '操作者ID',
    operator_name      varchar(128) default ''                   not null comment '操作者名称',
    update_time        datetime     default CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP (0) not null comment '操作时间',
    deleted            tinyint(4) default 0 not null comment '逻辑删除标识,0:未删除,1:已删除',
    INDEX              idx_profile_name (profile_name) USING BTREE,
    INDEX              idx_profile_code (profile_code) USING BTREE
) engine = InnoDB
  character set = utf8
  collate = utf8_general_ci
    comment = '设备模板表';

-- ----------------------------
-- Table structure for dc3_profile_bind
-- ----------------------------
drop table if exists dc3_profile_bind;
create table dc3_profile_bind
(
    id            bigint unsigned auto_increment primary key not null comment '主键ID',
    profile_id    bigint       default 0                    not null comment '模版ID',
    device_id     bigint       default 0                    not null comment '设备ID',
    remark        varchar(512) default ''                   not null comment '描述',
    creator_id    bigint       default 0                    not null comment '创建者ID',
    creator_name  varchar(128) default ''                   not null comment '创建者名称',
    create_time   datetime     default CURRENT_TIMESTAMP(0) not null comment '创建时间',
    operator_id   bigint       default 0                    not null comment '操作者ID',
    operator_name varchar(128) default ''                   not null comment '操作者名称',
    update_time   datetime     default CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP (0) not null comment '操作时间',
    deleted       tinyint(4) default 0 not null comment '逻辑删除标识,0:未删除,1:已删除',
    INDEX         idx_profile_id (profile_id) USING BTREE,
    INDEX         idx_device_id (device_id) USING BTREE
) engine = InnoDB
  character set = utf8
  collate = utf8_general_ci
    comment = '设备与模版映射关联表';

-- ----------------------------
-- Table structure for dc3_point
-- ----------------------------
drop table if exists dc3_point;
create table dc3_point
(
    id              bigint unsigned auto_increment primary key not null comment '主键ID',
    point_name      varchar(128)   default ''                   not null comment '位号名称',
    point_code      varchar(128)   default ''                   not null comment '位号编号',
    point_type_flag tinyint(4) default 0 not null comment '位号类型标识',
    rw_flag         tinyint(4) default 0 not null comment '读写标识',
    base_value      decimal(15, 6) default 0                    not null comment '基础值',
    multiple        decimal(15, 6) default 1                    not null comment '比例系数',
    accrue_flag     tinyint(4) default 0 not null comment '累计标识',
    value_decimal   tinyint(4) default 6 not null comment '数据精度',
    unit            tinyint(4) default 0 not null comment '单位',
    profile_id      bigint         default 0                    not null comment '模板ID',
    group_id        bigint         default 0                    not null comment '分组ID',
    enable_flag     tinyint(4) default 1 not null comment '使能标识',
    tenant_id       bigint         default 0                    not null comment '租户ID',
    remark          varchar(512)   default ''                   not null comment '描述',
    creator_id      bigint         default 0                    not null comment '创建者ID',
    creator_name    varchar(128)   default ''                   not null comment '创建者名称',
    create_time     datetime       default CURRENT_TIMESTAMP(0) not null comment '创建时间',
    operator_id     bigint         default 0                    not null comment '操作者ID',
    operator_name   varchar(128)   default ''                   not null comment '操作者名称',
    update_time     datetime       default CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP (0) not null comment '操作时间',
    deleted         tinyint(4) default 0 not null comment '逻辑删除标识,0:未删除,1:已删除',
    INDEX           idx_point_name (point_name) USING BTREE,
    INDEX           idx_point_code (point_code) USING BTREE,
    INDEX           idx_profile_id (profile_id) USING BTREE
) engine = InnoDB
  character set = utf8
  collate = utf8_general_ci
    comment = '设备位号表';

-- ----------------------------
-- Table structure for dc3_device
-- ----------------------------
drop table if exists dc3_device;
create table dc3_device
(
    id            bigint unsigned auto_increment primary key not null comment '主键ID',
    device_name   varchar(128) default ''                   not null comment '设备名称',
    device_code   varchar(128) default ''                   not null comment '设备编号',
    multi_flag    tinyint(4) default 0 not null comment '结构化标识',
    driver_id     bigint       default 0                    not null comment '驱动ID',
    group_id      bigint       default 0                    not null comment '分组ID',
    enable_flag   tinyint(4) default 1 not null comment '使能标识',
    tenant_id     bigint       default 0                    not null comment '租户ID',
    remark        varchar(512) default ''                   not null comment '描述',
    creator_id    bigint       default 0                    not null comment '创建者ID',
    creator_name  varchar(128) default ''                   not null comment '创建者名称',
    create_time   datetime     default CURRENT_TIMESTAMP(0) not null comment '创建时间',
    operator_id   bigint       default 0                    not null comment '操作者ID',
    operator_name varchar(128) default ''                   not null comment '操作者名称',
    update_time   datetime     default CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP (0) not null comment '操作时间',
    deleted       tinyint(4) default 0 not null comment '逻辑删除标识,0:未删除,1:已删除',
    INDEX         idx_device_name (device_name) USING BTREE,
    INDEX         idx_device_code (device_code) USING BTREE,
    INDEX         idx_driver_id (driver_id) USING BTREE
) engine = InnoDB
  character set = utf8
  collate = utf8_general_ci
    comment = '设备表';

-- ----------------------------
-- Table structure for dc3_driver_info
-- ----------------------------
drop table if exists dc3_driver_info;
create table dc3_driver_info
(
    id                  bigint unsigned auto_increment primary key not null comment '主键ID',
    driver_attribute_id bigint       default 0                    not null comment '驱动配置ID',
    config_value        varchar(128) default ''                   not null comment '驱动配置值',
    device_id           bigint       default 0                    not null comment '设备ID',
    enable_flag         tinyint(4) default 1 not null comment '使能标识',
    tenant_id           bigint       default 0                    not null comment '租户ID',
    remark              varchar(512) default ''                   not null comment '描述',
    creator_id          bigint       default 0                    not null comment '创建者ID',
    creator_name        varchar(128) default ''                   not null comment '创建者名称',
    create_time         datetime     default CURRENT_TIMESTAMP(0) not null comment '创建时间',
    operator_id         bigint       default 0                    not null comment '操作者ID',
    operator_name       varchar(128) default ''                   not null comment '操作者名称',
    update_time         datetime     default CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP (0) not null comment '操作时间',
    deleted             tinyint(4) default 0 not null comment '逻辑删除标识,0:未删除,1:已删除',
    INDEX               idx_driver_attribute_id (driver_attribute_id) USING BTREE,
    INDEX               idx_device_id (device_id) USING BTREE
) engine = InnoDB
  character set = utf8
  collate = utf8_general_ci
    comment = '模板连接配置信息表';

-- ----------------------------
-- Table structure for dc3_point_info
-- ----------------------------
drop table if exists dc3_point_info;
create table dc3_point_info
(
    id                 bigint unsigned auto_increment primary key not null comment '主键ID',
    point_attribute_id bigint       default 0                    not null comment '位号配置ID',
    config_value       varchar(128) default ''                   not null comment '位号配置值',
    device_id          bigint       default 0                    not null comment '设备ID',
    point_id           bigint       default 0                    not null comment '位号ID',
    enable_flag        tinyint(4) default 1 not null comment '使能标识',
    tenant_id          bigint       default 0                    not null comment '租户ID',
    remark             varchar(512) default ''                   not null comment '描述',
    creator_id         bigint       default 0                    not null comment '创建者ID',
    creator_name       varchar(128) default ''                   not null comment '创建者名称',
    create_time        datetime     default CURRENT_TIMESTAMP(0) not null comment '创建时间',
    operator_id        bigint       default 0                    not null comment '操作者ID',
    operator_name      varchar(128) default ''                   not null comment '操作者名称',
    update_time        datetime     default CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP (0) not null comment '操作时间',
    deleted            tinyint(4) default 0 not null comment '逻辑删除标识,0:未删除,1:已删除',
    INDEX              idx_point_attribute_id (point_attribute_id) USING BTREE,
    INDEX              idx_device_id (device_id) USING BTREE,
    INDEX              idx_point_id (point_id) USING BTREE
) engine = InnoDB
  character set = utf8
  collate = utf8_general_ci
    comment = '位号配置信息表';

-- ----------------------------
-- Table structure for dc3_user
-- ----------------------------
drop table if exists dc3_user;
create table dc3_user
(
    id               bigint unsigned auto_increment primary key not null comment '主键ID',
    login_name       varchar(128) default ''                   not null comment '登录名称',
    user_ext_id      bigint       default 0                    not null comment '用户拓展ID',
    user_password_id bigint       default 0                    not null comment '用户密码ID',
    enable_flag      tinyint(4) default 1 not null comment '使能标识',
    remark           varchar(512) default ''                   not null comment '描述',
    creator_id       bigint       default 0                    not null comment '创建者ID',
    creator_name     varchar(128) default ''                   not null comment '创建者名称',
    create_time      datetime     default CURRENT_TIMESTAMP(0) not null comment '创建时间',
    operator_id      bigint       default 0                    not null comment '操作者ID',
    operator_name    varchar(128) default ''                   not null comment '操作者名称',
    update_time      datetime     default CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP (0) not null comment '操作时间',
    deleted          tinyint(4) default 0 not null comment '逻辑删除标识,0:未删除,1:已删除',
    INDEX            idx_login_name (login_name) USING BTREE,
    INDEX            idx_user_ext_id (user_ext_id) USING BTREE,
    INDEX            idx_user_password_id (user_password_id) USING BTREE
) engine = InnoDB
  character set = utf8
  collate = utf8_general_ci
    comment = '用户表';

-- ----------------------------
-- Table structure for dc3_user_password
-- ----------------------------
drop table if exists dc3_user_password;
create table dc3_user_password
(
    id             bigint unsigned auto_increment primary key not null comment '主键ID',
    login_password varchar(512) default ''                   not null comment '登录密码',
    remark         varchar(512) default ''                   not null comment '描述',
    creator_id     bigint       default 0                    not null comment '创建者ID',
    creator_name   varchar(128) default ''                   not null comment '创建者名称',
    create_time    datetime     default CURRENT_TIMESTAMP(0) not null comment '创建时间',
    operator_id    bigint       default 0                    not null comment '操作者ID',
    operator_name  varchar(128) default ''                   not null comment '操作者名称',
    update_time    datetime     default CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP (0) not null comment '操作时间',
    deleted        tinyint(4) default 0 not null comment '逻辑删除标识,0:未删除,1:已删除'
) engine = InnoDB
  character set = utf8
  collate = utf8_general_ci
    comment = '用户密码表';

-- ----------------------------
-- Table structure for dc3_user_ext
-- ----------------------------
drop table if exists dc3_user_ext;
create table dc3_user_ext
(
    id            bigint unsigned auto_increment primary key not null comment '主键ID',
    nick_name     varchar(128) default ''                   not null comment '用户昵称',
    user_name     varchar(128) default ''                   not null comment '用户名',
    phone         varchar(32)  default ''                   not null comment '手机号',
    email         varchar(128) default ''                   not null comment '邮箱',
    remark        varchar(512) default ''                   not null comment '描述',
    creator_id    bigint       default 0                    not null comment '创建者ID',
    creator_name  varchar(128) default ''                   not null comment '创建者名称',
    create_time   datetime     default CURRENT_TIMESTAMP(0) not null comment '创建时间',
    operator_id   bigint       default 0                    not null comment '操作者ID',
    operator_name varchar(128) default ''                   not null comment '操作者名称',
    update_time   datetime     default CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP (0) not null comment '操作时间',
    deleted       tinyint(4) default 0 not null comment '逻辑删除标识,0:未删除,1:已删除'
) engine = InnoDB
  character set = utf8
  collate = utf8_general_ci
    comment = '用户拓展表';

-- ----------------------------
-- Table structure for dc3_tenant_bind
-- ----------------------------
drop table if exists dc3_tenant_bind;
create table dc3_tenant_bind
(
    id            bigint unsigned auto_increment primary key not null comment '主键ID',
    tenant_id     bigint       default 0                    not null comment '租户ID',
    user_id       bigint       default 0                    not null comment '用户ID',
    remark        varchar(512) default ''                   not null comment '描述',
    creator_id    bigint       default 0                    not null comment '创建者ID',
    creator_name  varchar(128) default ''                   not null comment '创建者名称',
    create_time   datetime     default CURRENT_TIMESTAMP(0) not null comment '创建时间',
    operator_id   bigint       default 0                    not null comment '操作者ID',
    operator_name varchar(128) default ''                   not null comment '操作者名称',
    update_time   datetime     default CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP (0) not null comment '操作时间',
    deleted       tinyint(4) default 0 not null comment '逻辑删除标识,0:未删除,1:已删除',
    INDEX         idx_tenant_id (tenant_id) USING BTREE,
    INDEX         idx_user_id (user_id) USING BTREE
) engine = InnoDB
  character set = utf8
  collate = utf8_general_ci
    comment = '租户关联表';

-- ----------------------------
-- Table structure for dc3_group
-- ----------------------------
drop table if exists dc3_group;
create table dc3_group
(
    id              bigint unsigned auto_increment primary key not null comment '主键ID',
    parent_group_id bigint       default 0                    not null comment '父分组ID',
    group_name      varchar(128) default ''                   not null comment '分组名称',
    position        int(10) default 0 not null comment '分组排序位置',
    enable_flag     tinyint(4) default 1 not null comment '使能标识',
    tenant_id       bigint       default 0                    not null comment '租户ID',
    remark          varchar(512) default ''                   not null comment '描述',
    creator_id      bigint       default 0                    not null comment '创建者ID',
    creator_name    varchar(128) default ''                   not null comment '创建者名称',
    create_time     datetime     default CURRENT_TIMESTAMP(0) not null comment '创建时间',
    operator_id     bigint       default 0                    not null comment '操作者ID',
    operator_name   varchar(128) default ''                   not null comment '操作者名称',
    update_time     datetime     default CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP (0) not null comment '操作时间',
    deleted         tinyint(4) default 0 not null comment '逻辑删除标识,0:未删除,1:已删除',
    INDEX           idx_group_id (parent_group_id) USING BTREE,
    INDEX           idx_group_name (group_name) USING BTREE
) engine = InnoDB
  character set = utf8
  collate = utf8_general_ci
    comment = '分组表';

-- ----------------------------
-- Table structure for dc3_role
-- ----------------------------
drop table if exists dc3_role;
create table dc3_role
(
    id             bigint unsigned auto_increment primary key not null comment '主键ID',
    parent_role_id bigint       default 0                    not null comment '角色父级ID',
    role_name      varchar(128) default ''                   not null comment '角色名称',
    role_code      varchar(128) default ''                   not null comment '角色编号',
    enable_flag    tinyint(4) default 1 not null comment '使能标识',
    tenant_id      bigint       default 0                    not null comment '租户ID',
    remark         varchar(512) default ''                   not null comment '描述',
    creator_id     bigint       default 0                    not null comment '创建者ID',
    creator_name   varchar(128) default ''                   not null comment '创建者名称',
    create_time    datetime     default CURRENT_TIMESTAMP(0) not null comment '创建时间',
    operator_id    bigint       default 0                    not null comment '操作者ID',
    operator_name  varchar(128) default ''                   not null comment '操作者名称',
    update_time    datetime     default CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP (0) not null comment '操作时间',
    deleted        tinyint(4) default 0 not null comment '逻辑删除标识,0:未删除,1:已删除',
    INDEX          idx_parent_role_id (parent_role_id) USING BTREE,
    INDEX          idx_role_name (role_name) USING BTREE,
    INDEX          idx_role_code (role_code) USING BTREE
) engine = InnoDB
  character set = utf8
  collate = utf8_general_ci
    comment = '角色表';

-- ----------------------------
-- Table structure for dc3_resource
-- ----------------------------
drop table if exists dc3_resource;
create table dc3_resource
(
    id                  bigint unsigned auto_increment primary key not null comment '主键ID',
    parent_resource_id  bigint       default 0                    not null comment '权限资源父级ID',
    resource_name       varchar(128) default ''                   not null comment '权限资源名称',
    resource_code       varchar(128) default ''                   not null comment '权限资源编号',
    resource_type_flag  tinyint(4) default 0 not null comment '权限资源类型标识',
    resource_scope_flag tinyint(4) default 0 not null comment '权限资源范围标识',
    entity_id           bigint       default 0                    not null comment '权限资源实体ID',
    enable_flag         tinyint(4) default 1 not null comment '使能标识',
    tenant_id           bigint       default 0                    not null comment '租户ID',
    remark              varchar(512) default ''                   not null comment '描述',
    creator_id          bigint       default 0                    not null comment '创建者ID',
    creator_name        varchar(128) default ''                   not null comment '创建者名称',
    create_time         datetime     default CURRENT_TIMESTAMP(0) not null comment '创建时间',
    operator_id         bigint       default 0                    not null comment '操作者ID',
    operator_name       varchar(128) default ''                   not null comment '操作者名称',
    update_time         datetime     default CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP (0) not null comment '操作时间',
    deleted             tinyint(4) default 0 not null comment '逻辑删除标识,0:未删除,1:已删除',
    INDEX               idx_parent_resource_id (parent_resource_id) USING BTREE,
    INDEX               idx_resource_name (resource_name) USING BTREE,
    INDEX               idx_resource_code (resource_code) USING BTREE,
    INDEX               idx_entity_id (entity_id) USING BTREE
) engine = InnoDB
  character set = utf8
  collate = utf8_general_ci
    comment = '权限资源表';

-- ----------------------------
-- Table structure for dc3_role_user_bind
-- ----------------------------
drop table if exists dc3_role_user_bind;
create table dc3_role_user_bind
(
    id            bigint unsigned auto_increment primary key not null comment '主键ID',
    role_id       bigint       default 0                    not null comment '角色ID',
    user_id       bigint       default 0                    not null comment '用户ID',
    remark        varchar(512) default ''                   not null comment '描述',
    creator_id    bigint       default 0                    not null comment '创建者ID',
    creator_name  varchar(128) default ''                   not null comment '创建者名称',
    create_time   datetime     default CURRENT_TIMESTAMP(0) not null comment '创建时间',
    operator_id   bigint       default 0                    not null comment '操作者ID',
    operator_name varchar(128) default ''                   not null comment '操作者名称',
    update_time   datetime     default CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP (0) not null comment '操作时间',
    deleted       tinyint(4) default 0 not null comment '逻辑删除标识,0:未删除,1:已删除',
    INDEX         idx_role_id (role_id) USING BTREE,
    INDEX         idx_user_id (user_id) USING BTREE
) engine = InnoDB
  character set = utf8
  collate = utf8_general_ci
    comment = '角色-用户关联表';

-- ----------------------------
-- Table structure for dc3_role_resource_bind
-- ----------------------------
drop table if exists dc3_role_resource_bind;
create table dc3_role_resource_bind
(
    id            bigint unsigned auto_increment primary key not null comment '主键ID',
    role_id       bigint       default 0                    not null comment '权限资源ID',
    resource_id   bigint       default 0                    not null comment '权限资源ID',
    remark        varchar(512) default ''                   not null comment '描述',
    creator_id    bigint       default 0                    not null comment '创建者ID',
    creator_name  varchar(128) default ''                   not null comment '创建者名称',
    create_time   datetime     default CURRENT_TIMESTAMP(0) not null comment '创建时间',
    operator_id   bigint       default 0                    not null comment '操作者ID',
    operator_name varchar(128) default ''                   not null comment '操作者名称',
    update_time   datetime     default CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP (0) not null comment '操作时间',
    deleted       tinyint(4) default 0 not null comment '逻辑删除标识,0:未删除,1:已删除',
    INDEX         idx_role_id (role_id) USING BTREE,
    INDEX         idx_resource_id (resource_id) USING BTREE
) engine = InnoDB
  character set = utf8
  collate = utf8_general_ci
    comment = '角色-权限资源关联表';

-- ----------------------------
-- Table structure for dc3_api
-- ----------------------------
drop table if exists dc3_api;
create table dc3_api
(
    id            bigint unsigned auto_increment primary key not null comment '主键ID',
    api_type_flag tinyint(4) default 0 not null comment 'Api接口类型标识',
    api_name      varchar(128) default ''                   not null comment 'Api接口名称',
    api_code      varchar(256) default ''                   not null comment 'Api接口编号，一般为URL的MD5编码',
    api_ext       json                                      not null comment 'Api接口拓展信息',
    enable_flag   tinyint(4) default 1 not null comment '使能标识',
    tenant_id     bigint       default 0                    not null comment '租户ID',
    remark        varchar(512) default ''                   not null comment '描述',
    creator_id    bigint       default 0                    not null comment '创建者ID',
    creator_name  varchar(128) default ''                   not null comment '创建者名称',
    create_time   datetime     default CURRENT_TIMESTAMP(0) not null comment '创建时间',
    operator_id   bigint       default 0                    not null comment '操作者ID',
    operator_name varchar(128) default ''                   not null comment '操作者名称',
    update_time   datetime     default CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP (0) not null comment '操作时间',
    deleted       tinyint(4) default 0 not null comment '逻辑删除标识,0:未删除,1:已删除',
    INDEX         idx_api_name (api_name) USING BTREE,
    INDEX         idx_api_code (api_code) USING BTREE
) engine = InnoDB
  character set = utf8
  collate = utf8_general_ci
    comment = '接口表';

-- ----------------------------
-- Table structure for dc3_menu
-- ----------------------------
drop table if exists dc3_menu;
create table dc3_menu
(
    id             bigint unsigned auto_increment primary key not null comment '主键ID',
    parent_menu_id bigint       default 0                    not null comment '菜单父级ID',
    menu_type_flag tinyint(4) default 0 not null comment '菜单类型标识',
    menu_name      varchar(128) default ''                   not null comment '菜单名称',
    menu_code      varchar(256) default ''                   not null comment '菜单编号，一般为URL的MD5编码',
    menu_level     tinyint(4) default 0 not null comment '菜单层级',
    menu_index     tinyint(4) default 0 not null comment '菜单顺序',
    menu_ext       json                                      not null comment '菜单拓展信息',
    enable_flag    tinyint(4) default 1 not null comment '使能标识',
    tenant_id      bigint       default 0                    not null comment '租户ID',
    remark         varchar(512) default ''                   not null comment '描述',
    creator_id     bigint       default 0                    not null comment '创建者ID',
    creator_name   varchar(128) default ''                   not null comment '创建者名称',
    create_time    datetime     default CURRENT_TIMESTAMP(0) not null comment '创建时间',
    operator_id    bigint       default 0                    not null comment '操作者ID',
    operator_name  varchar(128) default ''                   not null comment '操作者名称',
    update_time    datetime     default CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP (0) not null comment '操作时间',
    deleted        tinyint(4) default 0 not null comment '逻辑删除标识,0:未删除,1:已删除',
    INDEX          idx_parent_menu_id (parent_menu_id) USING BTREE,
    INDEX          idx_menu_name (menu_name) USING BTREE,
    INDEX          idx_menu_code (menu_code) USING BTREE
) engine = InnoDB
  character set = utf8
  collate = utf8_general_ci
    comment = '菜单表';

-- ----------------------------
-- Table structure for dc3_label
-- ----------------------------
drop table if exists dc3_label;
create table dc3_label
(
    id               bigint unsigned auto_increment primary key not null comment '主键ID',
    label_name       varchar(128) default ''                   not null comment '标签名称',
    color            varchar(128) default ''                   not null comment '标签颜色',
    entity_type_flag tinyint(4) default 0 not null comment '实体类型标识',
    enable_flag      tinyint(4) default 1 not null comment '使能标识',
    tenant_id        bigint       default 0                    not null comment '租户ID',
    remark           varchar(512) default ''                   not null comment '描述',
    creator_id       bigint       default 0                    not null comment '创建者ID',
    creator_name     varchar(128) default ''                   not null comment '创建者名称',
    create_time      datetime     default CURRENT_TIMESTAMP(0) not null comment '创建时间',
    operator_id      bigint       default 0                    not null comment '操作者ID',
    operator_name    varchar(128) default ''                   not null comment '操作者名称',
    update_time      datetime     default CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP (0) not null comment '操作时间',
    deleted          tinyint(4) default 0 not null comment '逻辑删除标识,0:未删除,1:已删除',
    INDEX            idx_label_name (label_name) USING BTREE
) engine = InnoDB
  character set = utf8
  collate = utf8_general_ci
    comment = '标签表';

-- ----------------------------
-- Table structure for dc3_label_bind
-- ----------------------------
drop table if exists dc3_label_bind;
create table dc3_label_bind
(
    id            bigint unsigned auto_increment primary key not null comment '主键ID',
    label_id      bigint       default 0                    not null comment '标签ID',
    entity_id     bigint       default 0                    not null comment '实体ID',
    remark        varchar(512) default ''                   not null comment '描述',
    creator_id    bigint       default 0                    not null comment '创建者ID',
    creator_name  varchar(128) default ''                   not null comment '创建者名称',
    create_time   datetime     default CURRENT_TIMESTAMP(0) not null comment '创建时间',
    operator_id   bigint       default 0                    not null comment '操作者ID',
    operator_name varchar(128) default ''                   not null comment '操作者名称',
    update_time   datetime     default CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP (0) not null comment '操作时间',
    deleted       tinyint(4) default 0 not null comment '逻辑删除标识,0:未删除,1:已删除',
    INDEX         idx_label_id (label_id) USING BTREE,
    INDEX         idx_entity_id (entity_id) USING BTREE
) engine = InnoDB
  character set = utf8
  collate = utf8_general_ci
    comment = '标签关联表';

-- ----------------------------
-- Table structure for dc3_black_ip
-- ----------------------------
drop table if exists dc3_black_ip;
create table dc3_black_ip
(
    id            bigint unsigned auto_increment primary key not null comment '主键ID',
    ip            varchar(128) default ''                   not null comment '黑IP',
    enable_flag   tinyint(4) default 1 not null comment '使能标识',
    tenant_id     bigint       default 0                    not null comment '租户ID',
    remark        varchar(512) default ''                   not null comment '描述',
    creator_id    bigint       default 0                    not null comment '创建者ID',
    creator_name  varchar(128) default ''                   not null comment '创建者名称',
    create_time   datetime     default CURRENT_TIMESTAMP(0) not null comment '创建时间',
    operator_id   bigint       default 0                    not null comment '操作者ID',
    operator_name varchar(128) default ''                   not null comment '操作者名称',
    update_time   datetime     default CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP (0) not null comment '操作时间',
    deleted       tinyint(4) default 0 not null comment '逻辑删除标识,0:未删除,1:已删除',
    INDEX         idx_ip (ip) USING BTREE
) engine = InnoDB
  character set = utf8
  collate = utf8_general_ci
    comment = 'Ip黑名单表';

-- ----------------------------
-- Records of dc3_tenant
-- ----------------------------
INSERT INTO dc3_tenant
VALUES (0, '', 'default', 1, '租户', 0, 'dc3', '2016-10-01 00:00:00', 0, 'dc3', '2016-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_user
-- ----------------------------
INSERT INTO dc3_user
VALUES (0, 'pnoker', 0, 0, 1, '用户', 0, 'dc3', '2016-10-01 00:00:00', 0, 'dc3', '2016-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_user
-- ----------------------------
INSERT INTO dc3_user_password
VALUES (0, '10e339be1130a90dc1b9ff0332abced6', '用户密码', 0, 'dc3', '2016-10-01 00:00:00', 0, 'dc3',
        '2016-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_user
-- ----------------------------
INSERT INTO dc3_user_ext
VALUES (0, '张红元', 'pnoker', '18304071393', 'pnokers@icloud.com', '用户拓展信息', 0, 'dc3', '2016-10-01 00:00:00', 0,
        'dc3', '2016-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_tenant_bind
-- ----------------------------
INSERT INTO dc3_tenant_bind
VALUES (0, 0, 0, '租户,用户关联', 0, 'dc3', '2016-10-01 00:00:00', 0, 'dc3', '2016-10-01 00:00:00', 0);

CREATE DATABASE dc3_nacos;
USE dc3_nacos;

/******************************************/
/*   数据库全名 = nacos_config   */
/*   表名称 = config_info   */
/******************************************/
CREATE TABLE `config_info`
(
    `id`                 bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
    `data_id`            varchar(255) NOT NULL COMMENT 'data_id',
    `group_id`           varchar(128)          DEFAULT NULL,
    `content`            longtext     NOT NULL COMMENT 'content',
    `md5`                varchar(32)           DEFAULT NULL COMMENT 'md5',
    `gmt_create`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `src_user`           text COMMENT 'source user',
    `src_ip`             varchar(50)           DEFAULT NULL COMMENT 'source ip',
    `app_name`           varchar(128)          DEFAULT NULL,
    `tenant_id`          varchar(128)          DEFAULT '' COMMENT '租户字段',
    `c_desc`             varchar(256)          DEFAULT NULL,
    `c_use`              varchar(64)           DEFAULT NULL,
    `effect`             varchar(64)           DEFAULT NULL,
    `type`               varchar(64)           DEFAULT NULL,
    `c_schema`           text,
    `encrypted_data_key` text         NOT NULL COMMENT '秘钥',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_configinfo_datagrouptenant` (`data_id`,`group_id`,`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci COMMENT='config_info';

/******************************************/
/*   数据库全名 = nacos_config   */
/*   表名称 = config_info_aggr   */
/******************************************/
CREATE TABLE `config_info_aggr`
(
    `id`           bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
    `data_id`      varchar(255) NOT NULL COMMENT 'data_id',
    `group_id`     varchar(128) NOT NULL COMMENT 'group_id',
    `datum_id`     varchar(255) NOT NULL COMMENT 'datum_id',
    `content`      longtext     NOT NULL COMMENT '内容',
    `gmt_modified` datetime     NOT NULL COMMENT '修改时间',
    `app_name`     varchar(128) DEFAULT NULL,
    `tenant_id`    varchar(128) DEFAULT '' COMMENT '租户字段',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_configinfoaggr_datagrouptenantdatum` (`data_id`,`group_id`,`tenant_id`,`datum_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci COMMENT='增加租户字段';


/******************************************/
/*   数据库全名 = nacos_config   */
/*   表名称 = config_info_beta   */
/******************************************/
CREATE TABLE `config_info_beta`
(
    `id`                 bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
    `data_id`            varchar(255) NOT NULL COMMENT 'data_id',
    `group_id`           varchar(128) NOT NULL COMMENT 'group_id',
    `app_name`           varchar(128)          DEFAULT NULL COMMENT 'app_name',
    `content`            longtext     NOT NULL COMMENT 'content',
    `beta_ips`           varchar(1024)         DEFAULT NULL COMMENT 'betaIps',
    `md5`                varchar(32)           DEFAULT NULL COMMENT 'md5',
    `gmt_create`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `src_user`           text COMMENT 'source user',
    `src_ip`             varchar(50)           DEFAULT NULL COMMENT 'source ip',
    `tenant_id`          varchar(128)          DEFAULT '' COMMENT '租户字段',
    `encrypted_data_key` text         NOT NULL COMMENT '秘钥',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_configinfobeta_datagrouptenant` (`data_id`,`group_id`,`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci COMMENT='config_info_beta';

/******************************************/
/*   数据库全名 = nacos_config   */
/*   表名称 = config_info_tag   */
/******************************************/
CREATE TABLE `config_info_tag`
(
    `id`           bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
    `data_id`      varchar(255) NOT NULL COMMENT 'data_id',
    `group_id`     varchar(128) NOT NULL COMMENT 'group_id',
    `tenant_id`    varchar(128)          DEFAULT '' COMMENT 'tenant_id',
    `tag_id`       varchar(128) NOT NULL COMMENT 'tag_id',
    `app_name`     varchar(128)          DEFAULT NULL COMMENT 'app_name',
    `content`      longtext     NOT NULL COMMENT 'content',
    `md5`          varchar(32)           DEFAULT NULL COMMENT 'md5',
    `gmt_create`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `src_user`     text COMMENT 'source user',
    `src_ip`       varchar(50)           DEFAULT NULL COMMENT 'source ip',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_configinfotag_datagrouptenanttag` (`data_id`,`group_id`,`tenant_id`,`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci COMMENT='config_info_tag';

/******************************************/
/*   数据库全名 = nacos_config   */
/*   表名称 = config_tags_relation   */
/******************************************/
CREATE TABLE `config_tags_relation`
(
    `id`        bigint(20) NOT NULL COMMENT 'id',
    `tag_name`  varchar(128) NOT NULL COMMENT 'tag_name',
    `tag_type`  varchar(64)  DEFAULT NULL COMMENT 'tag_type',
    `data_id`   varchar(255) NOT NULL COMMENT 'data_id',
    `group_id`  varchar(128) NOT NULL COMMENT 'group_id',
    `tenant_id` varchar(128) DEFAULT '' COMMENT 'tenant_id',
    `nid`       bigint(20) NOT NULL AUTO_INCREMENT,
    PRIMARY KEY (`nid`),
    UNIQUE KEY `uk_configtagrelation_configidtag` (`id`,`tag_name`,`tag_type`),
    KEY         `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci COMMENT='config_tag_relation';

/******************************************/
/*   数据库全名 = nacos_config   */
/*   表名称 = group_capacity   */
/******************************************/
CREATE TABLE `group_capacity`
(
    `id`                bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `group_id`          varchar(128) NOT NULL DEFAULT '' COMMENT 'Group ID，空字符表示整个集群',
    `quota`             int(10) unsigned NOT NULL DEFAULT '0' COMMENT '配额，0表示使用默认值',
    `usage`             int(10) unsigned NOT NULL DEFAULT '0' COMMENT '使用量',
    `max_size`          int(10) unsigned NOT NULL DEFAULT '0' COMMENT '单个配置大小上限，单位为字节，0表示使用默认值',
    `max_aggr_count`    int(10) unsigned NOT NULL DEFAULT '0' COMMENT '聚合子配置最大个数，，0表示使用默认值',
    `max_aggr_size`     int(10) unsigned NOT NULL DEFAULT '0' COMMENT '单个聚合数据的子配置大小上限，单位为字节，0表示使用默认值',
    `max_history_count` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '最大变更历史数量',
    `gmt_create`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_id` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci COMMENT='集群、各Group容量信息表';

/******************************************/
/*   数据库全名 = nacos_config   */
/*   表名称 = his_config_info   */
/******************************************/
CREATE TABLE `his_config_info`
(
    `id`                 bigint(20) unsigned NOT NULL,
    `nid`                bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `data_id`            varchar(255) NOT NULL,
    `group_id`           varchar(128) NOT NULL,
    `app_name`           varchar(128)          DEFAULT NULL COMMENT 'app_name',
    `content`            longtext     NOT NULL,
    `md5`                varchar(32)           DEFAULT NULL,
    `gmt_create`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `src_user`           text,
    `src_ip`             varchar(50)           DEFAULT NULL,
    `op_type`            char(10)              DEFAULT NULL,
    `tenant_id`          varchar(128)          DEFAULT '' COMMENT '租户字段',
    `encrypted_data_key` text         NOT NULL COMMENT '秘钥',
    PRIMARY KEY (`nid`),
    KEY                  `idx_gmt_create` (`gmt_create`),
    KEY                  `idx_gmt_modified` (`gmt_modified`),
    KEY                  `idx_did` (`data_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci COMMENT='多租户改造';


/******************************************/
/*   数据库全名 = nacos_config   */
/*   表名称 = tenant_capacity   */
/******************************************/
CREATE TABLE `tenant_capacity`
(
    `id`                bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`         varchar(128) NOT NULL DEFAULT '' COMMENT 'Tenant ID',
    `quota`             int(10) unsigned NOT NULL DEFAULT '0' COMMENT '配额，0表示使用默认值',
    `usage`             int(10) unsigned NOT NULL DEFAULT '0' COMMENT '使用量',
    `max_size`          int(10) unsigned NOT NULL DEFAULT '0' COMMENT '单个配置大小上限，单位为字节，0表示使用默认值',
    `max_aggr_count`    int(10) unsigned NOT NULL DEFAULT '0' COMMENT '聚合子配置最大个数',
    `max_aggr_size`     int(10) unsigned NOT NULL DEFAULT '0' COMMENT '单个聚合数据的子配置大小上限，单位为字节，0表示使用默认值',
    `max_history_count` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '最大变更历史数量',
    `gmt_create`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci COMMENT='租户容量信息表';


CREATE TABLE `tenant_info`
(
    `id`            bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
    `kp`            varchar(128) NOT NULL COMMENT 'kp',
    `tenant_id`     varchar(128) default '' COMMENT 'tenant_id',
    `tenant_name`   varchar(128) default '' COMMENT 'tenant_name',
    `tenant_desc`   varchar(256) DEFAULT NULL COMMENT 'tenant_desc',
    `create_source` varchar(32)  DEFAULT NULL COMMENT 'create_source',
    `gmt_create`    bigint(20) NOT NULL COMMENT '创建时间',
    `gmt_modified`  bigint(20) NOT NULL COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_info_kptenantid` (`kp`,`tenant_id`),
    KEY             `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci COMMENT='tenant_info';

CREATE TABLE `users`
(
    `username` varchar(50)  NOT NULL PRIMARY KEY,
    `password` varchar(500) NOT NULL,
    `enabled`  boolean      NOT NULL
);

CREATE TABLE `roles`
(
    `username` varchar(50) NOT NULL,
    `role`     varchar(50) NOT NULL,
    UNIQUE INDEX `idx_user_role` (`username` ASC, `role` ASC) USING BTREE
);

CREATE TABLE `permissions`
(
    `role`     varchar(50)  NOT NULL,
    `resource` varchar(255) NOT NULL,
    `action`   varchar(8)   NOT NULL,
    UNIQUE INDEX `uk_role_permission` (`role`,`resource`,`action`) USING BTREE
);

INSERT INTO users (username, password, enabled)
VALUES ('dc3', '$2a$10$wM3B4eTqtsbD0GQzPcSdnOVFCTY/eB1VTJqnLbQWq/Xk/PVyCz13i', TRUE);

INSERT INTO roles (username, role)
VALUES ('dc3', 'ROLE_ADMIN');

SET FOREIGN_KEY_CHECKS = 1;