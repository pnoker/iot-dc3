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

CREATE DATABASE `iot-dc3`;

USE `iot-dc3`;

-- ----------------------------
-- Table structure for dc3_device
-- ----------------------------
DROP TABLE IF EXISTS `dc3_device`;
CREATE TABLE `dc3_device` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) DEFAULT NULL COMMENT '设备名称',
  `device_code` varchar(255) DEFAULT NULL COMMENT '设备 CODE，对应到设备组中的设备CODE',
  `type` varchar(255) DEFAULT NULL COMMENT '设备类型',
  `status` tinyint(4) DEFAULT NULL COMMENT '设备状态（离线0，在线1，维护2，故障3，废弃4）',
  `node_id` bigint(20) DEFAULT -1 COMMENT '节点ID，节点类型为device',
  `image_id` bigint(20) DEFAULT -1 COMMENT '图片ID',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户ID',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint(4) DEFAULT 0 COMMENT '逻辑删标识',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `image_id` (`image_id`) USING BTREE,
  KEY `user_id` (`user_id`) USING BTREE,
  KEY `node_id` (`node_id`) USING BTREE,
  CONSTRAINT `dc3_device_ibfk_2` FOREIGN KEY (`node_id`) REFERENCES `dc3_node` (`id`),
  CONSTRAINT `dc3_device_ibfk_3` FOREIGN KEY (`image_id`) REFERENCES `dc3_image` (`id`),
  CONSTRAINT `dc3_device_ibfk_4` FOREIGN KEY (`user_id`) REFERENCES `dc3_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='设备表';

-- ----------------------------
-- Table structure for dc3_device_driver
-- ----------------------------
DROP TABLE IF EXISTS `dc3_device_driver`;
CREATE TABLE `dc3_device_driver` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) DEFAULT NULL COMMENT ' 协议名称',
  `service_name` varchar(255) DEFAULT NULL COMMENT '协议服务名称',
  `connect_info` text DEFAULT NULL COMMENT '设备驱动连接属性',
  `profile_info` text DEFAULT NULL COMMENT '设备测点配置属性',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户ID',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint(4) DEFAULT 0 COMMENT '逻辑删标识',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `user_id` (`user_id`) USING BTREE,
  CONSTRAINT `dc3_device_driver_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `dc3_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for dc3_image
-- ----------------------------
DROP TABLE IF EXISTS `dc3_image`;
CREATE TABLE `dc3_image` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) DEFAULT NULL COMMENT '图片名称',
  `url` varchar(255) DEFAULT NULL COMMENT '图片或者图标链接',
  `node_id` bigint(20) DEFAULT -1 COMMENT '节点ID，节点类型为image',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户ID',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint(4) DEFAULT 0 COMMENT '逻辑删标识',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `user_id` (`user_id`) USING BTREE,
  KEY `node_id` (`node_id`) USING BTREE,
  CONSTRAINT `dc3_image_ibfk_1` FOREIGN KEY (`node_id`) REFERENCES `dc3_node` (`id`),
  CONSTRAINT `dc3_image_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `dc3_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='图片表';

-- ----------------------------
-- Records of dc3_image
-- ----------------------------
BEGIN;
INSERT INTO `dc3_image` VALUES (-1, 'iotdc3-logo', '/images/logo/dc3-logo.png', -1, -1, 'iotdc3平台logo，默认创建', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
COMMIT;

-- ----------------------------
-- Table structure for dc3_label
-- ----------------------------
DROP TABLE IF EXISTS `dc3_label`;
CREATE TABLE `dc3_label` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) DEFAULT NULL COMMENT 'label名称',
  `color` varchar(255) DEFAULT NULL COMMENT '标签颜色',
  `node_id` bigint(20) DEFAULT -1 COMMENT '节点ID，节点类型为label',
  `image_id` bigint(20) DEFAULT -1 COMMENT '图片ID',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户ID',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint(4) DEFAULT 0 COMMENT '逻辑删标识',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `image_id` (`image_id`) USING BTREE,
  KEY `user_id` (`user_id`) USING BTREE,
  KEY `node_id` (`node_id`) USING BTREE,
  CONSTRAINT `dc3_label_ibfk_1` FOREIGN KEY (`node_id`) REFERENCES `dc3_node` (`id`),
  CONSTRAINT `dc3_label_ibfk_2` FOREIGN KEY (`image_id`) REFERENCES `dc3_image` (`id`),
  CONSTRAINT `dc3_label_ibfk_3` FOREIGN KEY (`user_id`) REFERENCES `dc3_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='标签表';

-- ----------------------------
-- Table structure for dc3_label_bind
-- ----------------------------
DROP TABLE IF EXISTS `dc3_label_bind`;
CREATE TABLE `dc3_label_bind` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `node_id` bigint(20) DEFAULT -1 COMMENT '节点ID，节点类型为label',
  `label_id` bigint(20) DEFAULT NULL COMMENT '标签ID',
  `entity_id` bigint(20) DEFAULT NULL COMMENT '实体ID，可为设备、设备组等',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户ID',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint(4) DEFAULT 0 COMMENT '逻辑删标识',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `label_id` (`label_id`) USING BTREE,
  KEY `user_id` (`user_id`) USING BTREE,
  KEY `entity_id` (`entity_id`) USING BTREE,
  KEY `node_id` (`node_id`) USING BTREE,
  CONSTRAINT `dc3_label_bind_ibfk_1` FOREIGN KEY (`node_id`) REFERENCES `dc3_node` (`id`),
  CONSTRAINT `dc3_label_bind_ibfk_2` FOREIGN KEY (`label_id`) REFERENCES `dc3_label` (`id`),
  CONSTRAINT `dc3_label_bind_ibfk_3` FOREIGN KEY (`user_id`) REFERENCES `dc3_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='标签关联表';

-- ----------------------------
-- Table structure for dc3_node
-- ----------------------------
DROP TABLE IF EXISTS `dc3_node`;
CREATE TABLE `dc3_node` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) DEFAULT NULL COMMENT '名称',
  `type` varchar(255) DEFAULT NULL COMMENT '节点类型，可以是设备、设备组等的节点类型',
  `parent_id` bigint(20) DEFAULT -1 COMMENT '父级ID,默认为根节点，ID=-1',
  `image_id` bigint(20) DEFAULT -1 COMMENT '图片ID',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户ID',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint(4) DEFAULT 0 COMMENT '逻辑删标识',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `image_id` (`image_id`) USING BTREE,
  KEY `user_id` (`user_id`) USING BTREE,
  CONSTRAINT `dc3_node_ibfk_1` FOREIGN KEY (`image_id`) REFERENCES `dc3_image` (`id`),
  CONSTRAINT `dc3_node_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `dc3_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='节点表，有多种类型的节点，用户，设备组，设备，设备测点等，分别对应：user,group,device,point';

-- ----------------------------
-- Records of dc3_node
-- ----------------------------
BEGIN;
INSERT INTO `dc3_node` VALUES (-2, '计量类', 'UNIT', -1, -1, -1, '计量单位', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_node` VALUES (-1, '根节点', 'NODE', -1, -1, -1, '相对根节点', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
COMMIT;

-- ----------------------------
-- Table structure for dc3_point
-- ----------------------------
DROP TABLE IF EXISTS `dc3_point`;
CREATE TABLE `dc3_point` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) DEFAULT NULL COMMENT '变量名称',
  `point_code` varchar(255) DEFAULT NULL COMMENT '对应设备组中变量编号',
  `device_id` bigint(20) DEFAULT NULL COMMENT '设备ID',
  `node_id` bigint(20) DEFAULT -1 COMMENT '节点ID，节点类型为point',
  `property_id` bigint(20) DEFAULT NULL COMMENT '参数属性ID',
  `profile_id` bigint(20) DEFAULT NULL COMMENT '测点配置信息ID',
  `unit_id` bigint(20) DEFAULT NULL COMMENT '单位ID',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户ID',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint(4) DEFAULT 0 COMMENT '逻辑删标识',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `device_id` (`device_id`) USING BTREE,
  KEY `unit_id` (`unit_id`) USING BTREE,
  KEY `user_id` (`user_id`) USING BTREE,
  KEY `property_id` (`property_id`) USING BTREE,
  KEY `node_id` (`node_id`) USING BTREE,
  KEY `profile_id` (`profile_id`) USING BTREE,
  CONSTRAINT `dc3_point_ibfk_1` FOREIGN KEY (`device_id`) REFERENCES `dc3_device` (`id`),
  CONSTRAINT `dc3_point_ibfk_2` FOREIGN KEY (`node_id`) REFERENCES `dc3_node` (`id`),
  CONSTRAINT `dc3_point_ibfk_3` FOREIGN KEY (`unit_id`) REFERENCES `dc3_unit` (`id`),
  CONSTRAINT `dc3_point_ibfk_4` FOREIGN KEY (`user_id`) REFERENCES `dc3_user` (`id`),
  CONSTRAINT `dc3_point_ibfk_5` FOREIGN KEY (`property_id`) REFERENCES `dc3_point_property` (`id`),
  CONSTRAINT `dc3_point_ibfk_6` FOREIGN KEY (`profile_id`) REFERENCES `dc3_point_profile` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='设备测点表';

-- ----------------------------
-- Table structure for dc3_point_profile
-- ----------------------------
DROP TABLE IF EXISTS `dc3_point_profile`;
CREATE TABLE `dc3_point_profile` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config` text DEFAULT NULL COMMENT '测点配置信息，Json字符串',
  `driver_id` bigint(20) DEFAULT NULL COMMENT '设备驱动ID',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户ID',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint(4) DEFAULT 0 COMMENT '逻辑删标识',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `user_id` (`user_id`) USING BTREE,
  KEY `driver_id` (`driver_id`) USING BTREE,
  CONSTRAINT `dc3_point_profile_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `dc3_user` (`id`),
  CONSTRAINT `dc3_point_profile_ibfk_2` FOREIGN KEY (`driver_id`) REFERENCES `dc3_device_driver` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='设备测点配置表';

-- ----------------------------
-- Table structure for dc3_point_property
-- ----------------------------
DROP TABLE IF EXISTS `dc3_point_property`;
CREATE TABLE `dc3_point_property` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) DEFAULT NULL COMMENT '名称概述',
  `base` float DEFAULT NULL COMMENT '基础值',
  `minimum` float DEFAULT NULL COMMENT '最小值',
  `maximum` float DEFAULT NULL COMMENT '最大值',
  `multiple` float(255,0) DEFAULT 1 COMMENT '倍数',
  `value` float DEFAULT 0 COMMENT '默认值',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户ID',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint(4) DEFAULT 0 COMMENT '逻辑删标识',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `user_id` (`user_id`) USING BTREE,
  CONSTRAINT `dc3_point_property_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `dc3_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='测点属性表';

-- ----------------------------
-- Table structure for dc3_rtmp
-- ----------------------------
DROP TABLE IF EXISTS `dc3_rtmp`;
CREATE TABLE `dc3_rtmp` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) DEFAULT NULL COMMENT '名称',
  `rtsp_url` varchar(128) DEFAULT NULL COMMENT '源视频链接，在线视频或本地文件',
  `rtmp_url` varchar(128) DEFAULT NULL COMMENT 'rtp播放链接,填写后缀即可',
  `command` varchar(128) DEFAULT NULL COMMENT 'cmd运行模板',
  `video_type` tinyint(4) DEFAULT NULL COMMENT '摄像头类型',
  `run` tinyint(4) NULL DEFAULT 0 COMMENT '状态，0停止，1启动',
  `auto_start` tinyint(4) NULL DEFAULT NULL COMMENT '自启动',
  `node_id` bigint(20) DEFAULT -1 COMMENT '节点ID，节点类型为rtmp',
  `image_id` bigint(20) DEFAULT -1 COMMENT '图片ID',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户ID',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint(4) DEFAULT 0 COMMENT '逻辑删标识',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `image_id` (`image_id`) USING BTREE,
  KEY `node_id` (`node_id`) USING BTREE,
  KEY `dc3_rtmp_ibfk_3` (`user_id`) USING BTREE,
  CONSTRAINT `dc3_rtmp_ibfk_1` FOREIGN KEY (`node_id`) REFERENCES `dc3_node` (`id`),
  CONSTRAINT `dc3_rtmp_ibfk_2` FOREIGN KEY (`image_id`) REFERENCES `dc3_image` (`id`),
  CONSTRAINT `dc3_rtmp_ibfk_3` FOREIGN KEY (`user_id`) REFERENCES `dc3_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='rtmp表';

-- ----------------------------
-- Records of dc3_rtmp
-- ----------------------------
BEGIN;
INSERT INTO `dc3_rtmp` VALUES (-2, '在线测试视频', 'http://vfx.mtime.cn/Video/2019/03/19/mp4/190319104618910544.mp4', 'rtmp://iotdc3.nginx:1935/rtmp/190314223540373995_online', '{exe} -re -stream_loop -1 -i {rtsp_url} -vcodec copy -acodec copy -f flv -y {rtmp_url}', 0, 0, 0, -1, -1, -1, '在线视频流（无限动力预告），用于测试使用', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_rtmp` VALUES (-1, '本地测试视频', 'D:/FFmpeg/bin/190314223540373995.mp4', 'rtmp://iotdc3.nginx:1935/rtmp/190314223540373995_local', '{exe} -re -stream_loop -1 -i {rtsp_url} -vcodec copy -acodec copy -f flv -y {rtmp_url}', 0, 0, 0, -1, -1, -1, '本地MP4视频文件（复仇者联盟预告），用于测试使用', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
COMMIT;

-- ----------------------------
-- Table structure for dc3_schedule
-- ----------------------------
DROP TABLE IF EXISTS `dc3_schedule`;
CREATE TABLE `dc3_schedule` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) DEFAULT NULL COMMENT '任务名称',
  `corn_expression` varchar(255) DEFAULT NULL COMMENT '定时任务规则',
  `status` tinyint(4) DEFAULT NULL COMMENT '当前状态',
  `url` varchar(255) DEFAULT NULL COMMENT '链接',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户ID',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint(4) DEFAULT 0 COMMENT '逻辑删标识',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `user_id` (`user_id`) USING BTREE,
  CONSTRAINT `dc3_schedule_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `dc3_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='任务调度表';

-- ----------------------------
-- Table structure for dc3_token
-- ----------------------------
DROP TABLE IF EXISTS `dc3_token`;
CREATE TABLE `dc3_token`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `token` varchar(32) DEFAULT NULL COMMENT 'Token,用于接口验证',
  `app_id` varchar(32) DEFAULT NULL COMMENT '应用ID,由平台生成',
  `private_key` varchar(255) DEFAULT NULL COMMENT '密钥,由平台生成',
  `expire_time` datetime(0) NULL DEFAULT NULL COMMENT '过期时间',
  `type` tinyint(4) DEFAULT 0 COMMENT '密钥类型',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint(4) DEFAULT 0 COMMENT '逻辑删标识',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='Token表';

-- ----------------------------
-- Records of dc3_token
-- ----------------------------
INSERT INTO `dc3_token` VALUES (-1, 'RSHPmSyOwQZqjMbn', 'Tp6vIA0hjv5dxrqi', 'Q5IkfORRoP5EQa8ED4EeR73WKXont2X6', '2099-10-01 00:00:00', 0, '测试专用Token', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Table structure for dc3_unit
-- ----------------------------
DROP TABLE IF EXISTS `dc3_unit`;
CREATE TABLE `dc3_unit` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) DEFAULT NULL COMMENT '单位概要，摄氏度|℃|%f',
  `unit` varchar(255) DEFAULT NULL COMMENT '单位中文名称',
  `symbol` varchar(255) DEFAULT NULL COMMENT '单位符号',
  `format` varchar(255) DEFAULT '%.3f' COMMENT '保留小数格式，采用string.format格式化',
  `node_id` bigint(20) DEFAULT -1 COMMENT '节点ID',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户ID',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint(4) DEFAULT 0 COMMENT '逻辑删标识',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `user_id` (`user_id`) USING BTREE,
  KEY `node_id` (`node_id`) USING BTREE,
  CONSTRAINT `dc3_unit_ibfk_1` FOREIGN KEY (`node_id`) REFERENCES `dc3_node` (`id`),
  CONSTRAINT `dc3_unit_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `dc3_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='单位表';

-- ----------------------------
-- Records of dc3_unit
-- ----------------------------
BEGIN;
INSERT INTO `dc3_unit` VALUES (-10, '度|°|%.3f', '度', '°', '%.3f', -2, -1, NULL, '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_unit` VALUES (-9, '升|L|%.3f', '升', 'L', '%.3f', -2, -1, NULL, '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_unit` VALUES (-8, '立方米|m³|%.3f', '立方米', 'm³', '%.3f', -2, -1, NULL, '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_unit` VALUES (-7, '平方米|m²|%.3f', '平方米', 'm²', '%.3f', -2, -1, NULL, '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_unit` VALUES (-6, '千克|kg|%.3f', '千克', 'kg', '%.3f', -2, -1, NULL, '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_unit` VALUES (-5, '克|g|%.3f', '克', 'g', '%.3f', -2, -1, NULL, '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_unit` VALUES (-4, '千米|km|%.3f', '千米', 'km', '%.3f', -2, -1, NULL, '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_unit` VALUES (-3, '米|m|%.3f', '米', 'm', '%.3f', -2, -1, NULL, '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_unit` VALUES (-2, '厘米|cm|%.3f', '厘米', 'cm', '%.3f', -2, -1, NULL, '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_unit` VALUES (-1, '毫米|mm|%.3f', '毫米', 'mm', '%.3f', -2, -1, NULL, '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
COMMIT;

-- ----------------------------
-- Table structure for dc3_user
-- ----------------------------
DROP TABLE IF EXISTS `dc3_user`;
CREATE TABLE `dc3_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(255) DEFAULT NULL COMMENT '用户名，需要加密存储，均可用于登录',
  `phone` varchar(255) DEFAULT NULL COMMENT '手机号，需要加密存储，均可用于登录',
  `email` varchar(255) DEFAULT NULL COMMENT '邮箱，需要加密存储，均可用于登录',
  `password` varchar(255) DEFAULT NULL COMMENT '密码，需要加密存储',
  `token_id` bigint(20) DEFAULT -1 COMMENT 'Token ID',
  `node_id` bigint(20) DEFAULT -1 COMMENT '节点ID，节点类型为用户',
  `image_id` bigint(20) DEFAULT -1 COMMENT '图片ID',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint(4) DEFAULT 0 COMMENT '逻辑删标识',
  `enable` tinyint(4) DEFAULT NULL COMMENT '是否可用',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `image_id` (`image_id`) USING BTREE,
  KEY `node_id` (`node_id`) USING BTREE,
  KEY `token_id` (`token_id`) USING BTREE,
  CONSTRAINT `dc3_user_ibfk_1` FOREIGN KEY (`node_id`) REFERENCES `dc3_node` (`id`),
  CONSTRAINT `dc3_user_ibfk_2` FOREIGN KEY (`image_id`) REFERENCES `dc3_image` (`id`),
  CONSTRAINT `dc3_user_ibfk_3` FOREIGN KEY (`token_id`) REFERENCES `dc3_token` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='用户表';

-- ----------------------------
-- Records of dc3_user
-- ----------------------------
BEGIN;
INSERT INTO `dc3_user` VALUES (-1, 'pnoker', '18304071393', 'pnokers@icloud.com', 'iotdc3', -1,-1, -1, '平台开发者账号', '2019-10-01 00:00:00', '2019-10-27 11:51:06', 0, 1);
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;