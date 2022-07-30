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

CREATE DATABASE `dc3`;

USE `dc3`;

-- ----------------------------
-- Table structure for dc3_driver
-- ----------------------------
DROP TABLE IF EXISTS `dc3_driver`;
CREATE TABLE `dc3_driver`
(
    `id`           varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '主键ID',
    `name`         varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '协议名称',
    `service_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '协议服务名称',
    `type`         varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT 'driver' COMMENT '类型，deriver、gateway...',
    `host`         varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '主机IP',
    `port`         int(11)                                                 NOT NULL COMMENT '端口',
    `enable`       tinyint(4)                                              NULL DEFAULT 1 COMMENT '是否可用',
    `tenant_id`    varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '租户ID',
    `description`  varchar(380) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '描述',
    `create_time`  datetime(0)                                             NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `update_time`  datetime(0)                                             NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '修改时间',
    `deleted`      tinyint(4)                                              NULL DEFAULT 0 COMMENT '逻辑删标识',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `name` (`name`) USING BTREE,
    INDEX `tenant_id` (`tenant_id`) USING BTREE,
    CONSTRAINT `dc3_driver_ibfk_1` FOREIGN KEY (`tenant_id`) REFERENCES `dc3_tenant` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '协议驱动表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dc3_driver_attribute
-- ----------------------------
DROP TABLE IF EXISTS `dc3_driver_attribute`;
CREATE TABLE `dc3_driver_attribute`
(
    `id`           varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '主键ID',
    `display_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '显示名称',
    `name`         varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '名称',
    `type`         varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '类型',
    `value`        varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '默认值',
    `driver_id`    varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '驱动ID',
    `description`  varchar(380) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '描述',
    `create_time`  datetime(0)                                             NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `update_time`  datetime(0)                                             NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '修改时间',
    `deleted`      tinyint(4)                                              NULL DEFAULT 0 COMMENT '逻辑删标识',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `driver_id` (`driver_id`) USING BTREE,
    CONSTRAINT `dc3_driver_attribute_ibfk_1` FOREIGN KEY (`driver_id`) REFERENCES `dc3_driver` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '连接配置信息表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dc3_point_attribute
-- ----------------------------
DROP TABLE IF EXISTS `dc3_point_attribute`;
CREATE TABLE `dc3_point_attribute`
(
    `id`           varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '主键ID',
    `display_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '显示名称',
    `name`         varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '名称',
    `type`         varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '类型',
    `value`        varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '默认值',
    `driver_id`    varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '驱动ID',
    `description`  varchar(380) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '描述',
    `create_time`  datetime(0)                                             NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `update_time`  datetime(0)                                             NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '修改时间',
    `deleted`      tinyint(4)                                              NULL DEFAULT 0 COMMENT '逻辑删标识',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `driver_id` (`driver_id`) USING BTREE,
    CONSTRAINT `dc3_point_attribute_ibfk_1` FOREIGN KEY (`driver_id`) REFERENCES `dc3_driver` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '模板配置信息表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dc3_profile
-- ----------------------------
DROP TABLE IF EXISTS `dc3_profile`;
CREATE TABLE `dc3_profile`
(
    `id`          varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '主键ID',
    `name`        varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '模板名称',
    `share`       tinyint(4)                                              NULL DEFAULT 0 COMMENT '公有/私有模板标识',
    `type`        tinyint(4)                                              NULL DEFAULT 1 COMMENT '系统创建/用户创建/驱动创建',
    `enable`      tinyint(4)                                              NULL DEFAULT 1 COMMENT '是否可用',
    `group_id`    varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '分组ID',
    `device_id`   varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '设备ID',
    `tenant_id`   varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '租户ID',
    `description` varchar(380) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '描述',
    `create_time` datetime(0)                                             NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `update_time` datetime(0)                                             NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '修改时间',
    `deleted`     tinyint(4)                                              NULL DEFAULT 0 COMMENT '逻辑删标识',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `name` (`name`) USING BTREE,
    INDEX `group_id` (`group_id`) USING BTREE,
    INDEX `device_id` (`device_id`) USING BTREE,
    INDEX `tenant_id` (`tenant_id`) USING BTREE,
    CONSTRAINT `dc3_profile_ibfk_1` FOREIGN KEY (`group_id`) REFERENCES `dc3_group` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `dc3_profile_ibfk_2` FOREIGN KEY (`device_id`) REFERENCES `dc3_group` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `dc3_profile_ibfk_3` FOREIGN KEY (`tenant_id`) REFERENCES `dc3_tenant` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '设备模板表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dc3_profile_bind
-- ----------------------------
DROP TABLE IF EXISTS `dc3_profile_bind`;
CREATE TABLE `dc3_profile_bind`
(
    `id`          varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '主键ID',
    `profile_id`  varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '模版ID',
    `device_id`   varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '设备ID',
    `description` varchar(380) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '描述',
    `create_time` datetime(0)                                             NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `update_time` datetime(0)                                             NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '修改时间',
    `deleted`     tinyint(4)                                              NULL DEFAULT 0 COMMENT '逻辑删标识',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `profile_id` (`profile_id`) USING BTREE,
    INDEX `device_id` (`device_id`) USING BTREE,
    CONSTRAINT `dc3_profile_bind_ibfk_1` FOREIGN KEY (`profile_id`) REFERENCES `dc3_profile` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `dc3_profile_bind_ibfk_2` FOREIGN KEY (`device_id`) REFERENCES `dc3_device` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '设备与模版映射关联表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dc3_driver_info
-- ----------------------------
DROP TABLE IF EXISTS `dc3_driver_info`;
CREATE TABLE `dc3_driver_info`
(
    `id`                  varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '主键ID',
    `driver_attribute_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '连接配置ID',
    `value`               varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '值',
    `device_id`           varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '设备ID',
    `description`         varchar(380) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '描述',
    `create_time`         datetime(0)                                             NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `update_time`         datetime(0)                                             NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '修改时间',
    `deleted`             tinyint(4)                                              NULL DEFAULT 0 COMMENT '逻辑删标识',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `driver_attribute_id` (`driver_attribute_id`) USING BTREE,
    INDEX `device_id` (`device_id`) USING BTREE,
    CONSTRAINT `dc3_driver_info_ibfk_1` FOREIGN KEY (`driver_attribute_id`) REFERENCES `dc3_driver_attribute` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `dc3_driver_info_ibfk_2` FOREIGN KEY (`device_id`) REFERENCES `dc3_device` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '模板连接配置信息表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dc3_point
-- ----------------------------
DROP TABLE IF EXISTS `dc3_point`;
CREATE TABLE `dc3_point`
(
    `id`          varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '主键ID',
    `name`        varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '位号名称',
    `type`        varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT '' COMMENT '数据类型：string\int\double\float\long\boolean',
    `rw`          tinyint(4)                                              NULL DEFAULT 0 COMMENT '读写标识：0读，1写，2读写',
    `base`        float(255, 6)                                           NULL DEFAULT 0 COMMENT '基础值',
    `minimum`     float(255, 6)                                           NULL DEFAULT NULL COMMENT '最小值',
    `maximum`     float(255, 6)                                           NULL DEFAULT NULL COMMENT '最大值',
    `multiple`    float(255, 6)                                           NULL DEFAULT 1 COMMENT '倍数',
    `accrue`      tinyint(4)                                              NULL DEFAULT 0 COMMENT '累计标识',
    `format`      varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT '' COMMENT '格式数据，Jave格式 %.3f',
    `unit`        varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT '' COMMENT '单位',
    `enable`      tinyint(4)                                              NULL DEFAULT 1 COMMENT '是否可用',
    `profile_id`  varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '模板ID',
    `group_id`    varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '分组ID',
    `tenant_id`   varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '租户ID',
    `description` varchar(380) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '描述',
    `create_time` datetime(0)                                             NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `update_time` datetime(0)                                             NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '修改时间',
    `deleted`     tinyint(4)                                              NULL DEFAULT 0 COMMENT '逻辑删标识',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `profile_id` (`profile_id`) USING BTREE,
    INDEX `group_id` (`group_id`) USING BTREE,
    INDEX `tenant_id` (`tenant_id`) USING BTREE,
    CONSTRAINT `dc3_point_ibfk_1` FOREIGN KEY (`profile_id`) REFERENCES `dc3_profile` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `dc3_point_ibfk_2` FOREIGN KEY (`group_id`) REFERENCES `dc3_group` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `dc3_point_ibfk_3` FOREIGN KEY (`tenant_id`) REFERENCES `dc3_tenant` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '设备位号表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dc3_group
-- ----------------------------
DROP TABLE IF EXISTS `dc3_group`;
CREATE TABLE `dc3_group`
(
    `id`          varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '主键ID',
    `name`        varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '分组名称',
    `group_id`    varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '父分组ID',
    `position`    int(11)                                                 NULL DEFAULT NULL COMMENT '分组排序位置',
    `tenant_id`   varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '租户ID',
    `description` varchar(380) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '描述',
    `create_time` datetime(0)                                             NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `update_time` datetime(0)                                             NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '修改时间',
    `deleted`     tinyint(4)                                              NULL DEFAULT 0 COMMENT '逻辑删标识',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `name` (`name`) USING BTREE,
    INDEX `group_id` (`group_id`) USING BTREE,
    INDEX `tenant_id` (`tenant_id`) USING BTREE,
    CONSTRAINT `dc3_group_ibfk_1` FOREIGN KEY (`group_id`) REFERENCES `dc3_group` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `dc3_group_ibfk_2` FOREIGN KEY (`tenant_id`) REFERENCES `dc3_tenant` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '分组表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dc3_device
-- ----------------------------
DROP TABLE IF EXISTS `dc3_device`;
CREATE TABLE `dc3_device`
(
    `id`          varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '主键ID',
    `name`        varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '设备名称',
    `multi`       tinyint(4)                                              NULL DEFAULT 0 COMMENT '位号数据是否结构化',
    `enable`      tinyint(4)                                              NULL DEFAULT 1 COMMENT '是否可用',
    `driver_id`   varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '驱动ID',
    `group_id`    varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '分组ID',
    `tenant_id`   varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '租户ID',
    `description` varchar(380) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '描述',
    `create_time` datetime(0)                                             NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `update_time` datetime(0)                                             NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '修改时间',
    `deleted`     tinyint(4)                                              NULL DEFAULT 0 COMMENT '逻辑删标识',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `name` (`name`) USING BTREE,
    INDEX `driver_id` (`driver_id`) USING BTREE,
    INDEX `group_id` (`group_id`) USING BTREE,
    INDEX `tenant_id` (`tenant_id`) USING BTREE,
    CONSTRAINT `dc3_device_ibfk_1` FOREIGN KEY (`driver_id`) REFERENCES `dc3_driver` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `dc3_device_ibfk_2` FOREIGN KEY (`group_id`) REFERENCES `dc3_group` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `dc3_device_ibfk_3` FOREIGN KEY (`tenant_id`) REFERENCES `dc3_tenant` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '设备表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dc3_point_info
-- ----------------------------
DROP TABLE IF EXISTS `dc3_point_info`;
CREATE TABLE `dc3_point_info`
(
    `id`                 varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '主键ID',
    `point_attribute_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '模板配置ID',
    `value`              varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '值',
    `device_id`          varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '设备ID',
    `point_id`           varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '位号ID',
    `description`        varchar(380) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '描述',
    `create_time`        datetime(0)                                             NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `update_time`        datetime(0)                                             NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '修改时间',
    `deleted`            tinyint(4)                                              NULL DEFAULT 0 COMMENT '逻辑删标识',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `point_attribute_id` (`point_attribute_id`) USING BTREE,
    INDEX `device_id` (`device_id`) USING BTREE,
    INDEX `point_id` (`point_id`) USING BTREE,
    CONSTRAINT `dc3_point_info_ibfk_1` FOREIGN KEY (`point_attribute_id`) REFERENCES `dc3_point_attribute` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `dc3_point_info_ibfk_2` FOREIGN KEY (`device_id`) REFERENCES `dc3_device` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `dc3_point_info_ibfk_3` FOREIGN KEY (`point_id`) REFERENCES `dc3_point` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '位号配置信息表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dc3_tenant
-- ----------------------------
DROP TABLE IF EXISTS `dc3_tenant`;
CREATE TABLE `dc3_tenant`
(
    `id`          varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '主键ID',
    `name`        varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '租户',
    `enable`      tinyint(4)                                              NULL DEFAULT 1 COMMENT '是否可用',
    `description` varchar(380) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '描述',
    `create_time` datetime(0)                                             NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `update_time` datetime(0)                                             NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '修改时间',
    `deleted`     tinyint(4)                                              NULL DEFAULT 0 COMMENT '逻辑删标识',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `name` (`name`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '租户表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dc3_tenant_bind
-- ----------------------------
DROP TABLE IF EXISTS `dc3_tenant_bind`;
CREATE TABLE `dc3_tenant_bind`
(
    `id`          varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '主键ID',
    `tenant_id`   varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '租户ID',
    `user_id`     varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '用户ID',
    `description` varchar(380) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '描述',
    `create_time` datetime(0)                                             NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `update_time` datetime(0)                                             NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '修改时间',
    `deleted`     tinyint(4)                                              NULL DEFAULT 0 COMMENT '逻辑删标识',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `tenant_id` (`tenant_id`) USING BTREE,
    INDEX `user_id` (`user_id`) USING BTREE,
    CONSTRAINT `dc3_tenant_bind_ibfk_1` FOREIGN KEY (`tenant_id`) REFERENCES `dc3_tenant` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `dc3_tenant_bind_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `dc3_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '租户关联表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dc3_user
-- ----------------------------
DROP TABLE IF EXISTS `dc3_user`;
CREATE TABLE `dc3_user`
(
    `id`          varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '主键ID',
    `name`        varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '用户名，需要加密存储，可用于登录',
    `phone`       varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '手机号，需要加密存储，可用于登录',
    `email`       varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '邮箱，需要加密存储，可用于登录',
    `password`    varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '密码，需要加密存储',
    `enable`      tinyint(4)                                              NULL DEFAULT 1 COMMENT '是否可用',
    `description` varchar(380) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '描述',
    `create_time` datetime(0)                                             NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `update_time` datetime(0)                                             NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '修改时间',
    `deleted`     tinyint(4)                                              NULL DEFAULT 0 COMMENT '逻辑删标识',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `name` (`name`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '用户表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dc3_rtmp
-- ----------------------------
DROP TABLE IF EXISTS `dc3_rtmp`;
CREATE TABLE `dc3_rtmp`
(
    `id`          varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '主键ID',
    `name`        varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '名称',
    `rtsp_url`    varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '源视频链接，在线视频或本地文件',
    `rtmp_url`    varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT 'rtp播放链接,填写后缀即可',
    `command`     varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT 'cmd运行模板',
    `video_type`  tinyint(4)                                              NULL DEFAULT 0 COMMENT '摄像头类型',
    `run`         tinyint(4)                                              NULL DEFAULT 0 COMMENT '状态，0停止，1启动',
    `auto_start`  tinyint(4)                                              NULL DEFAULT 0 COMMENT '自启动',
    `tenant_id`   varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '租户ID',
    `description` varchar(380) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '描述',
    `create_time` datetime(0)                                             NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `update_time` datetime(0)                                             NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '修改时间',
    `deleted`     tinyint(4)                                              NULL DEFAULT 0 COMMENT '逻辑删标识',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `name` (`name`) USING BTREE,
    INDEX `tenant_id` (`tenant_id`) USING BTREE,
    CONSTRAINT `dc3_rtmp_ibfk_1` FOREIGN KEY (`tenant_id`) REFERENCES `dc3_tenant` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = 'rtmp表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dc3_label
-- ----------------------------
DROP TABLE IF EXISTS `dc3_label`;
CREATE TABLE `dc3_label`
(
    `id`          varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '主键ID',
    `name`        varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT 'label名称',
    `color`       varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT '' COMMENT '标签颜色',
    `tenant_id`   varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '租户ID',
    `description` varchar(380) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '描述',
    `create_time` datetime(0)                                             NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `update_time` datetime(0)                                             NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '修改时间',
    `deleted`     tinyint(4)                                              NULL DEFAULT 0 COMMENT '逻辑删标识',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `name` (`name`) USING BTREE,
    INDEX `tenant_id` (`tenant_id`) USING BTREE,
    CONSTRAINT `dc3_label_ibfk_1` FOREIGN KEY (`tenant_id`) REFERENCES `dc3_tenant` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '标签表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dc3_label_bind
-- ----------------------------
DROP TABLE IF EXISTS `dc3_label_bind`;
CREATE TABLE `dc3_label_bind`
(
    `id`          varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '主键ID',
    `label_id`    varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '标签ID',
    `entity_id`   varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '实体ID，可为设备、设备组、模板、点位、用户',
    `type`        varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '关联实体类型，user、device、profile、group、point',
    `description` varchar(380) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '描述',
    `create_time` datetime(0)                                             NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `update_time` datetime(0)                                             NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '修改时间',
    `deleted`     tinyint(4)                                              NULL DEFAULT 0 COMMENT '逻辑删标识',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `label_id` (`label_id`) USING BTREE,
    INDEX `entity_id` (`entity_id`) USING BTREE,
    CONSTRAINT `dc3_label_bind_ibfk_1` FOREIGN KEY (`label_id`) REFERENCES `dc3_label` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '标签关联表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dc3_black_ip
-- ----------------------------
DROP TABLE IF EXISTS `dc3_black_ip`;
CREATE TABLE `dc3_black_ip`
(
    `id`          varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '主键ID',
    `ip`          varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT 'ip',
    `enable`      tinyint(4)                                              NULL DEFAULT 1 COMMENT '是否可用',
    `description` varchar(380) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '描述',
    `create_time` datetime(0)                                             NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `update_time` datetime(0)                                             NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '修改时间',
    `deleted`     tinyint(4)                                              NULL DEFAULT 0 COMMENT '逻辑删标识',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `ip` (`ip`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = 'Ip黑名单表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dc3_code_bind
-- ----------------------------
DROP TABLE IF EXISTS `dc3_code_bind`;
CREATE TABLE `dc3_code_bind`
(
    `id`          varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '主键ID',
    `code_id`     varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '编码ID，全局唯一',
    `entity_id`   varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '实体ID，可为设备、用户',
    `type`        tinyint(4)                                              NOT NULL COMMENT '关联实体类型，device、user',
    `enable`      tinyint(4)                                              NULL DEFAULT 1 COMMENT '是否可用',
    `tenant_id`   varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '租户ID',
    `description` varchar(380) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '描述',
    `create_time` datetime(0)                                             NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `update_time` datetime(0)                                             NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '修改时间',
    `deleted`     tinyint(4)                                              NULL DEFAULT 0 COMMENT '逻辑删标识',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `code_id` (`code_id`) USING BTREE,
    INDEX `entity_id` (`entity_id`) USING BTREE,
    INDEX `tenant_id` (`tenant_id`) USING BTREE,
    CONSTRAINT `dc3_code_bind_ibfk_1` FOREIGN KEY (`code_id`) REFERENCES `dc3_label` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `dc3_code_bind_ibfk_2` FOREIGN KEY (`tenant_id`) REFERENCES `dc3_tenant` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '编码关联表'
  ROW_FORMAT = Dynamic;

/*
 * Copyright 2022 Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

-- ----------------------------
-- Records of dc3_tenant
-- ----------------------------
INSERT INTO `dc3_tenant`
VALUES ('-1', 'default', 1, '平台开发默认租户', '2016-10-01 00:00:00', '2016-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_user
-- ----------------------------
INSERT INTO `dc3_user`
VALUES ('-1', 'pnoker', '18304071393', 'pnokers@icloud.com', '10e339be1130a90dc1b9ff0332abced6', 1, '平台开发者账号', '2016-10-01 00:00:00', '2016-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_tenant_bind
-- ----------------------------
INSERT INTO `dc3_tenant_bind`
VALUES ('-1', '-1', '-1', '平台开发默认租户和默认用户关联', '2016-10-01 00:00:00', '2016-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_rtmp
-- ----------------------------
INSERT INTO `dc3_rtmp`
VALUES ('-1', '本地测试视频', 'D:/FFmpeg/bin/190314223540373995.mp4', 'rtmp://dc3-nginx:1935/rtmp/190314223540373995_local',
        '{exe} -re -stream_loop -1 -i {rtsp_url} -vcodec copy -acodec copy -f flv -y {rtmp_url}', 0, 0, 0, '-1', '本地MP4视频文件（复仇者联盟预告），用于测试使用', '2016-10-01 00:00:00',
        '2016-10-01 00:00:00', 0);
INSERT INTO `dc3_rtmp`
VALUES ('-2', '在线测试视频', 'http://vfx.mtime.cn/Video/2019/03/19/mp4/190319104618910544.mp4', 'rtmp://dc3-nginx:1935/rtmp/190314223540373995_online',
        '{exe} -re -stream_loop -1 -i {rtsp_url} -vcodec copy -acodec copy -f flv -y {rtmp_url}', 0, 0, 0, '-1', '在线视频流（无限动力预告），用于测试使用', '2016-10-01 00:00:00',
        '2016-10-01 00:00:00', 0);

SET FOREIGN_KEY_CHECKS = 1;