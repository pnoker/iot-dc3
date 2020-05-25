## Dc3 Mqtt Driver 说明

## 功能列表

- [x] 接收上行 Mqtt 数据
- [x] 下发 Mqtt 数据
- [ ] 定时下发 Mqtt  点位
- [x] Qos
- [x] 模糊匹配, *（星号）表示一个单词, #（井号）表示零个或者多个单词
- [ ] 上行数据，动态添加主题
- [x] 下行指令，动态指定主题
- [ ] Virtual Host
- [x] 跨平台
- [x] 支持数据类型： string
- [x] 用户&密码认证方式
- [ ] 其他权限认证方式



> **什么是 Qos ？**
>
> **0**：最多一次的传输，仅发一次包，是否收到完全不管，适合那些不是很重要的数据；
>
> **1**：至少一次的传输，(鸡肋)，当client没收到service的puback或者service没有收到client的puback，那么就会一直发送publisher；
>
> **2**： 只有一次的传输，publisher和broker进行了缓存，其中publisher缓存了message和msgID，而broker缓存了msgID，两方都做记录所以可以保证消息不重复。



## 测试工具

- [Mqtt.fx](dhttp://mqttfx.jensd.de/index.php/download)
- Rabbitmq



## 配置项

> src/main/resources/application.yml

###  Mqtt 连接配置

- Username：Mqtt 服务用户名
- Password：Mqtt 服务密码
- Url：Mqtt 服务连接地址
- Qos：list集合，同 Topics 个数对应
- Topics：list集合，需要开启的订阅上行 Mqtt 主题
- Client.Id：Mqtt Client ID，用于标识本身  
- Default.Topic：默认主题
- Default.Qos：默认主题 Qos
- Default.Receive.Enable：是否开启接收默认主题上行Mqtt数据
- KeepAlive：保活时间间隔（秒）
- CompletionTimeout：超时设置（秒）



###  Mqtt Point 配置

- commandTopic：指令下发到设备的 Topic
- commandQos：指令下发到设备的 Topic 的 Qos，默认是 2



## 接口测试

> dc3/api/dc3-driver-mqtt.http

