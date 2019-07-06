FROM rabbitmq:3.7-alpine
MAINTAINER pnoker <pnokers.icloud.com>

ENV RABBITMQ_DEFAULT_USER iotdc3
ENV RABBITMQ_DEFAULT_PASS iotdc3

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && rabbitmq-plugins enable rabbitmq_management rabbitmq_mqtt rabbitmq_stomp