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
INSERT INTO `dc3_driver_attribute` VALUES (-2, '端口', 'port', 'string', '18600', -1, 'Port', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_point_attribute
-- ----------------------------
INSERT INTO `dc3_point_attribute` VALUES (-1, '位号', 'tag', 'string', 'TAG', -1, '位号Tag', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_attribute` VALUES (-2, '分组', 'group', 'string', 'GROUP', -1, '位号Group', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

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
INSERT INTO `dc3_point_info` VALUES (-7, -2, 'group', -1, -1, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-8, -2, 'group', -1, -2, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-9, -2, 'group', -1, -3, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-10, -2, 'group', -1, -4, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-11, -2, 'group', -1, -5, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-12, -2, 'group', -1, -6, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

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