### 安装 VNC Server

> ```bash
> yum install tigervnc-server
> ```



### 配置 VNC Server 实例



### root 用户

> ```bash
> cp /lib/systemd/system/vncserver@.service /etc/systemd/system/vncserver@:1.service
> ```
>
> 修改拷贝之后的配置文件 `vncserver@:1.service` 中 [Service] 部分：
>
> ```bash
> # /etc/systemd/system/vncserver@:1.service
> 
> [Service]
> Type=forking
> User=root
> # Clean any existing files in /tmp/.X11-unix environment
> ExecStartPre=-/usr/bin/vncserver -kill %i
> ExecStart=/sbin/runuser -l root -c "/usr/bin/vncserver %i"
> PIDFile=/root/.vnc/%H%i.pid
> ExecStop=-/usr/bin/vncserver -kill %i
> ```



### 其他用户，例如：pnoker

> ```bash
> cp /lib/systemd/system/vncserver@.service /etc/systemd/system/vncserver@:2.service
> ```
>
> 修改拷贝之后的配置文件 `vncserver@:2.service` 中 [Service] 部分，其中普通用户的ExecStart不同于root，加/sbin/runuser则会在启动服务时报以下错误：
>
> ```bash
> # /etc/systemd/system/vncserver@:1.service
> 
> [Service]
> Type=forking
> User=pnoker
> # Clean any existing files in /tmp/.X11-unix environment
> ExecStartPre=-/usr/bin/vncserver -kill %i
> ExecStart=/usr/bin/vncserver %i
> PIDFile=/root/pnoker/.vnc/%H%i.pid
> ExecStop=-/usr/bin/vncserver -kill %i
> ```





### 设置 VNC 密码

> ```bash
> vncpasswd    #root用户实例的vnc密码
> su - pnoker
> vncpasswd    #普通用户一定要切换到用户自己的环境下
> ```
>
> 密码设置完成后回到root权限下，启动服务



### 启动服务

> ```bash
> systemctl daemon-reload
> systemctl start vncserver@:1.service
> systemctl enable vncserver@:1.service 
> 
> #如果配置了其他用户还需要执行
> systemctl start vncserver@:2.service
> systemctl enable vncserver@:2.service 
> ```



## 防火墙设置

> ```bash
> firewall-cmd --zone=public --add-port=5901/tcp
> firewall-cmd --zone=public --add-port=5902/tcp
> firewall-cmd --reload
> ```



## 远程连接

在其他电脑上安装 VNV Viewer 然后通过 主机地址，用户名，密码即可远程改Centos7系统。

[VNV Viewer 下载地址](https://www.realvnc.com/en/connect/download/viewer/)