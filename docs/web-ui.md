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