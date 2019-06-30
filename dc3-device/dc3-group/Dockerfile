FROM anapsix/alpine-java:8_server-jre_unlimited

MAINTAINER pnoker pnokers.icloud.com

#修改时区，兼容CentOS7、RHEL7、Scientific Linux 7、Oracle Linux 7
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

RUN mkdir -p /dc3-device/dc3-group

WORKDIR /dc3-device/dc3-group

EXPOSE 8700

ADD ./dc3-device/dc3-group/target/dc3-group.jar ./

CMD java -Djava.security.egd=file:/dev/./urandom -jar dc3-group.jar
