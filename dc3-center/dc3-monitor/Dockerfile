FROM anapsix/alpine-java:8_server-jre_unlimited

MAINTAINER pnokers.icloud.com

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

RUN mkdir -p /dc3-center/dc3-monitor

WORKDIR /dc3-center/dc3-monitor

EXPOSE 8200

ADD ./dc3-center/dc3-monitor/target/dc3-monitor.jar ./

CMD java -Djava.security.egd=file:/dev/./urandom -jar dc3-monitor.jar
