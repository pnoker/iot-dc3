FROM mariadb:10.4
MAINTAINER pnoker <pnokers.icloud.com>

ENV MYSQL_ROOT_PASSWORD iotdc3

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

COPY ./iot-dc3.sql /docker-entrypoint-initdb.d
