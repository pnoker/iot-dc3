### 必要软件环境

> **前提：** 务必保证至少需要给 docker 分配：1 核 CPU 以及 4G 以上的运行内存！

-   JDK : 推荐使用 `Oracle JDK 1.8` 或者 `OpenJDK8`，理论来说其他版本也行；

-   Maven : 推荐使用 `Maven 3.8` ，理论来说其他版本也行；

-   IDE : `IntelliJ IDEA` 或者 `Eclipse`，理论来说其他 Java IDE 也行；；

-   Docker : 需要提供 `docker` 和 `docker-compose` 指令，至少需要给 `docker` 分配 `4G` 的运行内存，建议配置国内镜像加速，下载镜像速度会快一些。

### Hosts 配置

> **说明：** 该步骤 <u>仅在本地开发电脑</u> 上进行配置，如果部署生产环境无需配置

-   Windows（需要使用管理员权限）：`c:\windows\system32\drivers\etc\hosts`

-   Linux：`/etc/hosts`

-   在 hosts 文件中添加以下内容，如果你在多台机器配置，可以将 `127.0.0.1` 替换成具体具体机器的 `IP`：

```bash
# Added by DC3
127.0.0.1       dc3-mysql
127.0.0.1       dc3-redis
127.0.0.1       dc3-mongo
127.0.0.1       dc3-rabbitmq
127.0.0.1       dc3-emqx
# dev
127.0.0.1       dc3-center-register
127.0.0.1       dc3-center-auth
127.0.0.1       dc3-center-manager
127.0.0.1       dc3-center-data
127.0.0.1       dc3-gateway
# End DC3
```

### 基础服务

> **说明：** 该步骤会启动必要的依赖服务 MySQL、MongoDB、Redis、RabbitMQ，建议直接在 docker 中启动，简单快捷

```bash
cd iot-dc3

# 提示：如果你在使用 mvn clean -U package 时失败，请使用以下指令
mvn -s dc3/dependencies/maven/settings.xml clean -U package

cd dc3
docker-compose build mysql redis mongo rabbitmq
docker-compose up -d mysql redis mongo rabbitmq
```

### 导入项目

> **说明**：务必使用 Maven 方式导入项目

![import-dc3](../images/idea/import-dc3.gif)

### 启动 Register、Auth、Manager、Data、Gateway 服务

> **注意**：在启动 `dc3-center-auth`、 `dc3-center-manager` 、 `dc3-center-data` 的时候由于开启了 `ASPECTJ` ，所以需要配置 `-javaagent:dc3/lib/aspectjweaver-1.9.5.jar`

在 Idea 中依次启动：

-   `dc3-center-register`
-   `dc3-center-auth`
-   `dc3-center-manager`
-   `dc3-center-data`
-   `dc3-center-gateway`

![](../images/idea/aspectj.png)

### 启动待开发的 驱动 程序

根据实际情况，启动 、开发 驱动程序

位置：dc3-driver/

目前已支持的驱动，需要根据实际项目情况，适当微调，也可以仿照现有的驱动开发其他的驱动模块：

-   plcs7
-   socket（client 模式、server 模式）
-   mqtt
-   opcda
-   opcua
-   modbus-tcp
