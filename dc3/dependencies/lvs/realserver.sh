#!/bin/bash

NETWORK_CARD=lo

# 需要结合实际情况填写
SNS_VIP=192.168.1.100

# Config real server and apply no arp
case "$1" in
start)
   ifconfig $NETWORK_CARD:0 $SNS_VIP netmask 255.255.255.255 broadcast $SNS_VIP
   /sbin/route add -host $SNS_VIP dev $NETWORK_CARD:0
   echo "1" >/proc/sys/net/ipv4/conf/$NETWORK_CARD/arp_ignore
   echo "2" >/proc/sys/net/ipv4/conf/$NETWORK_CARD/arp_announce
   echo "1" >/proc/sys/net/ipv4/conf/all/arp_ignore
   echo "2" >/proc/sys/net/ipv4/conf/all/arp_announce
   sysctl -p >/dev/null 2>&1
   echo "RealServer Start OK"
   ;;
stop)
   ifconfig $NETWORK_CARD:0 down
   route del $SNS_VIP >/dev/null 2>&1
   echo "0" >/proc/sys/net/ipv4/conf/$NETWORK_CARD/arp_ignore
   echo "0" >/proc/sys/net/ipv4/conf/$NETWORK_CARD/arp_announce
   echo "0" >/proc/sys/net/ipv4/conf/all/arp_ignore
   echo "0" >/proc/sys/net/ipv4/conf/all/arp_announce
   echo "RealServer Stop OK"
   ;;
*)
   echo "Usage: $0 {start|stop}"
   exit 1
esac
exit 0