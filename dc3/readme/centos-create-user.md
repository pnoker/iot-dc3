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
```

## 回收权限

```bash
chmod -v u-w /etc/sudoers
```

> 到此就完成了用户 centos 的创建