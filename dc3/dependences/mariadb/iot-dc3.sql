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

-- ----------------------------
-- Table structure for dc3_rtmp
-- ----------------------------
DROP TABLE IF EXISTS `dc3_rtmp`;
CREATE TABLE `dc3_rtmp`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID,唯一标识',
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

SET FOREIGN_KEY_CHECKS = 1;
