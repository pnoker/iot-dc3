FROM rabbitmq:3.7-alpine

MAINTAINER pnoker pnokers.icloud.com

#修改时区，兼容CentOS7、RHEL7、Scientific Linux 7、Oracle Linux 7
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

ENV RABBITMQ_DEFAULT_USER iotdc3
ENV RABBITMQ_DEFAULT_PASS iotdc3

RUN rabbitmq-plugins enable rabbitmq_management rabbitmq_mqtt rabbitmq_stomp