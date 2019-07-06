FROM anapsix/alpine-java:8_server-jre_unlimited
MAINTAINER pnoker <pnokers.icloud.com>

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && mkdir -p /dc3-transfer/dc3-opc-ua

WORKDIR /dc3-transfer/dc3-opc-ua

ADD ./target/lib ./lib
ADD ./target/classes ./classes
ADD ./target/dc3-opc-ua.jar.original ./

EXPOSE 8820

CMD java -Djava.security.egd=file:/dev/./urandom -jar dc3-opc-ua.jar.original
