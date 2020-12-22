## 创建新用户

```bash
adduser centos
```



## 为新用户初始化密码

```bash
passwd centos
```



## 授权sudoers

```bash
whereis sudoers
ls -l /etc/sudoers
chmod -v u+w /etc/sudoers
```



## 修改&编辑sudoers

```bash
vim /etc/sudoers
# 找到一下这个地方添加一条即可 centos centos  ALL=(ALL)       ALL
## Allow root to run any commands anywhere 
root    ALL=(ALL)       ALL
centos  ALL=(ALL)       ALL
#centos  ALL=(ALL)       NOPASSWD:ALL
```



## 回收权限

```bash
chmod -v u-w /etc/sudoers
```
