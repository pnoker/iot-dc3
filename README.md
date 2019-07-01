<p align="center">
	<img src="./dc3/images/iot-dc3-logo.png" width="400""><br>
	<a><img src="https://img.shields.io/badge/JDK-1.8-yellow.svg"></a>
    <a><img src="https://img.shields.io/github/issues/pnoker/iot-dc3.svg"></a>
	<a target="_blank" href="https://github.com/pnoker/iot-dc3/blob/master/LICENSE">
		<img src="https://img.shields.io/github/license/pnoker/iot-dc3.svg" >
	</a><br>
	<strong>DC3是一个基于Spring Cloud的开源的、分布式的物联网(IOT)平台,用于快速开发物联网项目和管理物联设备,是一整套物联系统解决方案。<br>IOT DC3 is an open source, distributed Internet of Things (IOT) platform based on Spring Cloud. It is used for rapid development of IOT projects and management of IOT devices. It is a set of solutions for IOT system.</strong>
</p> 
<p align="center">
    <span>GitHub:</span><a target="_blank" href="https://github.com/pnoker/iot-dc3">https://github.com/pnoker/iot-dc3</a><br>
    <span>码云:</span><a target="_blank" href="https://gitee.com/pnoker/iot-dc3">https://gitee.com/pnoker/iot-dc3</a><br>
</p>

---

 ### 什么是DC3 IOT平台？

 ![iot-dc3-architecture](dc3/images/iot-dc3-architecture1.jpg)

 #### 模块

 * 设备微服务层:用于提供标准或者私有协议连接物理设备的SDK;
 * 核心微服务层:用于提供微服务注册中心、配置管理中心、设备指令接口、设备注册与关联配对、数据管理中心,是所有微服务交互的核心部分;
 * 支持微服务层:用于提供规则引擎、任务调度、报警与消息通知、日志管理、数据清理;
 * 开放微服务层:用于提供数据开放等服务...

  #### DC3致力于

 * 跨平台分布式、多设备、多协议（目前支持Mqtt\Rtsp\Rtmp\Http\Plc-S7\Opc\Opc-Ua\自定义Tcp\自定义Socket\数据报文解析）;
 * 物联设备数据采集、边缘处理、数据持久化、数据缓存、全文搜索、功能性微服务调用;
 * 物联设备端代码一键生成,快速安全的接入设备和物联设备管理（目前支持Java）;
 * 安全权限控制、数据支持对称和非对称加密（目前支持AES\RSA）;
 * 物联设备远程指令透传、监控和控制;
 * 对外开放数据接口及微服务接口;
 * 支持网关端和云端部署;
 * 低功耗运行。

 #### DC3设计为

* 可伸缩:水平可伸缩的平台,构建使用领先的Spring Cloud开源技术;
* 容错:没有单点故障弱,集群中的每个节点是相同的;
* 健壮和高效:单一服务器节点可以处理甚至数百成千上万的设备根据用例;
* 可定制:添加新的设备协议,并注册到服务中心;
* 跨平台:使用Java环境可异地、分布式多平台部署;
* 完善性:设备快速接入、注册、权限校验;
* 安全:数据加密传输。

### DC3 IOT平台架构？

DC3平台是基于Spring Cloud架构开发的,是一系列松耦合、开源的微服务集合。
微服务集合由4个微服务层和两个增强的基础系统服务组成,提供从物理域数据采集到信息域数据处理等一系列的服务。

![iot-dc3-architecture](dc3/images/iot-dc3-architecture2.jpg)

[`Spring Cloud Netflix`](https://cloud.spring.io/spring-cloud-netflix)、
[`Spring Cloud Gateway`](https://cloud.spring.io/spring-cloud-gateway)、
[`Spring Cloud Security`](https://cloud.spring.io/spring-cloud-security)、
[`Spring Cloud OpenFeign`](https://cloud.spring.io/spring-cloud-openfeign)、
[`Spring Cloud Config`](https://cloud.spring.io/spring-cloud-config)、
[`Spring Cloud Bus`](https://cloud.spring.io/spring-cloud-bus) 
等微服务模块。

### DC3 IOT 镜像资源

[IOT DC3 Docker Hub Repositories](https://hub.docker.com/u/pnoker)

### 联系作者

邮箱:pnokers@icloud.com

感谢:`lombok`、`netty`、`spring boot`、`spring cloud`、[`s7connector`](https://github.com/s7connector/s7connector) 等提供的工具以及源码

### 参与贡献

1. Fork项目到自己的repo
2. clone到本地
3. 修改代码(dev分支)
4. commit后push到自己的库（dev分支）
5.  pull request
6. 等待作者合并

### 通用指令

```bash
# Git强制覆盖本地
git fetch --all && git reset --hard origin/master && git pull

# Maven操作
mvn clean package

# Yarn 操作
# 安装项目全部依赖
yarn
# 添加|更新|删除依赖
yarn add|upgrade|remove [package]
# 启动项目
yarn run [script] [<args>]

# Docker操作命令
# list
docker images
# build
docker build -t pnoker/dc3-dbs:3.0 .
# delete
docker rmi -f pnoker/dc3-dbs:3.0
# run
docker run -d -p 80:8080 --name dc3-dbs -h iotdc3.dbs --link dc3-register:iotdc3.register  pnoker/dc3-dbs:3.0
# stop/start/restart
docker start|start|restart pnoker/dc3-dbs:3.0
# exec
docker exec -i -t  pnoker/dc3-dbs:3.0 /bin/bash
```