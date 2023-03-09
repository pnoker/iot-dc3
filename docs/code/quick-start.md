# 快速启动

## 一、前提

-   确保机器有 16G 运行内存
-   推荐安装 Chrome，[Chrome 安装](#chrome安装)
-   确保安装了 Git，[Git 安装](#git安装)
-   确保安装了 Docker，[Docker 安装](#docker安装)

## 二、启动

```bash
# 1. 下载iot-dc3源码
git clone https://gitee.com/pnoker/iot-dc3.git
cd iot-dc3/dc3
# 1.1 启动后端容器
docker-compose -f docker-compose-demo.yml up -d

# 2. 下载iot-dc3-web源码
cd ../../
git clone https://gitee.com/pnoker/iot-dc3-web.git
cd iot-dc3-web/dc3
# 2.1 启动前端容器
docker-compose -f docker-compose-demo.yml up -d

```

## 三、访问

> 网页打不打，需要检查 docker 的服务是否都启动正常，一般经验是多等它一会就可以了。

待以上服务全部正常启动，访问 [http://localhost:8080](http://localhost:8080) 即可进入登陆页面！

---

## 安装指南

### Chrome 安装

[Chrome](https://www.google.com/chrome/) ，不一样的浏览器体验

### Git 安装

| 操作系统 | 链接                               |
| -------- | ---------------------------------- |
| Mac      | https://git-scm.com/download/mac   |
| Windows  | https://git-scm.com/download/win   |
| Linux    | https://git-scm.com/download/linux |

### Docker 安装

| 操作系统 | 链接                                                                                                |
| -------- | --------------------------------------------------------------------------------------------------- |
| Mac      | [Docker Desktop For Mac](https://download.docker.com/mac/edge/Docker.dmg)                           |
| Windows  | [Docker Desktop For Windows](https://download.docker.com/win/edge/Docker%20Desktop%20Installer.exe) |
| Centos   | [Install Docker Engine on CentOS](https://docs.docker.com/engine/install/centos/)                   |
| Ubuntu   | [Install Docker Engine on Ubuntu](https://docs.docker.com/engine/install/ubuntu/)                   |
| Debian   | [Install Docker Engine on Debian](https://docs.docker.com/engine/install/debian/)                   |
