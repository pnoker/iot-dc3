cd ../
mvn clean -U package
# shellcheck disable=SC2164
cd dc3/
docker-compose build mysql redis mongo rabbitmq register auth monitor
docker-compose up -d mysql redis mongo rabbitmq register auth monitor