> 当开发环境中使用 `docker` 启动的 `mongo` 服务出现故障时，可以使用以下指令进行重置：


```bash
cd dc3

# Remove stopped mongo container and volume
docker container stop dc3-mongo
docker container rm dc3-mongo
docker volume rm dc3_mongo

# Rebuild mongo and start
docker-compose build mongo
docker-compose up -d mongo
```