/*
  Copyright 2019 Pnoker. All Rights Reserved.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE `dc3`;

USE `dc3`;

-- ----------------------------
-- Table structure for dc3_driver
-- ----------------------------
DROP TABLE IF EXISTS `dc3_driver`;
CREATE TABLE `dc3_driver`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT ' 协议名称',
  `service_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '协议服务名称',
  `host` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '主机IP',
  `port` int(11) NOT NULL COMMENT '端口',
  `description` varchar(380) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '描述',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '修改时间',
  `deleted` tinyint(4) NULL DEFAULT 0 COMMENT '逻辑删标识',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '协议驱动表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dc3_driver_attribute
-- ----------------------------
DROP TABLE IF EXISTS `dc3_driver_attribute`;
CREATE TABLE `dc3_driver_attribute`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `display_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '显示名称',
  `name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '名称',
  `type` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '类型',
  `value` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '默认值',
  `driver_id` bigint(20) NOT NULL COMMENT '驱动ID',
  `description` varchar(380) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '描述',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '修改时间',
  `deleted` tinyint(4) NULL DEFAULT 0 COMMENT '逻辑删标识',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `driver_id`(`driver_id`) USING BTREE,
  CONSTRAINT `dc3_driver_attribute_ibfk_1` FOREIGN KEY (`driver_id`) REFERENCES `dc3_driver` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '连接配置信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dc3_point_attribute
-- ----------------------------
DROP TABLE IF EXISTS `dc3_point_attribute`;
CREATE TABLE `dc3_point_attribute`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `display_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '显示名称',
  `name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '名称',
  `type` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '类型',
  `value` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '默认值',
  `driver_id` bigint(20) NOT NULL COMMENT '驱动ID',
  `description` varchar(380) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '描述',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '修改时间',
  `deleted` tinyint(4) NULL DEFAULT 0 COMMENT '逻辑删标识',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `driver_id`(`driver_id`) USING BTREE,
  CONSTRAINT `dc3_point_attribute_ibfk_1` FOREIGN KEY (`driver_id`) REFERENCES `dc3_driver` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '模板配置信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dc3_profile
-- ----------------------------
DROP TABLE IF EXISTS `dc3_profile`;
CREATE TABLE `dc3_profile`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '模板名称',
  `share` tinyint(4) NULL DEFAULT 0 COMMENT '公有/私有模板标识',
  `driver_id` bigint(20) NOT NULL COMMENT '驱动ID',
  `description` varchar(380) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '描述',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '修改时间',
  `deleted` tinyint(4) NULL DEFAULT 0 COMMENT '逻辑删标识',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `name`(`name`) USING BTREE,
  INDEX `driver_id`(`driver_id`) USING BTREE,
  CONSTRAINT `dc3_profile_ibfk_1` FOREIGN KEY (`driver_id`) REFERENCES `dc3_driver` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '设备模板表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dc3_driver_info
-- ----------------------------
DROP TABLE IF EXISTS `dc3_driver_info`;
CREATE TABLE `dc3_driver_info`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `driver_attribute_id` bigint(20) NOT NULL COMMENT '连接配置ID',
  `value` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '值',
  `profile_id` bigint(20) NOT NULL COMMENT '模板ID',
  `description` varchar(380) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '描述',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '修改时间',
  `deleted` tinyint(4) NULL DEFAULT 0 COMMENT '逻辑删标识',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `driver_attribute_id`(`driver_attribute_id`) USING BTREE,
  INDEX `profile_id`(`profile_id`) USING BTREE,
  CONSTRAINT `dc3_driver_info_ibfk_1` FOREIGN KEY (`driver_attribute_id`) REFERENCES `dc3_driver_attribute` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `dc3_driver_info_ibfk_2` FOREIGN KEY (`profile_id`) REFERENCES `dc3_profile` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '模板连接配置信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dc3_point
-- ----------------------------
DROP TABLE IF EXISTS `dc3_point`;
CREATE TABLE `dc3_point`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '位号名称',
  `type` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '数据类型：string\int\double\float\long\boolean',
  `rw` tinyint(4) NULL DEFAULT 0 COMMENT '读写标识：0读，1写，2读写',
  `base` float NULL DEFAULT 0 COMMENT '基础值',
  `minimum` float NULL DEFAULT NULL COMMENT '最小值',
  `maximum` float NULL DEFAULT NULL COMMENT '最大值',
  `multiple` float(255, 0) NULL DEFAULT 1 COMMENT '倍数',
  `accrue` tinyint(4) NULL DEFAULT 0 COMMENT '累计标识',
  `format` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '格式数据，Jave格式 %.3f',
  `unit` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '单位',
  `profile_id` bigint(20) NULL DEFAULT NULL COMMENT '模板ID',
  `description` varchar(380) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '描述',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '修改时间',
  `deleted` tinyint(4) NULL DEFAULT 0 COMMENT '逻辑删标识',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `profile_id`(`profile_id`) USING BTREE,
  CONSTRAINT `dc3_point_ibfk_1` FOREIGN KEY (`profile_id`) REFERENCES `dc3_profile` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '设备位号表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dc3_group
-- ----------------------------
DROP TABLE IF EXISTS `dc3_group`;
CREATE TABLE `dc3_group`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '分组名称',
  `description` varchar(380) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '描述',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '修改时间',
  `deleted` tinyint(4) NULL DEFAULT 0 COMMENT '逻辑删标识',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '分组表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dc3_device
-- ----------------------------
DROP TABLE IF EXISTS `dc3_device`;
CREATE TABLE `dc3_device`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '设备名称',
  `code` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '设备编码',
  `status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '设备状态（离线0，在线1，维护2，故障3，失效4）',
  `profile_id` bigint(20) NULL DEFAULT NULL COMMENT '模板ID',
  `group_id` bigint(20) NULL DEFAULT NULL COMMENT '分组ID',
  `description` varchar(380) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '描述',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '修改时间',
  `deleted` tinyint(4) NULL DEFAULT 0 COMMENT '逻辑删标识',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `name`(`name`) USING BTREE,
  INDEX `code`(`code`) USING BTREE,
  INDEX `profile_id`(`profile_id`) USING BTREE,
  INDEX `group_id`(`group_id`) USING BTREE,
  CONSTRAINT `dc3_device_ibfk_1` FOREIGN KEY (`profile_id`) REFERENCES `dc3_profile` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `dc3_device_ibfk_2` FOREIGN KEY (`group_id`) REFERENCES `dc3_group` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '设备表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dc3_point_info
-- ----------------------------
DROP TABLE IF EXISTS `dc3_point_info`;
CREATE TABLE `dc3_point_info`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `point_attribute_id` bigint(20) NOT NULL COMMENT '模板配置ID',
  `value` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '值',
  `device_id` bigint(20) NOT NULL COMMENT '设备ID',
  `point_id` bigint(20) NOT NULL COMMENT '位号ID',
  `description` varchar(380) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '描述',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '修改时间',
  `deleted` tinyint(4) NULL DEFAULT 0 COMMENT '逻辑删标识',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `point_attribute_id`(`point_attribute_id`) USING BTREE,
  INDEX `device_id`(`device_id`) USING BTREE,
  INDEX `point_id`(`point_id`) USING BTREE,
  CONSTRAINT `dc3_point_info_ibfk_1` FOREIGN KEY (`point_attribute_id`) REFERENCES `dc3_point_attribute` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `dc3_point_info_ibfk_2` FOREIGN KEY (`device_id`) REFERENCES `dc3_device` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `dc3_point_info_ibfk_3` FOREIGN KEY (`point_id`) REFERENCES `dc3_point` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '位号配置信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dc3_user
-- ----------------------------
DROP TABLE IF EXISTS `dc3_user`;
CREATE TABLE `dc3_user`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '用户名，需要加密存储，均可用于登录',
  `password` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '密码，需要加密存储',
  `enable` tinyint(4) NULL DEFAULT 1 COMMENT '是否可用',
  `description` varchar(380) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '描述',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '修改时间',
  `deleted` tinyint(4) NULL DEFAULT 0 COMMENT '逻辑删标识',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dc3_rtmp
-- ----------------------------
DROP TABLE IF EXISTS `dc3_rtmp`;
CREATE TABLE `dc3_rtmp`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '名称',
  `rtsp_url` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '源视频链接，在线视频或本地文件',
  `rtmp_url` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT 'rtp播放链接,填写后缀即可',
  `command` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT 'cmd运行模板',
  `video_type` tinyint(4) NULL DEFAULT 0 COMMENT '摄像头类型',
  `run` tinyint(4) NULL DEFAULT 0 COMMENT '状态，0停止，1启动',
  `auto_start` tinyint(4) NULL DEFAULT 0 COMMENT '自启动',
  `description` varchar(380) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '描述',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '修改时间',
  `deleted` tinyint(4) NULL DEFAULT 0 COMMENT '逻辑删标识',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = 'rtmp表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dc3_label
-- ----------------------------
DROP TABLE IF EXISTS `dc3_label`;
CREATE TABLE `dc3_label`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'label名称',
  `color` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '标签颜色',
  `description` varchar(380) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '描述',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '修改时间',
  `deleted` tinyint(4) NULL DEFAULT 0 COMMENT '逻辑删标识',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '标签表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dc3_label_bind
-- ----------------------------
DROP TABLE IF EXISTS `dc3_label_bind`;
CREATE TABLE `dc3_label_bind`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `label_id` bigint(20) NOT NULL COMMENT '标签ID',
  `entity_id` bigint(20) NOT NULL COMMENT '实体ID，可为设备、设备组、模板、点位、用户',
  `type` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '关联实体类型，user\device\profile\group\point',
  `description` varchar(380) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '描述',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '修改时间',
  `deleted` tinyint(4) NULL DEFAULT 0 COMMENT '逻辑删标识',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `label_id`(`label_id`) USING BTREE,
  INDEX `entity_id`(`entity_id`) USING BTREE,
  CONSTRAINT `dc3_label_bind_ibfk_1` FOREIGN KEY (`label_id`) REFERENCES `dc3_label` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '标签关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dc3_driver
-- ----------------------------
INSERT INTO `dc3_driver` VALUES (-1, 'VirtualDriver', 'dc3-driver-virtual','127.0.0.1', 8600, 'IOT DC3 平台 Virtual驱动，仅用于测试用途。', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_driver` VALUES (-2, 'ListeningVirtualDriver', 'dc3-driver-listening-virtual','127.0.0.1', 8700, 'IOT DC3 平台 Virtual驱动，监听模式，仅用于测试用途。', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_driver` VALUES (-3, 'PlcS7Driver', 'dc3-driver-plcs7','127.0.0.1', 8601, 'IOT DC3 平台 Plc S7 Tcp 驱动。', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_driver` VALUES (-4, 'OpcDaDriver', 'dc3-driver-opc-da','127.0.0.1', 8602, 'IOT DC3 平台 Opc Da 驱动。', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_driver` VALUES (-5, 'OpcUaDriver', 'dc3-driver-opc-ua','127.0.0.1', 8602, 'IOT DC3 平台 Opc Ua 驱动。', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_driver_attribute
-- ----------------------------
INSERT INTO `dc3_driver_attribute` VALUES (-1, '主机', 'host', 'string', 'localhost', -1, 'Ip', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_driver_attribute` VALUES (-2, '端口', 'port', 'int', '18600', -1, 'Port', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

INSERT INTO `dc3_driver_attribute` VALUES (-3, '主机', 'host', 'string', '192.168.0.20', -3, 'Ip', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_driver_attribute` VALUES (-4, '端口', 'port', 'int', '102', -3, 'Port', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

INSERT INTO `dc3_driver_attribute` VALUES (-5, '主机', 'host', 'string', 'localhost', -4, 'Opc Da Host', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_driver_attribute` VALUES (-6, 'CLSID', 'clsId', 'string', 'F8582CF2-88FB-11D0-B850-00C0F0104305', -4, 'Opc Da Server CLAID', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_driver_attribute` VALUES (-7, '用户名', 'username', 'string', 'dc3', -4, 'Opc Da UserName', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_driver_attribute` VALUES (-8, '密码', 'password', 'string', 'dc3dc3', -4, 'Opc Da Passward', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

INSERT INTO `dc3_driver_attribute` VALUES (-9, '路径', 'path', 'string', '/', -5, 'Opc Ua Path', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_driver_attribute` VALUES (-10, '端口', 'port', 'int', '18600', -5, 'Opc Ua Port', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_driver_attribute` VALUES (-11, '主机', 'host', 'string', 'localhost', -5, 'Opc Ua IP', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_point_attribute
-- ----------------------------
INSERT INTO `dc3_point_attribute` VALUES (-1, '位号', 'tag', 'string', 'TAG', -1, '位号名称', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

INSERT INTO `dc3_point_attribute` VALUES (-2, '关键字', 'key', 'string', '62', -2, '报文关键字', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_attribute` VALUES (-3, '起始字节', 'start', 'int', '0', -2, '起始字节，包含该字节', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_attribute` VALUES (-4, '结束字节', 'end', 'int', '8', -2, '结束字节，不包含该字节', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_attribute` VALUES (-5, '类型', 'type', 'string', 'string', -2, '解析类型，short、int、long、float、double、boolean、string', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

INSERT INTO `dc3_point_attribute` VALUES (-6, 'DB序号', 'dbNum', 'int', '0', -3, '数据块号，从 0 开始计数', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_attribute` VALUES (-7, '类型', 'type', 'string', 'string', -3, '解析类型，bool、byte、int、dint、word、dword、real、date、time、datetime、string', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_attribute` VALUES (-8, '数据块长度', 'blockSize', 'int', '8', -3, '数据块长度', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_attribute` VALUES (-9, '字偏移', 'byteOffset', 'int', '0', -3, '字偏移', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_attribute` VALUES (-10, '位偏移', 'bitOffset', 'int', '0', -3, '位偏移', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

INSERT INTO `dc3_point_attribute` VALUES (-11, '分组', 'group', 'string', 'GROUP', -4, '位号分组名称', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_attribute` VALUES (-12, '位号', 'tag', 'string', 'TAG', -4, '位号名称', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

INSERT INTO `dc3_point_attribute` VALUES (-13, '命名空间', 'namespace', 'int', '5', -5, '命名空间索引', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_attribute` VALUES (-14, '位号', 'tag', 'string', 'TAG', -5, '位号名称', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_profile
-- ----------------------------
INSERT INTO `dc3_profile` VALUES (-1, 'VirtualDriverProfile', 0, -1, 'Virtual驱动模板', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_profile` VALUES (-2, 'ListeningVirtualDriverProfile', 0, -2, 'ListeningVirtual驱动模板', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_profile` VALUES (-3, 'PlcS7DriverProfile', 0, -3, 'PlcS7驱动模板', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_profile` VALUES (-4, 'OpcDaDriverProfile', 0, -4, 'OpcDa驱动模板', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_profile` VALUES (-5, 'OpcUaDriverProfile', 0, -5, 'OpcUa驱动模板', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_driver_info
-- ----------------------------
INSERT INTO `dc3_driver_info` VALUES (-1, -1, '127.0.0.1', -1, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_driver_info` VALUES (-2, -2, '8888', -1, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

INSERT INTO `dc3_driver_info` VALUES (-3, -3, '192.168.0.20', -3, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_driver_info` VALUES (-4, -4, '102', -3, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

INSERT INTO `dc3_driver_info` VALUES (-5, -5, '127.0.0.1', -4, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_driver_info` VALUES (-6, -6, 'F8582CF2-88FB-11D0-B850-00C0F0104305', -4, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_driver_info` VALUES (-7, -7, 'pnoke', -4, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_driver_info` VALUES (-8, -8, 'abcd4455563', -4, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

INSERT INTO `dc3_driver_info` VALUES (-9, -9, 'localhost', -5, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_driver_info` VALUES (-10, -10, '53530', -5, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_driver_info` VALUES (-11, -11, '/OPCUA/SimulationServer', -5, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_group
-- ----------------------------
INSERT INTO `dc3_group` VALUES (-1, 'VirtualDriverGroup', 'Virtual分组', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_group` VALUES (-2, 'ListeningVirtualDriverGroup', 'ListeningVirtual分组', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_group` VALUES (-3, 'PlcS7DriverGroup', 'PlcS7分组', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_group` VALUES (-4, 'OpcDaDriverGroup', 'OpcDa分组', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_group` VALUES (-5, 'OpcUaDriverGroup', 'OpcUa分组', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_device
-- ----------------------------
INSERT INTO `dc3_device` VALUES (-1, 'VirtualDevice', 'D1B60E969D3E4A26931A935E8EC62B44', 2, -1, -1, 'Virtual测试设备', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_device` VALUES (-2, 'ListeningVirtualDevice', '3E1BC62B49E964E69D31A95E8D4A2360', 2, -2, -2, 'ListeningVirtual测试设备', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_device` VALUES (-3, 'PlcS7Device', 'E1B31A953C62B49E964E69DE8D4A2360', 2, -3, -3, 'PlcS7测试设备', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_device` VALUES (-4, 'OpcDaDevice', 'E3834B8BA44D47799CE91E551485F415', 2, -4, -4, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_device` VALUES (-5, 'OpcUaDevice', '47799C85E3834B8BA44DF415E91E5514', 2, -5, -5, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_point
-- ----------------------------
INSERT INTO `dc3_point` VALUES (-1, '模拟温度', 'float', 0, 0, -999999, 999999, 1, 0, '%.3f', '℃', -1, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-2, '模拟压力', 'double', 0, 0, -999999, 999999, 1, 0, '%.3f', 'kPa', -1, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-3, '模拟时钟', 'long', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -1, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-4, '模拟信号', 'int', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -1, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-5, '模拟状态', 'boolean', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -1, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-6, '模拟标签', 'string', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -1, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

INSERT INTO `dc3_point` VALUES (-7, '模拟海拔', 'float', 0, 0, -999999, 999999, 1, 0, '%.3f', 'km', -2, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-8, '模拟速度', 'double', 0, 0, -999999, 999999, 1, 0, '%.3f', 'km/h', -2, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-9, '模拟液位', 'long', 0, 0, -999999, 999999, 1, 0, '%.3f', 'mm', -2, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-10, '模拟方向', 'int', 0, 0, -999999, 999999, 1, 0, '%.3f', '°', -2, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-11, '模拟锁定', 'boolean', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -2, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-12, '模拟经纬', 'string', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -2, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

INSERT INTO `dc3_point` VALUES (-13, '模拟设备运行状态', 'boolean', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -3, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-14, '模拟生产次数', 'long', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -3, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-15, '模拟滑块速度', 'float', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -3, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-16, '模拟运行时长', 'long', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -3, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

INSERT INTO `dc3_point` VALUES (-17, '随机int', 'int', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -4, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-18, '随机long', 'long', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -4, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-19, '随机float', 'float', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -4, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-20, '随机double', 'double', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -4, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-21, '随机boolean', 'boolean', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -4, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-22, '随机string', 'string', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -4, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

INSERT INTO `dc3_point` VALUES (-23, '随机int', 'int', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -5, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-24, '随机long', 'long', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -5, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-25, '随机float', 'float', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -5, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-26, '随机double', 'double', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -5, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-27, '随机boolean', 'boolean', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -5, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-28, '随机string', 'string', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -5, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);


-- ----------------------------
-- Records of dc3_point_info
-- ----------------------------
INSERT INTO `dc3_point_info` VALUES (-1, -1, 'temperature', -1, -1, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-2, -1, 'pressure', -1, -2, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-3, -1, 'clock', -1, -3, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-4, -1, 'signal', -1, -4, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-5, -1, 'status', -1, -5, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-6, -1, 'label', -1, -6, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

INSERT INTO `dc3_point_info` VALUES (-7, -2, '62', -2, -7, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-8, -3, '23', -2, -7, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-9, -4, '27', -2, -7, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-10, -2, '62', -2, -8, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-11, -3, '27', -2, -8, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-12, -4, '35', -2, -8, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-13, -2, '62', -2, -9, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-14, -3, '35', -2, -9, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-15, -4, '43', -2, -9, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-16, -2, '62', -2, -10, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-17, -3, '43', -2, -10, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-18, -4, '47', -2, -10, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-19, -2, '62', -2, -11, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-20, -3, '47', -2, -11, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-21, -4, '48', -2, -11, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-22, -2, '62', -2, -12, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-23, -3, '48', -2, -12, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-24, -4, '69', -2, -12, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

INSERT INTO `dc3_point_info` VALUES (-25, -6, '0', -3, -13, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-26, -7, 'bool', -3, -13, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-27, -8, '4', -3, -13, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-28, -9, '0', -3, -13, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-29, -10, '0', -3, -13, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-30, -6, '0', -3, -14, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-31, -7, 'long', -3, -14, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-32, -8, '4', -3, -14, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-33, -9, '4', -3, -14, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-34, -10, '0', -3, -14, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-35, -6, '0', -3, -15, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-36, -7, 'float', -3, -15, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-37, -8, '4', -3, -15, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-38, -9, '8', -3, -15, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-39, -10, '0', -3, -15, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-40, -6, '0', -3, -16, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-41, -7, 'long', -3, -16, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-42, -8, '4', -3, -16, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-43, -9, '12', -3, -16, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-44, -10, '0', -3, -16, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

INSERT INTO `dc3_point_info` VALUES (-45, -11, 'dc3', -4, -17, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-46, -12, 'Bucket Brigade.Int2', -4, -17, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-47, -11, 'dc3', -4, -18, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-48, -12, 'Bucket Brigade.Int4', -4, -18, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-49, -11, 'dc3', -4, -19, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-50, -12, 'Bucket Brigade.Real4', -4, -19, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-51, -11, 'dc3', -4, -20, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-52, -12, 'Bucket Brigade.Real8', -4, -20, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-53, -11, 'dc3', -4, -21, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-54, -12, 'Bucket Brigade.Boolean', -4, -21, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-55, -11, 'dc3', -4, -22, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-56, -12, 'Bucket Brigade.String', -4, -22, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

INSERT INTO `dc3_point_info` VALUES (-57, -13, '5', -5, -23, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-58, -14, 'Counter1', -5, -23, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-59, -13, '5', -5, -24, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-60, -14, 'Counter2', -5, -24, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-61, -13, '5', -5, -25, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-62, -14, 'Counter3', -5, -25, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-63, -13, '5', -5, -26, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-64, -14, 'Counter4', -5, -26, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-65, -13, '5', -5, -27, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-66, -14, 'Counter5', -5, -27, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-67, -13, '5', -5, -28, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-68, -14, 'Counter6', -5, -28, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_user
-- ----------------------------
INSERT INTO `dc3_user` VALUES (-1, 'pnoker', 'dc3dc3dc3', 1, '平台开发者账号', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_rtmp
-- ----------------------------
INSERT INTO `dc3_rtmp` VALUES (-1, '本地测试视频', 'D:/FFmpeg/bin/190314223540373995.mp4', 'rtmp://dc3-nginx:1935/rtmp/190314223540373995_local', '{exe} -re -stream_loop -1 -i {rtsp_url} -vcodec copy -acodec copy -f flv -y {rtmp_url}', 0, 0, 0,'本地MP4视频文件（复仇者联盟预告），用于测试使用', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_rtmp` VALUES (-2, '在线测试视频', 'http://vfx.mtime.cn/Video/2019/03/19/mp4/190319104618910544.mp4', 'rtmp://dc3-nginx:1935/rtmp/190314223540373995_online', '{exe} -re -stream_loop -1 -i {rtsp_url} -vcodec copy -acodec copy -f flv -y {rtmp_url}', 0, 0, 0, '在线视频流（无限动力预告），用于测试使用', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

SET FOREIGN_KEY_CHECKS = 1;