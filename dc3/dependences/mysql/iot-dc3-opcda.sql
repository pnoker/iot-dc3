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
INSERT INTO `dc3_driver` VALUES (-4, 'OpcDaDriver', 'dc3-driver-opc-da','127.0.0.1', 8602, 'IOT DC3 平台 Opc Da 驱动。', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_driver_attribute
-- ----------------------------
INSERT INTO `dc3_driver_attribute` VALUES (-5, '主机', 'host', 'string', 'localhost', -4, 'Opc Da Host', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_driver_attribute` VALUES (-6, 'CLSID', 'clsId', 'string', 'F8582CF2-88FB-11D0-B850-00C0F0104305', -4, 'Opc Da Server CLAID', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_driver_attribute` VALUES (-7, '用户名', 'username', 'string', 'dc3', -4, 'Opc Da UserName', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_driver_attribute` VALUES (-8, '密码', 'password', 'string', 'dc3dc3', -4, 'Opc Da Passward', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_point_attribute
-- ----------------------------
INSERT INTO `dc3_point_attribute` VALUES (-11, '分组', 'group', 'string', 'GROUP', -4, '位号分组名称', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_attribute` VALUES (-12, '位号', 'tag', 'string', 'TAG', -4, '位号名称', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_profile
-- ----------------------------
INSERT INTO `dc3_profile` VALUES (-4, 'OpcDaDriverProfile', 0, -4, 'OpcDa驱动模板', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_driver_info
-- ----------------------------
INSERT INTO `dc3_driver_info` VALUES (-5, -5, '127.0.0.1', -4, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_driver_info` VALUES (-6, -6, 'F8582CF2-88FB-11D0-B850-00C0F0104305', -4, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_driver_info` VALUES (-7, -7, 'pnoke', -4, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_driver_info` VALUES (-8, -8, 'abcd4455563', -4, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_group
-- ----------------------------
INSERT INTO `dc3_group` VALUES (-4, 'OpcDaDriverGroup', 'OpcDa分组', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_device
-- ----------------------------
INSERT INTO `dc3_device` VALUES (-4, 'OpcDaDevice', 'E3834B8BA44D47799CE91E551485F415', 2, -4, -4, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_point
-- ----------------------------
INSERT INTO `dc3_point` VALUES (-17, '随机byte', 'byte', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -4, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-18, '随机int', 'int', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -4, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-19, '随机long', 'long', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -4, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-20, '随机float', 'float', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -4, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-21, '随机double', 'double', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -4, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-22, '随机boolean', 'boolean', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -4, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-23, '随机string', 'string', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -4, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_point_info
-- ----------------------------
INSERT INTO `dc3_point_info` VALUES (-45, -11, 'dc3', -4, -17, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-46, -12, 'Bucket Brigade.Int1', -4, -17, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-47, -11, 'dc3', -4, -18, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-48, -12, 'Bucket Brigade.Int2', -4, -18, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-49, -11, 'dc3', -4, -19, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-50, -12, 'Bucket Brigade.Int4', -4, -19, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-51, -11, 'dc3', -4, -20, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-52, -12, 'Bucket Brigade.Real4', -4, -20, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-53, -11, 'dc3', -4, -21, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-54, -12, 'Bucket Brigade.Real8', -4, -21, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-55, -11, 'dc3', -4, -22, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-56, -12, 'Bucket Brigade.Boolean', -4, -22, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-57, -11, 'dc3', -4, -23, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-58, -12, 'Bucket Brigade.String', -4, -23, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

SET FOREIGN_KEY_CHECKS = 1;