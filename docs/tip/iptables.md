## 屏蔽某个 IP 

```bash
sudo iptables -A OUTPUT -d 192.168.1.100 -j REJECT
```



## 查询屏蔽的 IP 配置

```bash
sudo iptables -L -n --line-number | grep 192.168.1.100

# 输出
1    REJECT     all  --  0.0.0.0/0            192.168.1.100        reject-with icmp-port-unreachable
```



## 根据 Number 删除屏蔽 IP 配置

```bash
sudo iptables -D OUTPUT 1
```



## 添加 IP 转发

```bash
sudo iptables -t nat -A OUTPUT -d 192.168.1.101 -j DNAT --to-destination 192.168.1.100
```



## 查看 IP 转发

```bash
sudo iptables -t nat -L --line-number | grep 192.168.1.101
```



## 删除 IP 转发

```bash
sudo iptables -t nat -D OUTPUT 2
```
