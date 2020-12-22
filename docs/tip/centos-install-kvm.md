## 检测是否支持 KVM

> KVM 是基于 x86 虚拟化扩展(Intel VT 或者 AMD-V) 技术的虚拟机软件，所以查看 CPU 是否支持 VT 技术，就可以判断是否支持KVM。

```bash
cat /proc/cpuinfo | egrep 'vmx|svm'

# 结果中有vmx（Intel）或svm(AMD)字样，就说明CPU的支持
```

> 关闭SELinux

```bash
# 查看 SELinux 状态
getenforce

sestatus

# 临时关闭
setenforce 0

# 永久关闭
# 将 selinux 中的 SELinux=enforcing 修改为 SELinux=disabled
vim /etc/selinux/config
```



## 安装 KVM

> - `qemu-kvm` 主要的KVM程序包
> - `python-virtinst` 创建虚拟机所需要的命令行工具和程序库
> - `virt-manager` GUI虚拟机管理工具
> - `virt-top` 虚拟机统计命令
> - `virt-viewer` GUI连接程序，连接到已配置好的虚拟机
> - `libvirt` C语言工具包，提供libvirt服务
> - `libvirt-client` 为虚拟客户机提供的C语言工具包
> - `virt-install` 基于libvirt服务的虚拟机创建命令
> - `bridge-utils` 创建和管理桥接设备的工具


```bash
# 安装
yum -y install qemu-kvm python-virtinst libvirt libvirt-python virt-manager libguestfs-tools bridge-utils virt-install

# 重启
reboot

# 查看
lsmod | grep kvm

# 开启 KVM 服务并设置开机启动
systemctl start libvirtd
systemctl enable libvirtd
```



## KVM 中安装虚拟机

> cd /etc/sysconfig/network-scripts/



### 宿主机网卡配置

##### 配置网桥 ifcfg-bridge0

```bash
TYPE=Bridge
BOOTPROTO=static
IPADDR=192.168.1.200
PREFIX=24
GATEWAY=192.168.1.1
DNS1=192.168.1.1
NAME=bridge0
DEVICE=bridge0
ONBOOT=yes
```

##### 配置网卡 ifcfg-enp8s0

```bash
TYPE=Ethernet
BOOTPROTO=none
BRIDGE=bridge0
NAME=enp8s0
DEVICE=enp8s0
ONBOOT=yes
```



### 重启网卡服务

```bash
systemctl restart network
```



### 安装centos7虚拟机

```bash
virt-install --virt-type=kvm --name=centos7-base --vcpus=2 --memory=4096 --location=/home/pnoker/Downloads/iso/CentOS-7-x86_64-Minimal-2009.iso --disk path=/data/kvm/images/centos7-base.qcow2,size=40,format=qcow2 --network bridge=bridge0 --graphics none --extra-args='console=ttyS0' --force
```



### 虚拟机网卡配置

##### 配置网卡 ifcfg-ens3

```bash
TYPE=Ethernet
BOOTPROTO=static
IPADDR=192.168.1.10
PREFIX=24
GATEWAY=192.168.1.1
DNS1=192.168.1.1
DEFROUTE=yes
PEERDNS=yes
PEERROUTES=yes
IPV4_FAILURE_FATAL=no
IPV6INIT=yes
IPV6_AUTOCONF=yes
IPV6_DEFROUTE=yes
IPV6_PEERROUTES=yes
IPV6_FAILURE_FATAL=no
NAME=ens3
UUID=369be450-f9b3-468d-a66d-661944f9d7f4
DEVICE=ens3
ONBOOT=yes
```



### 启动网卡

```bash
ifup ens3
```



### 常见指令

KVM虚拟机的管理主要是通过virsh命令对虚拟机进行管理

帮助文档
```bash
[root@localhost ~]# virsh --help
```

查看虚拟机状态
```bash
[root@localhost ~]# virsh list --all
 Id    名称                         状态
----------------------------------------------------
 4     win2k8r2                       running
```
关机
```bash
[root@localhost ~]# virsh shutdown win2k8r2
```
强制关闭电源
```bash
[root@localhost ~]# virsh destroy win2k8r2
```
通过配置文件创建虚拟机
```bash
[root@localhost ~]# virsh create /etc/libvirt/qemu/win2k8r2.xml
```
设置虚拟机开机自启
```bash
[root@localhost ~]# virsh autostart win2k8r2
[root@localhost ~]# ll /etc/libvirt/qemu/autostart/
总用量 0
lrwxrwxrwx 1 root root 30 1月  24 13:06 win2k8r2.xml -> /etc/libvirt/qemu/win2k8r2.xml
```
到处虚拟机配置文件
```bash
[root@localhost ~]# virsh dumpxml win2k8r2 > /etc/libvirt/qemu/win2k8r2_bak.xml
```
删除虚拟机（该命令只删除配置文件，并不删除磁盘文件）
```bash
[root@localhost ~]# virsh undefine win2k8r2
```
通过导出备份的配置文件恢复原KVM虚拟机的定义，并重新定义虚拟机。
```bash
[root@localhost ~]# mv /etc/libvirt/qemu/win2k8r2_bak.xml /etc/libvirt/qemu/win2k8r2.xml
[root@localhost ~]# virsh define /etc/libvirt/qemu/win2k8r2.xml
```
编辑配置文件
```bash
[root@localhost ~]# virsh edit win2k8r2
```
挂起
```bash
[root@localhost ~]# virsh suspend win2k8r2
```
恢复
```bash
[root@localhost ~]# virsh resume win2k8r2
```
创建存储卷
```bash
[root@localhost ~]# qemu-img create -f qcow2 /data/kvmstorage/centos7.qcow2 20G
Formatting '/data/kvmstorage/centos7.qcow2', fmt=qcow2 size=21474836480 encryption=off cluster_size=65536 lazy_refcounts=off 
[root@localhost ~]# ll //data/kvmstorage
总用量 7437168
-rw-r--r-- 1 root root      197120 1月  24 13:21 centos7.qcow2
-rw------- 1 qemu qemu 42956488704 1月  24 13:21 win2k8r2.qcow2
```
生成虚拟机
```bash
[root@localhost ~]# virt-install --virt-type kvm --name centos --ram 1024 \
  --disk /data/kvmstorage/centos7.qcow2,format=qcow2 \
  --network bridge=br0 \
  --graphics vnc,listen=0.0.0.0 --noautoconsole \
  --os-type=linux --os-variant=rhel7 \
  --location=/data/iso/CentOS-7-x86_64-Minimal-1804.iso

[root@localhost ~]# virsh list --all
 Id    名称                         状态
----------------------------------------------------
 5     win2k8r2                       running
 7     centos                         running
```