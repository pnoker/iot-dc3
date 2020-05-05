## Dc3 Opc Ua Driver 说明

## 功能列表

- [x] 连接 OpcUa 服务
- [x] 读 OpcUa 点位
- [x] 定时读 OpcUa 点位
- [x] 写 OpcUa 数据
- [ ] 定时写 OpcUa 点位
- [x] 跨平台
- [x] 支持数据类型： int \ long \ string \ double \ float \ boolean
- [x] 无权限认证方式
- [ ] 其他权限认证方式



## 测试工具

- Prosys OPC UA Simulation



## 配置项

> src/main/resources/application.yml

###  OpcUa Server 连接配置

- Host ：OpcUa 服务所在主机IP
- Port ：OpcUa 服务所在主机Port
- Path ：OpcUa 服务Path

###  OpcUa Point 配置

- namespace ：namespace index
- tag ：item name

### 定时采集配置

```yaml
schedule:
    read:
      enable: true
      corn: '0/30 * * * * ?'
```



## 接口测试

> dc3/api/dc3-driver-opcua.http

