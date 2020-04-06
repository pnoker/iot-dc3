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

#### DC3设计模块

 * 设备微服务层:用于提供标准或者私有协议连接物理设备的`SDK`;
 * 核心微服务层:用于提供微服务注册中心、设备指令接口、设备注册与关联配对、数据管理中心,是所有微服务交互的核心部分;
 * 支持微服务层:用于提供任务调度、报警与消息通知、日志管理;
 * 开放微服务层:用于提供数据开放等服务...

#### DC3设计

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

[`Spring Cloud Netflix`](https://cloud.spring.io/spring-cloud-netflix)、[`Spring Cloud Gateway`](https://cloud.spring.io/spring-cloud-gateway)、[`Spring Cloud Security`](https://cloud.spring.io/spring-cloud-security)、[`Spring Cloud OpenFeign`](https://cloud.spring.io/spring-cloud-openfeign) 等微服务模块。

### 3 项目文档

一键启动Demo，按照以下三行命令进行操作即可（另外也可以参考详细的 [安装部署](https://github.com/pnoker/iot-dc3/wiki/%E5%AE%89%E8%A3%85%E9%85%8D%E7%BD%AE) 文档），首次启动需要下载镜像会比较慢（可在`docker`中配置国内加速源:`http://f1361db2.m.daocloud.io`,如何配置加速源，可参考[如何配置镜像加速？](https://github.com/pnoker/iot-dc3/wiki/Docker%E5%8A%A0%E9%80%9F)），执行完访问 [https://localhost](https://localhost:8000) `是https哈`，切换到 '数据' 菜单即可查看 Virtual 驱动定时采集的模拟数据 。

**注：DC3 Web UI 仅供开发环境&demo演示环境的配置使用，该UI不属于DC3项目的一部分**

> 必须保证提前安装了 `docker`和`docker-compose`

```bash
# git clone
git clone https://github.com/pnoker/iot-dc3.git

# cd
cd iot-dc3/dc3

# docker compose up
docker-compose -f docker-compose-demo.yml up -d
```

<p align="center">
<img src="./dc3/images/iot-dc3-web.png"><br>
</p>

详细说明内容请阅读 [`WiKi`](https://github.com/pnoker/iot-dc3/wiki) 文档。

**其中包括：安装配置文档、部署文档、项目结构说明、平台介绍等内容。**

### 4 联系作者

:whale2: 邮箱:pnokers@icloud.com

**:mega: 非常欢迎**
 - 提交`issue`，请标明遇到的问题、开发环境和如何复现；
 - 提交`pull request`改进 `iot-dc3` 的代码；
 - 提出新想法和设计方案；
 - 参与平台贡献,通过以下二维码加入微信交流群。

<p align="center">
<img src="./dc3/images/wechart.png" width="300"><br>
</p>

:lollipop: 感谢:`lombok`、`netty`、`spring boot`、`spring cloud`、[`s7connector`](https://github.com/s7connector/s7connector) 等提供的工具以及源码。

### 5 大家关心的问题

- 项目目前开发到什么阶段了？

> 完全情况：70% \
> 其中: \
> 网关服务 需要完善开发，驱动协议需要丰富（包括Mqtt,Opcua,Opcda）\
> 管理配置服务 需要优化（接口部分做小改动）\
> 数据存储&开放服务 需要添加流式计算模块（后期支持）\
> 驱动快速开发SDK模块 需要拓展功能（后期支持边缘计算）

- 项目目前支持的协议有哪些？

> 已完成的协议驱动：rtmp、plcs7、socket（client模式、server模式）、opcda \
> 计划开发协议驱动: mqtt、opcua、modbus-tcp

- 并发能力如何？

> 16G,i5机器可目前测试可承受5万并发（测试工具jMeter），目前Mongo数据库部分当并发很大时有较大的延时，后期采用Cassandra替代Mongo。

- Demo的镜像pull超时或报错？

> 可以配置加速源，请参考 [如何配置镜像加速？](https://github.com/pnoker/iot-dc3/wiki/Docker%E5%8A%A0%E9%80%9F) 进行配置。