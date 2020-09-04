### 注意事项

> **⚠️请注意；⚠️请注意；⚠️请注意：**
>
> 由于是开发模式，数据库和消息组件均采用容器模式启动，如果是实际项目中请勿使用docker部署数据库和消息组件服务（MySQL、MongoDB、Redis、RabbitMQ）,配置文件在dc3/dependences下，可手动自行配置。
>
> MySQL、MongoDB、Redis、RabbitMQ的用户名密码均为dc3，如果想修改用户名密码可以全文搜索，并进行统一修改。
>
>
> 其中：MySQL、MongoDB、Redis、RabbitMQ 用户名密码均为：`dc3`


### MySQL

#### Docker 上启动 MySQL 服务

> (推荐在 **开发** 环境使用 Docker 启动该服务)

```bash
cd iot-dc3/dc3

#构建 DC3 Mysql 服务
docker-compose build mysql

#创建并启动
docker-compose up -d mysql

#停止
docker-compose stop mysql
```

#### Centos 上启动 MySQL 服务

> (推荐在 **生产** 环境使用 Centos 部署该服务)

##### 1.安装 MySQL
```bash
wget -i -c http://dev.mysql.com/get/mysql57-community-release-el7-10.noarch.rpm
yum -y install mysql57-community-release-el7-10.noarch.rpm
yum -y install mysql-community-server
```

##### 2.启动
```bash
systemctl enable mysqld.service
systemctl start mysqld.service
```

##### 3.查看初始化密码
```bash
grep 'temporary password' /var/log/mysqld.log
```

##### 4.登陆 MySQL
```bash
mysql -uroot -p
#输入上述初始密码

#'new password'请替换成你想要设置的密码
ALTER USER 'root'@'localhost' IDENTIFIED BY 'new password';
```

##### 5.创建新的用户名和密码为 `dc3`

```bash
create user 'dc3'@'%' identified by 'dc3';
grant privileges on dc3.* to 'dc3'@'%';
flush privileges;
exit
```

##### 6.导入 iot-dc3 数据库脚本文件, dc3/dependences/mysql/iot-dc3.sql ,使用 Navicat 之类的软件即可。


### MongoDB

#### Docker 上启动 MongoDB 服务

> (推荐在 **开发** 环境使用 Docker 启动该服务)

```bash
cd iot-dc3/dc3

#构建 DC3 MongoDB 服务
docker-compose build mongo

#创建并启动
docker-compose up -d mongo

#停止
docker-compose stop mongo
```

#### Centos 上启动 MongoDB 服务




### Redis

#### Docker 上启动 Redis 服务

> (推荐在 **开发** 环境使用 Docker 启动该服务)

```bash
cd iot-dc3/dc3

#构建 DC3 Redis 服务
docker-compose build redis

#创建并启动
docker-compose up -d redis

#停止
docker-compose stop redis
```

#### Centos 上启动 Redis 服务




### RabbitMQ

#### Docker 上启动 RabbitMQ 服务

> (推荐在 **开发** 环境使用 Docker 启动该服务)

```bash
cd iot-dc3/dc3

#构建 DC3 RabbitMQ 服务
docker-compose build rabbitmq

#创建并启动
docker-compose up -d rabbitmq

#停止
docker-compose stop rabbitmq
```

#### Centos 上启动 RabbitMQ 服务