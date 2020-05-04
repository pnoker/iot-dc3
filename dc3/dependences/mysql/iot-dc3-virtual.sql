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
INSERT INTO `dc3_driver` VALUES (-1, 'VirtualDriver', 'dc3-driver-virtual','127.0.0.1', 8600, 'IOT DC3 平台 Virtual驱动，仅用于测试用途。', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_driver` VALUES (-2, 'ListeningVirtualDriver', 'dc3-driver-listening-virtual','127.0.0.1', 8700, 'IOT DC3 平台 Virtual驱动，监听模式，仅用于测试用途。', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_driver_attribute
-- ----------------------------
INSERT INTO `dc3_driver_attribute` VALUES (-1, '主机', 'host', 'string', 'localhost', -1, 'Ip', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_driver_attribute` VALUES (-2, '端口', 'port', 'int', '18600', -1, 'Port', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_point_attribute
-- ----------------------------
INSERT INTO `dc3_point_attribute` VALUES (-1, '位号', 'tag', 'string', 'TAG', -1, '位号名称', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

INSERT INTO `dc3_point_attribute` VALUES (-2, '关键字', 'key', 'string', '62', -2, '报文关键字', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_attribute` VALUES (-3, '起始字节', 'start', 'int', '0', -2, '起始字节，包含该字节', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_attribute` VALUES (-4, '结束字节', 'end', 'int', '8', -2, '结束字节，不包含该字节', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_attribute` VALUES (-5, '类型', 'type', 'string', 'string', -2, '解析类型，short、int、long、float、double、boolean、string', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_profile
-- ----------------------------
INSERT INTO `dc3_profile` VALUES (-1, 'VirtualDriverProfile', 0, -1, 'Virtual驱动模板', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_profile` VALUES (-2, 'ListeningVirtualDriverProfile', 0, -2, 'ListeningVirtual驱动模板', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_driver_info
-- ----------------------------
INSERT INTO `dc3_driver_info` VALUES (-1, -1, '127.0.0.1', -1, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_driver_info` VALUES (-2, -2, '8888', -1, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_group
-- ----------------------------
INSERT INTO `dc3_group` VALUES (-1, 'VirtualDriverGroup', 'Virtual分组', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_group` VALUES (-2, 'ListeningVirtualDriverGroup', 'ListeningVirtual分组', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_device
-- ----------------------------
INSERT INTO `dc3_device` VALUES (-1, 'VirtualDevice', 'd1b60e969d3e4a26931a935e8ec62b44', 2, -1, -1, 'Virtual测试设备', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_device` VALUES (-2, 'ListeningVirtualDevice', '3e1bc62b49e964e69d31a95e8d4a2360', 2, -2, -2, 'ListeningVirtual测试设备', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_point
-- ----------------------------
INSERT INTO `dc3_point` VALUES (-1, '温度', 'float', 0, 0, -999999, 999999, 1, 0, '%.3f', '℃', -1, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-2, '压力', 'double', 0, 0, -999999, 999999, 1, 0, '%.3f', 'kPa', -1, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-3, '时钟', 'long', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -1, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-4, '信号', 'int', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -1, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-5, '状态', 'boolean', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -1, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-6, '标签', 'string', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -1, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

INSERT INTO `dc3_point` VALUES (-7, '海拔', 'float', 0, 0, -999999, 999999, 1, 0, '%.3f', 'km', -2, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-8, '速度', 'double', 0, 0, -999999, 999999, 1, 0, '%.3f', 'km/h', -2, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-9, '液位', 'long', 0, 0, -999999, 999999, 1, 0, '%.3f', 'mm', -2, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-10, '方向', 'int', 0, 0, -999999, 999999, 1, 0, '%.3f', '°', -2, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-11, '锁定', 'boolean', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -2, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-12, '经纬', 'string', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -2, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

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

SET FOREIGN_KEY_CHECKS = 1;