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
CREATE TABLE `dc3_device`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `group_id` bigint(20) NULL DEFAULT NULL COMMENT '组ID',
  `code` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '设备 CODE',
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '设备名称',
  `type` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '设备类型',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '设备状态（离线0，在线1，维护2，故障3，废弃4）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `group_id`(`group_id`) USING BTREE,
  CONSTRAINT `dc3_device_ibfk_1` FOREIGN KEY (`group_id`) REFERENCES `dc3_group` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dc3_group
-- ----------------------------
DROP TABLE IF EXISTS `dc3_group`;
CREATE TABLE `dc3_group`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `org_id` bigint(20) NOT NULL COMMENT '组织ID',
  `code` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '编号',
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '名称',
  `location` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '位置',
  `service_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '微服务服务名称',
  `description` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '描述',
  `time` timestamp(0) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `org_id`(`org_id`) USING BTREE,
  CONSTRAINT `dc3_group_ibfk_1` FOREIGN KEY (`org_id`) REFERENCES `dc3_org` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dc3_image
-- ----------------------------
DROP TABLE IF EXISTS `dc3_image`;
CREATE TABLE `dc3_image`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `url` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '图片或者图标链接',
  `description` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '图片或者图标描述',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dc3_org
-- ----------------------------
DROP TABLE IF EXISTS `dc3_org`;
CREATE TABLE `dc3_org`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '名称',
  `image_id` bigint(20) NULL DEFAULT NULL COMMENT '图片ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `image_id`(`image_id`) USING BTREE,
  CONSTRAINT `dc3_org_ibfk_1` FOREIGN KEY (`image_id`) REFERENCES `dc3_image` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dc3_org_bind
-- ----------------------------
DROP TABLE IF EXISTS `dc3_org_bind`;
CREATE TABLE `dc3_org_bind`  (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `parent_org` bigint(20) NOT NULL COMMENT '父级组织机构',
  `child_org` bigint(20) NOT NULL COMMENT '子级别组织机构',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `parent_org`(`parent_org`) USING BTREE,
  INDEX `child_org`(`child_org`) USING BTREE,
  CONSTRAINT `dc3_org_bind_ibfk_1` FOREIGN KEY (`parent_org`) REFERENCES `dc3_org` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `dc3_org_bind_ibfk_2` FOREIGN KEY (`child_org`) REFERENCES `dc3_org` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dc3_rtmp
-- ----------------------------
DROP TABLE IF EXISTS `dc3_rtmp`;
CREATE TABLE `dc3_rtmp`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `rtsp_url` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'RTSP播放链接',
  `rtmp_url` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'RTMP播放链接',
  `command` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'cmd运行模板',
  `video_type` tinyint(1) NULL DEFAULT NULL COMMENT '摄像头类型',
  `auto_start` tinyint(1) NULL DEFAULT NULL COMMENT '自启动',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 100 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dc3_rtmp
-- ----------------------------
INSERT INTO `dc3_rtmp` VALUES (1, 'rtsp://184.72.239.149/vod/mp4://BigBuckBunny_175k.mov', 'rtmp://iotdc3.nginx:1935/rtmp/bigbuckbunny_175k', '{exe} -i {rtsp_url} -vcodec copy -acodec copy -f flv -y {rtmp_url}', 0, 0);
INSERT INTO `dc3_rtmp` VALUES (2, 'rtsp://admin:admin@192.168.2.236:37779/cam/realmonitor?channel=1&subtype=0', 'rtmp://iotdc3.nginx:1935/rtmp/monitor', NULL, 1, 0);

-- ----------------------------
-- Table structure for dc3_tag
-- ----------------------------
DROP TABLE IF EXISTS `dc3_tag`;
CREATE TABLE `dc3_tag`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `group_id` bigint(20) NULL DEFAULT NULL COMMENT 'GroupID',
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'tag名称',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dc3_tag_bind
-- ----------------------------
DROP TABLE IF EXISTS `dc3_tag_bind`;
CREATE TABLE `dc3_tag_bind`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tag_id` bigint(20) NULL DEFAULT NULL COMMENT '标签ID',
  `device_id` bigint(20) NULL DEFAULT NULL COMMENT '设备ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `tag_id`(`tag_id`) USING BTREE,
  INDEX `device_id`(`device_id`) USING BTREE,
  CONSTRAINT `dc3_tag_bind_ibfk_1` FOREIGN KEY (`tag_id`) REFERENCES `dc3_tag` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `dc3_tag_bind_ibfk_2` FOREIGN KEY (`device_id`) REFERENCES `dc3_device` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dc3_unit
-- ----------------------------
DROP TABLE IF EXISTS `dc3_unit`;
CREATE TABLE `dc3_unit`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `group_id` bigint(20) NULL DEFAULT NULL COMMENT '设备组ID',
  `code` bigint(20) NULL DEFAULT NULL COMMENT '对应到设备组中unit的ID',
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '单位中文名称',
  `symbol` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '单位符号',
  `multiple` float NULL DEFAULT NULL COMMENT '系数比例',
  `format` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '保留小数格式，采用string.format格式化',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `group_id`(`group_id`) USING BTREE,
  CONSTRAINT `dc3_unit_ibfk_1` FOREIGN KEY (`group_id`) REFERENCES `dc3_group` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dc3_variable
-- ----------------------------
DROP TABLE IF EXISTS `dc3_variable`;
CREATE TABLE `dc3_variable`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `device_id` bigint(20) NULL DEFAULT NULL COMMENT '设备ID',
  `unit_id` bigint(20) NULL DEFAULT NULL COMMENT '单位ID',
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '变量名称',
  `code` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '对应设备组中变量编号',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `device_id`(`device_id`) USING BTREE,
  INDEX `unit_id`(`unit_id`) USING BTREE,
  CONSTRAINT `dc3_variable_ibfk_3` FOREIGN KEY (`device_id`) REFERENCES `dc3_device` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `dc3_variable_ibfk_4` FOREIGN KEY (`unit_id`) REFERENCES `dc3_unit` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;