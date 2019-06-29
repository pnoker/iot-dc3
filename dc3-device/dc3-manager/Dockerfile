FROM anapsix/alpine-java:8_server-jre_unlimited

MAINTAINER pnokers.icloud.com

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

RUN mkdir -p /dc3-device/dc3-manager

WORKDIR /dc3-manager

EXPOSE 8300

ADD ./dc3-manager/target/dc3-manager.jar ./

CMD java -Djava.security.egd=file:/dev/./urandom -jar dc3-manager.jar
