## 只输出错误信息到日志文件

```bash
nohup java -jar dc3.jar >/dev/null 2>dc3/logs/dc3.log &
```



## 不输出任何信息

```bash
nohup java -jar dc3.jar >/dev/null 2>&1 &
```



## Linux 中重定向

- 0:标准输入
- 1:标准输出，默认为标准输出
- 2:错误信息输出



## 说明

- `2>log` 可以将错误信息输出到log文件中
- `0,1,2` 之间可以实现重定向，`2>&1` 可以将错误信息重定向到标准输出中
- `/dev/null` Linux下特殊文件，所有到这儿的信息都会消失，系统不在任何保留