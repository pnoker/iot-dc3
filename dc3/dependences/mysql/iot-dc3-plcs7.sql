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

USE `dc3`;

-- ----------------------------
-- Records of dc3_driver
-- ----------------------------
INSERT INTO `dc3_driver` VALUES (-3, 'PlcS7Driver', 'dc3-driver-plcs7','127.0.0.1', 8601, 'IOT DC3 平台 Plc S7 Tcp 驱动。', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_driver_attribute
-- ----------------------------
INSERT INTO `dc3_driver_attribute` VALUES (-3, '主机', 'host', 'string', '192.168.0.20', -3, 'Ip', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_driver_attribute` VALUES (-4, '端口', 'port', 'int', '102', -3, 'Port', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_point_attribute
-- ----------------------------
INSERT INTO `dc3_point_attribute` VALUES (-6, 'DB序号', 'dbNum', 'int', '0', -3, '数据块号，从 0 开始计数', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_attribute` VALUES (-7, '类型', 'type', 'string', 'string', -3, '解析类型，bool、byte、int、dint、word、dword、real、date、time、datetime、string', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_attribute` VALUES (-8, '数据块长度', 'blockSize', 'int', '8', -3, '数据块长度', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_attribute` VALUES (-9, '字偏移', 'byteOffset', 'int', '0', -3, '字偏移', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_attribute` VALUES (-10, '位偏移', 'bitOffset', 'int', '0', -3, '位偏移', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_profile
-- ----------------------------
INSERT INTO `dc3_profile` VALUES (-3, 'PlcS7DriverProfile', 0, -3, 'PlcS7驱动模板', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_driver_info
-- ----------------------------
INSERT INTO `dc3_driver_info` VALUES (-3, -3, '192.168.0.20', -3, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_driver_info` VALUES (-4, -4, '102', -3, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_group
-- ----------------------------
INSERT INTO `dc3_group` VALUES (-3, 'PlcS7DriverGroup', 'PlcS7分组', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_device
-- ----------------------------
INSERT INTO `dc3_device` VALUES (-3, 'PlcS7Device', 'e1b31a953c62b49e964e69de8d4a2360', 2, -3, -3, 'PlcS7测试设备', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_point
-- ----------------------------
INSERT INTO `dc3_point` VALUES (-13, '设备运行状态', 'boolean', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -3, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-14, '生产次数', 'long', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -3, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-15, '滑块速度', 'float', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -3, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-16, '运行时长', 'long', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -3, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_point_info
-- ----------------------------
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

SET FOREIGN_KEY_CHECKS = 1;