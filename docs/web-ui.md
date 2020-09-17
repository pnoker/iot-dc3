> 说明： IOT DC3 本身没有 Web ，该 Demo Web UI 是基于 DC3 接口开发的调试应用，该应用仅供开发、调试和测试接口用途。
>
> 关于 IOT DC3 的管理平台，还在规划阶段，后期开源。

### 构建 Demo Web UI

```bash
git clone https://gitee.com/pnoker/dc3-web.git
cd dc3-web

#这步至关重要，请务必使用 cnpm 进行 install
npm install -g cnpm --registry=https://registry.npm.taobao.org
cnpm install
```

### 启动 Demo Web UI

```bash
# run
npm run serve

# build 
npm run build

# docker build
cd dc3
docker-compose build

# docker run 
docker-compose up -d
```

### 访问 Demo Web UI

访问 https://localhost 即可进入登陆页面，点击登陆即可，⚠️请注意是 https 哈