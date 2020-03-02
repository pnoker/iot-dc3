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
INSERT INTO `dc3_driver` VALUES (-1, 'VirtualDriver', 'dc3-driver-virtual','127.0.0.1', 8600, 'IOT DC3 平台 虚拟驱动，仅用于测试用途。', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_driver_attribute
-- ----------------------------
INSERT INTO `dc3_driver_attribute` VALUES (-1, '主机', 'host', 'string', 'localhost', -1, '目标机器 Ip', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_driver_attribute` VALUES (-2, '端口', 'port', 'string', '18600', -1, '目标机器 Port', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_point_attribute
-- ----------------------------
INSERT INTO `dc3_point_attribute` VALUES (-1, '位号', 'tag', 'string', 'TAG', -1, '位号名称', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_attribute` VALUES (-2, '分组', 'group', 'string', 'GROUP', -1, '分组名称', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_profile
-- ----------------------------
INSERT INTO `dc3_profile` VALUES (-1, 'VProfile', 0, -1, '虚拟驱动模板', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_driver_info
-- ----------------------------
INSERT INTO `dc3_driver_info` VALUES (-1, -1, '127.0.0.1', -1, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_driver_info` VALUES (-2, -2, '8888', -1, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_group
-- ----------------------------
INSERT INTO `dc3_group` VALUES (-1, 'VGroup-Dev', '虚拟分组-开发', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_group` VALUES (-2, 'VGroup-Product', '虚拟分组-生产', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_device
-- ----------------------------
INSERT INTO `dc3_device` VALUES (-1, 'VDevice-D1', 'd1b60e969d3e4a26931a935e8ec62b44', 2, -1, -1, '虚拟测试设备', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_device` VALUES (-2, 'VDevice-P1', '3e1bc62b49e964e69d31a95e8d4a2360', 2, -1, -2, '虚拟测试设备', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_point
-- ----------------------------
INSERT INTO `dc3_point` VALUES (-1, '温度', 'float', 0, 0, -999999, 999999, 1, 0, '%.3f', '℃', -1, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-2, '压力', 'double', 0, 0, -999999, 999999, 1, 0, '%.3f', 'kPa', -1, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-3, '时钟', 'long', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -1, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-4, '信号', 'int', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -1, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-5, '状态', 'boolean', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -1, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point` VALUES (-6, '标签', 'string', 0, 0, -999999, 999999, 1, 0, '%.3f', '', -1, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_point_info
-- ----------------------------
INSERT INTO `dc3_point_info` VALUES (-1, -1, 'temperature', -1, -1, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-2, -1, 'pressure', -1, -2, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-3, -1, 'clock', -1, -3, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-4, -1, 'signal', -1, -4, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-5, -1, 'status', -1, -5, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-6, -1, 'label', -1, -6, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-7, -2, 'group-temperature', -1, -1, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-8, -2, 'group-pressure', -1, -2, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-9, -2, 'group-clock', -1, -3, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-10, -2, 'group-signal', -1, -4, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-11, -2, 'group-status', -1, -5, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-12, -2, 'group-label', -1, -6, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-13, -1, 'temperature', -2, -1, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-14, -1, 'pressure', -2, -2, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-15, -1, 'clock', -2, -3, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-16, -1, 'signal', -2, -4, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-17, -1, 'status', -2, -5, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-18, -1, 'label', -2, -6, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-19, -2, 'group-temperature', -2, -1, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-20, -2, 'group-pressure', -2, -2, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-21, -2, 'group-clock', -2, -3, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-22, -2, 'group-signal', -2, -4, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-23, -2, 'group-status', -2, -5, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);
INSERT INTO `dc3_point_info` VALUES (-24, -2, 'group-label', -2, -6, '', '2019-10-01 00:00:00', '2019-10-01 00:00:00', 0);

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