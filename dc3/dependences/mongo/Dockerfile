FROM mongo:4.0-xenial
MAINTAINER pnoker <pnokers.icloud.com>

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && mkdir -p /dc3-mongo/config/

WORKDIR /dc3-mongo

COPY init_mongo.js ./config/
COPY launch-dc3-mongo.sh ./config/

EXPOSE 27017
