

### 1.安装 KVM



### 1.1 检查 CPU 是否支持虚拟化

```bash
grep -E '(vmx|svm)' /proc/cpuinfo **
```



### 1.2 安装 kvm 相关工具包

- qemu-kvm（用户态管理工具）
- libvirt（命令行管理工具）
- virt-install（安装kvm工具）
- bridge-utils（桥接设备管理工具）

```bash
yum install -y qemu-kvm libvirt virt-install bridge-utils
```

确保 kvm 模块被加载

```bash
lsmod |grep kvm

# 例如：输出如下
#kvm_intel             174841  0 
#kvm                   578518  1 kvm_intel
#irqbypass              13503  1 kvm
```

如果没有加载，运行以下命令

```bash
modprobe kvm

modprobe kvm-intel
```



### 1.3 启动 libvirtd

```bash
systemctl enable libvirtd

systemctl start  libvirtd

systemctl status  libvirtd
```



### 1.4 配置 KVM 网桥模式

```bash
cd /etc/sysconfig/network-scripts/

#名称根据实际情况而定
cp ifcfg-ens32 ifcfg-br0

vim ifcfg-br0
```



编辑 `ifcfg-br0` 配置如下

vim ifcfg-br0

```bash
TYPE="Bridge"
PROXY_METHOD="none"
BROWSER_ONLY="no"
BOOTPROTO="none"
DEFROUTE="yes"
IPV4_FAILURE_FATAL="no"
IPV6INIT="yes"
IPV6_AUTOCONF="yes"
IPV6_DEFROUTE="yes"
IPV6_FAILURE_FATAL="no"
IPV6_ADDR_GEN_MODE="stable-privacy"
NAME="br0"
UUID="dd885bbb-43d6-4144-a434-ffd517965e76"
DEVICE="br0"
ONBOOT="yes"
IPADDR="192.168.1.200"
PREFIX="24"
GATEWAY="192.168.1.1"
DNS1="192.168.1.1"
IPV6_PRIVACY="no"
```



编辑 `ifcfg-ens32` 配置如下

vim ifcfg-ens32

```bash
NAME=ens32
DEVICE=ens32
BOOTPROTO=none
NM_CONTROLLED=no
ONBOOT=yes
BRIDGE=br0
```



查看网桥

```bash
brctl show

#例如输出如下
bridge name    bridge id        STP enabled    interfaces
br0        8000.000c29d1267b    no        ens32
virbr0        8000.52540063d8f4    yes        virbr0-nic
```



删除virbr0

```bash

brctl show

#例如输出如下
bridge name    bridge id        STP enabled    interfaces
br0        8000.000c29d1267b    no        ens32
virbr0        8000.52540063d8f4    yes        virbr0-nic
```
```bash
virsh net-list

#例如输出如下
 Name                 State      Autostart     Persistent
----------------------------------------------------------
 default              active     yes           yes
```
```bash
virsh net-destroy default

virsh net-undefine default

systemctl restart libvirtd.service
```
```bash
brctl show

#例如输出如下
bridge name    bridge id        STP enabled    interfaces
br0        8000.000c29d1267b    no        ens32
```



### 1.5 安装 virt-manager

```bash
yum -y install virt-manager
```



## 2. 在 KVM 中安装虚拟机

提前将下载好的 ISO 镜像文件放到 /data/iso 目录下，然后启动 KVM 管理工具，安装图形界面提示，进行安装虚机。

```bash
virt-manager
```



## 3. 常用指令

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

