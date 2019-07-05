FROM mariadb:10.4

MAINTAINER pnoker <pnokers.icloud.com>

#修改时区，兼容CentOS7、RHEL7、Scientific Linux 7、Oracle Linux 7
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

ENV MYSQL_ROOT_PASSWORD iotdc3

COPY ./iot-dc3.sql /docker-entrypoint-initdb.d
