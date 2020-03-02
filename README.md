:boom: 非常欢迎广大兴趣爱好者的加入，第一时间 [联系作者](#6-联系作者)，`show me you code`，让我们肩并肩 :alien:！
>
:rocket: 说明：项目目前处于代码整合阶段（其他功能初版已完成，正在进行协议的整合），敬请关注，你的 `Star` :star: [ [GitHub](https://github.com/pnoker/iot-dc3) , [Gitee](https://gitee.com/pnoker/iot-dc3) ]，是我们动力的源泉，谢谢你们 :tada:！

<p align="center">
    <img src="./dc3/images/iot-dc3-logo.png" width="400"><br>
    <a href="https://travis-ci.org/pnoker/iot-dc3"><img src="https://travis-ci.org/pnoker/iot-dc3.svg?branch=master"></a>
    <a href="https://codecov.io/gh/pnoker/iot-dc3"><img src="https://codecov.io/gh/pnoker/iot-dc3/branch/master/graph/badge.svg"></a><br>
	<a><img src="https://img.shields.io/badge/JDK-1.8-green.svg"></a>
	<a><img src="https://img.shields.io/badge/Spring Boot-2.2.4.RELEASE-blue.svg"></a>
	<a><img src="https://img.shields.io/badge/Spring Cloud-Hoxton.SR1-blue.svg"></a>
	<a href="https://github.com/pnoker/iot-dc3/blob/master/LICENSE"><img src="https://img.shields.io/github/license/pnoker/iot-dc3.svg"></a>	
	<br><strong>DC3是基于Spring Cloud的开源可分布式物联网(IOT)平台,用于快速开发、部署物联设备接入项目,是一整套物联系统解决方案。<br>IOT DC3 is an open source, distributed Internet of Things (IOT) platform based on Spring Cloud. It is used for rapid development of IOT projects and management of IOT devices. It is a set of solutions for IOT system.</strong>
</p>

------

 ### 1 什么是DC3 IOT平台？

 ![iot-dc3-architecture](dc3/images/iot-dc3-architecture1.jpg)

 #### 1.1 模块

 * 设备微服务层:用于提供标准或者私有协议连接物理设备的`SDK`;
 * 核心微服务层:用于提供微服务注册中心、设备指令接口、设备注册与关联配对、数据管理中心,是所有微服务交互的核心部分;
 * 支持微服务层:用于提供任务调度、报警与消息通知、日志管理;
 * 开放微服务层:用于提供数据开放等服务...

  #### 1.2 DC3致力于

 * 跨平台分布式、多设备、多协议（目前支持`Mqtt(:hammer:)`、`Rtsp`、`Rtmp`、`Http(:hammer:)`、`Plc-S7(:hammer:)`、`Opc(:hammer:)`、`Opc-Ua(:hammer:)`、`自定义Tcp(:hammer:)`、`自定义Socket(:hammer:)`）;
 * 物联设备数据采集、边缘处理、数据持久化、数据缓存、功能性微服务调用;
 * 物联驱动端代码快速开发,快速安全的接入设备和物联设备管理（目前支持`Java`）;
 * 安全权限控制、数据加密（目前支持`AES\RSA`）;
 * 物联设备远程指令透传、监控和控制;
 * 对外开放数据接口及微服务接口;
 * 支持网关端和云端部署;
 * 容器化部署。

 #### 1.3 DC3设计为

* 可伸缩:水平可伸缩的平台,构建使用领先的`Spring Cloud`开源技术;
* 容错:没有单点故障弱,集群中的每个节点是相同的;
* 健壮和高效:单一服务器节点可以处理甚至数百成千上万的设备根据用例;
* 可定制:添加新的设备协议,并注册到服务中心;
* 跨平台:使用`Java`环境可异地、分布式多平台部署;
* 完善性:设备快速接入、注册、权限校验;
* 安全:数据加密传输;
* Docker:容器化。

### 2 DC3 IOT平台架构？

DC3 平台是基于`Spring Cloud`架构开发的,是一系列松耦合、开源的微服务集合。
微服务集合由4个微服务层和两个增强的基础系统服务组成,提供从物理域数据采集到信息域数据处理等一系列的服务。

![iot-dc3-architecture](dc3/images/iot-dc3-architecture2.jpg)

[`Spring Cloud Netflix`](https://cloud.spring.io/spring-cloud-netflix)、[`Spring Cloud Gateway`](https://cloud.spring.io/spring-cloud-gateway)、[`Spring Cloud Security`](https://cloud.spring.io/spring-cloud-security)、[`Spring Cloud OpenFeign`](https://cloud.spring.io/spring-cloud-openfeign)、[`Spring Cloud Config`](https://cloud.spring.io/spring-cloud-config)、[`Spring Cloud Bus`](https://cloud.spring.io/spring-cloud-bus) 等微服务模块。

### 3 联系作者

:whale2: 邮箱:pnokers@icloud.com

**:mega: 非常欢迎**
- 提交`issue`，请标明遇到的问题、开发环境和如何复现；
- 提交`pull request`改进 `iot-dc3` 的代码。
- 提出新想法和设计方案；
- 参与平台贡献。

:lollipop: 感谢:`lombok`、`netty`、`spring boot`、`spring cloud`、[`s7connector`](https://github.com/s7connector/s7connector) 等提供的工具以及源码。

### 4 项目文档

详细内容请阅读 [`WiKi`](https://github.com/pnoker/iot-dc3/wiki) 文档。

> 文档持续更新中...

**其中包括：安装配置文档、部署文档、项目结构说明、平台介绍等内容。**