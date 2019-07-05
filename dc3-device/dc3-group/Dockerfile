FROM anapsix/alpine-java:8_server-jre_unlimited
MAINTAINER pnoker <pnokers.icloud.com>

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && mkdir -p /dc3-device/dc3-group

WORKDIR /dc3-device/dc3-group

ADD ./target/lib ./lib
ADD ./target/classes ./classes
ADD ./target/dc3-group.jar.original ./

EXPOSE 8700

CMD java -Djava.security.egd=file:/dev/./urandom -jar dc3-group.jar.original
