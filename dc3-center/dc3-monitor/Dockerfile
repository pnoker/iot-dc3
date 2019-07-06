FROM anapsix/alpine-java:8_jdk_unlimited
MAINTAINER pnoker <pnokers.icloud.com>

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && mkdir -p /dc3-center/dc3-monitor

WORKDIR /dc3-center/dc3-monitor

ADD ./target/lib ./lib
ADD ./target/classes ./classes
ADD ./target/dc3-monitor.jar.original ./

EXPOSE 8200

CMD java -Djava.security.egd=file:/dev/./urandom -jar dc3-monitor.jar.original
