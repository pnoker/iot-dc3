> 当开发环境中使用 `docker` 启动的 `redis` 服务出现故障时，可以使用以下指令进行重置：


```bash
cd dc3

# Remove stopped redis container and volume
docker container stop dc3-redis
docker container rm dc3-redis
docker volume rm dc3_redis

# Rebuild redis and start
docker-compose build redis
docker-compose up -d redis
```