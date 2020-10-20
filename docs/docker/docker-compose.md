### Docker Compose

> - 使用 Docker Compose 可以轻松、高效的管理容器，它是一个用于定义和运行多容器 Docker 的应用程序工具
>
> - Docker Compose 将所管理的容器分为三层，分别是工程（project）、服务（service）、容器（container）
>
> - Docker Compose 运行目录下的所有文件（docker-compose.yml）组成一个工程,一个工程包含多个服务，每个服务中定义了容器运行的镜像、参数、依赖，一个服务可包括多个容器实例



### 安装

> 这里以按照 `1.27.4` 版本为例，最新版本可以在 [https://github.com/docker/compose/releases](https://github.com/docker/compose/releases) 查询到。


```bash
# 该步骤将 docker-compose 可执行文件下载到目标机器的 /usr/local/bin/docker-compose 位置；
# 如果网络比较慢，也可以手动下载，然放到 /usr/local/bin/docker-compose 位置。
sudo curl -L https://github.com/docker/compose/releases/download/1.27.4/docker-compose-$(uname -s)-$(uname -m) -o /usr/local/bin/docker-compose

# 该步骤给 /usr/local/bin/docker-compose 可执行文件授权
sudo chmod +x /usr/local/bin/docker-compose

# 查看 docker-compose 版本，检查是否安装成功
docker-compose -v
```



### docker-compose.yml 属性

> Compose允许用户通过一个docker-compose.yml模板文件（YAML 格式）来定义一组相关联的应用容器为一个项目（project）。
>
> Compose模板文件是一个定义服务、网络和卷的YAML文件。Compose模板文件默认路径是当前目录下的docker-compose.yml，可以使用.yml或.yaml作为文件扩展名。
> Docker-Compose标准模板文件应该包含version、services、networks 三大部分，最关键的是services和networks两个部分。

```yaml
version: '3.3'

services:
  gateway:
    build:
      context: ../dc3-gateway/
      dockerfile: ./Dockerfile
    image: pnoker/dc3-gateway:1.0.0
    restart: always
    ports:
      - 8000:8000
    container_name: dc3-gateway
    hostname: dc3-gateway
    volumes:
      - logs:/dc3-gateway/dc3/logs
    networks:
      dc3net:
        aliases:
          - dc3-gateway
volumes:
  logs:

networks:
  dc3net:
    driver: 'bridge'
```

- **version**：docker-compose的版本号
- **context**：Dockerfile的文件路径，也可以是到链接到git仓库的url，当提供的值是相对路径时，被解析为相对于撰写文件的路径，此目录也是发送到Docker守护进程的context
- **dockerfile**：使用dockerfile文件来构建，必须指定构建路径
- **image**：指定服务的镜像名称或镜像ID。如果镜像在本地不存在，Compose将会尝试拉取镜像 
- **restart**：重启策略
- **ports**：端口映射列表，宿主机端口：容器端口
- **container_name**：Compose容器名称格式是：<项目名称><服务名称><序号>
  可以自定义项目名称、服务名称，但如果想完全控制容器的命名，可以使用标签指定
- **hostname**：容器的hostname
- **volumes**：挂载一个目录或者一个已存在的数据卷容器，可以直接使用 [HOST:CONTAINER]格式，或者使用[HOST:CONTAINER:ro]格式，后者对于容器来说，数据卷是只读的，可以有效保护宿主机的文件系统
- **networks**：设置网络模式



### 常见指令

#### 1.ps（列出所有运行容器）

```bash
docker-compose ps
```

#### 2.logs（查看服务日志输出）

```bash
docker-compose logs
```

#### 3.port（打印绑定的公共端口）

```bash
docker-compose port dc3-web 443
```

#### 4.build（构建或者重新构建服务）

```bash
docker-compose build
```

#### 5.start（启动指定服务已存在的容器）

```bash
docker-compose start dc3-web
```

#### 6.stop（停止已运行的服务的容器）

```bash
docker-compose stop dc3-web
```

#### 7.rm（删除指定服务的容器）

```bash
docker-compose rm dc3-web
```

#### 8.up（构建、启动容器）

```bash
docker-compose up
```

#### 9.kill（通过发送 SIGKILL 信号来停止指定服务的容器）

```bash
docker-compose kill dc3-web
```

#### 10.pull（下载服务镜像）

```bash
docker-compose pull
```

#### 11.scale（设置指定服务运气容器的个数）

```bash
docker-compose scale dc3-web=3
```

#### 12.run（在一个服务上执行一个命令）

```bash
docker-compose run web bash
```