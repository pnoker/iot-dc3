## Dc3 Opc Da Driver 说明

## 功能列表

- [x] 连接 OpcDa 服务
- [x] 读 OpcDa 点位
- [x] 定时读 OpcDa 点位
- [x] 写 OpcDa 数据
- [ ] 定时写 OpcDa 点位
- [x] 跨平台
- [x] 支持 OpcDa 2.0
- [ ] 支持 OpcDa 3.0
- [x] 支持数据类型： int \ long \ string \ double \ float \ boolean



## 测试工具

- Matrikon Opc Simulation



## 配置项

> src/main/resources/application.yml

###  OpcDa Server 连接配置

- Host ：OpcDa 服务所在主机IP
- CLSID ：OpcDa 服务CLSID，可以通过OpcClient工具查看
- User ：OpcDa 服务所在主机用户名
- Password ：OpcDa 服务所在主机用户的远程登录密码

###  OpcDa Point 配置

- group ：item group
- tag ：item name

### 定时采集配置

```yaml
schedule:
    read:
      enable: true
      corn: '0/30 * * * * ?'
```



## 接口测试

> dc3/api/dc3-driver-opcda.http

