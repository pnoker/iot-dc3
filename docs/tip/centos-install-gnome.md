## 查询 yum group

> yum 可以以程序组的模式来安装成套的软件包，可通过以下指令进行查询可支持的 group 安装包

```bash
yum group list
```



## 安装图形界面

> 安装 "GNOME Desktop" 环境

```bash
yum group install "GNOME Desktop"
```



## 启动桌面环境

> 手动启动，需要每次开机手动执行才能进入桌面环境

```bash
startx
```

> 设置默认进去桌面环境

```bash
# 获取启动状态
systemctl get-default

# 设置默认进去桌面环境
systemctl set-default graphical.target

# 恢复默认
systemctl set-default multi-user.target
```



## 卸载图形界面

> 卸载 "GNOME Desktop" 环境

```bash
yum group remove "GNOME Desktop"
```



