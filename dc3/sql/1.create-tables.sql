/*Table structure for table `iot_rtmp` */

DROP TABLE IF EXISTS `iot_rtmp`;

CREATE TABLE `iot_rtmp` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID,唯一标识',
  `rtsp_url` varchar(128) DEFAULT NULL COMMENT 'RTSP播放链接',
  `rtmp_url` varchar(128) DEFAULT NULL COMMENT 'RTMP播放链接',
  `video_type` bit(1) DEFAULT NULL COMMENT '摄像头类型',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;