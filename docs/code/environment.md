### 注意事项

> **⚠️请注意；⚠️请注意；⚠️请注意：**
>
> 由于是开发模式，数据库和消息组件均采用容器模式启动，如果是实际项目中请勿使用docker部署数据库和消息组件服务（MySQL、MongoDB、Redis、RabbitMQ）,配置文件在dc3/dependencies下，可手动自行配置。
>
> MySQL、MongoDB、Redis、RabbitMQ的用户名密码均为dc3，如果想修改用户名密码可以全文搜索，并进行统一修改。
>
>
> 其中：MySQL、MongoDB、Redis、RabbitMQ 用户名密码均为：`dc3`
>
> MySQL、MongoDB、Redis、RabbitMQ 用户名密码的配置均在：dc3-common/dc3-config/src/main/resources/bootstrap.yml


---

### Docker

> 开发环境: 推荐安装 Docker Desktop

- Mac : [Docker Desktop For Mac](https://download.docker.com/mac/edge/Docker.dmg)

- Windows : [Docker Desktop For Windows](https://download.docker.com/win/edge/Docker%20Desktop%20Installer.exe)

> 生产环境: 推荐在宿主机上安装 Docker 服务

- 推荐 Centos : [Install Docker Engine on CentOS](https://docs.docker.com/engine/install/centos/)

- Ubuntu : [Install Docker Engine on Ubuntu](https://docs.docker.com/engine/install/ubuntu/)

- Debian : [Install Docker Engine on Debian](https://docs.docker.com/engine/install/debian/)


### Java

- Java JDK 1.8 [Java SE Development Kit 8 Downloads](https://www.oracle.com/java/technologies/javase/javase-jdk8-downloads.html)


### Maven

- Maven 3.6 [Installing Apache Maven](http://maven.apache.org/install.html)


### MySQL

> 开发环境: 推荐使用 Docker 启动该服务

```bash
cd iot-dc3/dc3

#构建 DC3 Mysql 服务
docker-compose build mysql

#创建并启动
docker-compose up -d mysql

#停止
docker-compose stop mysql
```

> 生产环境: 推荐在宿主机上安装 MySQL 服务

导入 iot-dc3 数据库脚本文件, dc3/dependencies/mysql/iot-dc3.sql ,使用 Navicat 之类的软件即可



### MongoDB

> 开发环境: 推荐使用 Docker 启动该服务

```bash
cd iot-dc3/dc3

#构建 DC3 MongoDB 服务
docker-compose build mongo

#创建并启动
docker-compose up -d mongo

#停止
docker-compose stop mongo
```

> 生产环境: 推荐在宿主机上安装 MongoDB 服务

导入 iot-dc3.js 数据库脚本文件, dc3/dependencies/mongo/iot-dc3.js ,使用 Navicat 之类的软件即可



### Redis

> 开发环境: 推荐使用 Docker 启动该服务

```bash
cd iot-dc3/dc3

#构建 DC3 Redis 服务
docker-compose build redis

#创建并启动
docker-compose up -d redis

#停止
docker-compose stop redis
```

> 生产环境: 推荐在宿主机上安装 MongoDB 服务

Reids 配置文件在 dc3/dependencies/redis/redis.conf



### RabbitMQ

> 开发环境: 推荐使用 Docker 启动该服务

```bash
cd iot-dc3/dc3

#构建 DC3 RabbitMQ 服务
docker-compose build rabbitmq

#创建并启动
docker-compose up -d rabbitmq

#停止
docker-compose stop rabbitmq
```

> 生产环境: 推荐在宿主机上安装 MongoDB 服务

RabbitMQ 配置文件在 dc3/dependencies/rabbitmq/rabbitmq.conf