> 当开发环境中使用 `docker` 启动的 `rabbitmq` 服务出现故障时，可以使用以下指令进行重置：


```bash
cd dc3

# Remove stopped rabbitmq container and volume
docker container stop dc3-rabbitmq
docker container rm dc3-rabbitmq
docker volume rm dc3_rabbitmq

# Rebuild rabbitmq and start
docker-compose build rabbitmq
docker-compose up -d rabbitmq
```