## Dc3 Modbus Tcp Driver 说明

## 功能列表

- [x] 连接 Modbus Slave
- [x] 读 Modbus Slave 点位
- [x] 定时读 Modbus Slave 点位
- [x] 写 Modbus Slave 数据
- [ ] 定时写 OpcDa 点位
- [x] 跨平台
- [x] 支持数据类型： int \ long \ string \ double \ float \ boolean



## 测试工具

- Modbus Slave v6.0.2



## 配置项

> src/main/resources/application.yml

###  OpcDa Server 连接配置

- Host ：Modbus  Tcp/Ip Server Ip
- Port ：Modbus  Tcp/Ip Server Port ，默认是 `502`

###  OpcDa Point 配置

- slaveId：从站编号，例如：1、2、3...
- functionCode：功能编码，分别是：
  - 1（Coil Status 0X）
  - 2（Input Status 1X）
  - 3（Holding Register 4X）
  - 4（Input Register 3X）
- offset：偏移量，例如：0、1、2...

### 定时采集配置

```yaml
schedule:
    read:
      enable: true
      corn: '0/30 * * * * ?'
```



## 接口测试

> dc3/api/dc3-driver-modbus-tcp.http

