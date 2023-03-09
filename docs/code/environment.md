### 注意事项

> **⚠️ 请注意；⚠️ 请注意；⚠️ 请注意：**
>
> 为了便于快速搭建**开发环境**，数据库（MySQL、Redis、MongoDB）和消息组件（RabbitMQ）均采用容器启动。
>
> 如果是在**生产环境**下，<u>建议直接在宿主机上安装数据库和消息组件</u>，数据库（MySQL、Redis、MongoDB）和消息组件（RabbitMQ）。

### 开发环境

-   Java JDK 1.8 [Java SE Development Kit 8 Downloads](https://www.oracle.com/java/technologies/javase/javase-jdk8-downloads.html)

-   Maven 3.8 [Installing Apache Maven](http://maven.apache.org/install.html)

-   Mac : [Docker Desktop For Mac](https://download.docker.com/mac/edge/Docker.dmg)

-   Windows : [Docker Desktop For Windows](https://download.docker.com/win/edge/Docker%20Desktop%20Installer.exe)

### 基础服务

> MySQL、MongoDB、Redis、RabbitMQ 是 DC3 开发所需的数据库和消息中间件。

```bash
cd iot-dc3/dc3

#构建 MySQL、MongoDB、Redis、RabbitMQ 服务
docker-compose build mysql mongo redis rabbitmq

#创建并启动 MySQL、MongoDB、Redis、RabbitMQ 服务
docker-compose up -d mysql mongo redis rabbitmq

#停止 MySQL、MongoDB、Redis、RabbitMQ 服务
docker-compose stop mysql mongo redis rabbitmq
```
