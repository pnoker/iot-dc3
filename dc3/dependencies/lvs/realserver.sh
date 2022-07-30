#!/bin/bash

#
# Copyright 2022 Pnoker All Rights Reserved
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

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