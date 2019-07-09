/*
SQLyog Ultimate v11.27 (32 bit)
MySQL - 10.4.6-MariaDB-1:10.4.6+maria~bionic : Database - iot-dc3
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
CREATE DATABASE /*!32312 IF NOT EXISTS*/`iot-dc3` /*!40100 DEFAULT CHARACTER SET utf8 */;

USE `iot-dc3`;

/*Table structure for table `iot_rtmp` */

DROP TABLE IF EXISTS `iot_rtmp`;

CREATE TABLE `iot_rtmp` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID,唯一标识',
  `rtsp_url` varchar(128) DEFAULT NULL COMMENT 'RTSP播放链接',
  `rtmp_url` varchar(128) DEFAULT NULL COMMENT 'RTMP播放链接',
  `command` varchar(128) DEFAULT NULL COMMENT 'cmd运行模板',
  `video_type` tinyint(1) DEFAULT NULL COMMENT '摄像头类型',
  `auto_start` tinyint(1) DEFAULT NULL COMMENT '自启动',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;

/*Data for the table `iot_rtmp` */

insert  into `iot_rtmp`(`id`,`rtsp_url`,`rtmp_url`,`command`,`video_type`,`auto_start`) values (1,'rtsp://184.72.239.149/vod/mp4://BigBuckBunny_175k.mov','rtmp://iotdc3.nginx:1935/rtmp/bigbuckbunny_175k','{exe} -i {rtsp_url} -vcodec copy -acodec copy -f flv -y {rtmp_url}',0,0),(2,'rtsp://admin:admin@192.168.2.236:37779/cam/realmonitor?channel=1&subtype=0','rtmp://iotdc3.nginx:1935/rtmp/monitor',NULL,1,0);

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
