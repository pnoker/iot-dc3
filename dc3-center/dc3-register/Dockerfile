FROM anapsix/alpine-java:8_server-jre_unlimited

MAINTAINER pnoker pnokers.icloud.com

#修改时区，兼容CentOS7、RHEL7、Scientific Linux 7、Oracle Linux 7
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

RUN mkdir -p /dc3-center/dc3-register

WORKDIR /dc3-center/dc3-register

EXPOSE 8100

ADD ./dc3-center/dc3-register/target/dc3-register.jar ./

CMD java -Djava.security.egd=file:/dev/./urandom -jar dc3-register.jar
