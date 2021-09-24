> 当开发环境中使用 `docker` 启动的 `mysql` 服务出现故障时，可以使用以下指令进行重置：


```bash
cd dc3

# Remove stopped mysql container and volume
docker container stop dc3-mysql
docker container rm dc3-mysql
docker volume rm dc3_mysql

# Rebuild mysql and start
docker-compose build mysql
docker-compose up -d mysql
```