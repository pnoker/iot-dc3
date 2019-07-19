FROM anapsix/alpine-java:8_server-jre_unlimited
MAINTAINER pnoker <pnokers.icloud.com>

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && mkdir -p /dc3-device/dc3-virtual

WORKDIR /dc3-device/dc3-virtual

ADD ./target/dc3-virtual.jar ./

EXPOSE 8610

CMD java -Djava.security.egd=file:/dev/./urandom -jar dc3-virtual.jar
