FROM anapsix/alpine-java:8_server-jre_unlimited
MAINTAINER pnoker <pnokers.icloud.com>

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && mkdir -p /dc3-gateway

WORKDIR /dc3-gateway

ADD ./target/lib ./lib
ADD ./target/classes ./classes
ADD ./target/dc3-gateway.jar.original ./

EXPOSE 8300

CMD java -Djava.security.egd=file:/dev/./urandom -jar dc3-gateway.jar.original
