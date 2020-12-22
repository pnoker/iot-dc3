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

