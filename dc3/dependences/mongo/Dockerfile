FROM mongo:4.0-xenial
MAINTAINER pnoker <pnokers.icloud.com>

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && mkdir -p /dc3-mongo/config/

WORKDIR /dc3-mongo

COPY ./iot-dc3.sql ./config/
COPY ./launch.sh ./config/

CMD ./config/launch.sh

EXPOSE 27017
