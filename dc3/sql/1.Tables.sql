/*Table structure for table `iot_rtmp` */

DROP TABLE IF EXISTS `iot_rtmp`;

CREATE TABLE `iot_rtmp` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID,唯一标识',
  `rtsp_url` varchar(128) DEFAULT NULL COMMENT 'RTSP播放链接',
  `rtmp_url` varchar(128) DEFAULT NULL COMMENT 'RTMP播放链接',
  `command` varchar(128) DEFAULT NULL COMMENT 'cmd运行模板',
  `video_type` tinyint(1) DEFAULT NULL COMMENT '摄像头类型',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;