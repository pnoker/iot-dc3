### 1.功能

DC3权限中心，提供用户鉴权、设备鉴权以及接口鉴权功能。

![](../../dc3/images/dc3-auth.png)

#### 1.1 用户鉴权

- [x] 注册用户
- [x] 删除用户信息
- [x] 修改用户信息
- [x] 重置用户密码
- [x] 用户使能
- [x] 模糊查询
- [x] 分页查询
- [x] 多次登录错误限制登录，配合网关使用
- [x] 信息加密存储，基于 Md5 + Salt



#### 1.2 设备鉴权

- [ ] 设备注册
- [ ] 设备注销
- [ ] 读写接口权限控制
- [ ] 设备操作记录
- [ ] 设备身份ID管理




#### 1.3 接口鉴权

- [x] Ip 黑名单限制，配合网关使用
- [x] Ip新增
- [x] Ip删除
- [x] Ip修改
- [x] Ip使能
- [x] Jwt Token 验证
- [ ] 权限控制



### 2.依赖

- MySQL >= 5.7
- Redis >= 5.0
- Dc3-Register



### 3.接口

> 接口测试文件 dc3/api/dc3-auth.http



#### 3.1 生成用户 Salt

```http
### 生成用户 Salt
#/auth/token/salt
GET http://localhost:8300/auth/token/salt
Accept: */*
Content-Type: application/x-www-form-urlencoded
Cache-Control: no-cache

username=pnoker
```



#### 3.2 生成用户 Token 令牌

> 密码需要使用md5(md5(password)+salt)进行加密，salt 通过 3.1 接口获取

```http
### 生成用户 Token 令牌
#/auth/token/generate
POST http://localhost:8300/auth/token/generate
Accept: */*
Content-Type: application/json
Cache-Control: no-cache

{
  "name": "pnoker",
  "password": "dc3dc3dc3"
}
```



#### 3.3 检测用户 Token 令牌是否有效

```http
### 检测用户 Token 令牌是否有效
#/auth/token/:token
GET http://localhost:8300/auth/token/check
Accept: */*
Content-Type: application/x-www-form-urlencoded
Cache-Control: no-cache

username=pnoker&token=eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJwbm9rZXIiLCJpYXQiOjE1OTE0OTQ5MTQsImV4cCI6MTU5MTUxNjUxNH0.J2ND1WkIRKf68t3pPVj9iO-rAq5jl_1raTa4zPVfJnw
```



#### 3.4 注销用户 Token 令牌

```http
### 注销用户 Token 令牌
#/auth/token/cancel
GET http://localhost:8300/auth/token/cancel
Accept: */*
Content-Type: application/x-www-form-urlencoded
Cache-Control: no-cache

username=pnoker
```



#### 3.5 新增 User

```http
### 新增 User
#/auth/user/add
POST http://localhost:8300/auth/user/add
Accept: */*
Content-Type: application/json
Cache-Control: no-cache

{
  "name": "dc3",
  "password": "dc3dc3dc3",
  "description": "平台开发者账号"
}
```



#### 3.6 修改 User

```http
### 修改 User
#/auth/user/update
POST http://localhost:8300/auth/user/update
Accept: */*
Content-Type: application/json
Cache-Control: no-cache

{
  "id": 1,
  "password": "dc3dc3dc3",
  "enable": true,
  "description": "平台开发者账号"
}
```



#### 3.7 根据 ID 重置 User 密码

```http
### 根据 ID 重置 User 密码
#/auth/user/restPassword/:id
POST http://localhost:8300/auth/user/restPassword/-1
Accept: */*
Content-Type: application/json
Cache-Control: no-cache
```



#### 3.8 根据 ID 查询 User

```http
### 根据 ID 查询 User
#/auth/user/id/:id
GET http://localhost:8300/auth/user/id/-1
Accept: */*
Cache-Control: no-cache
```



#### 3.9 根据 Name 查询 User

```http
### 根据 Name 查询 User
#/auth/user/name/:name
GET http://localhost:8300/auth/user/name/pnoker
Accept: */*
Cache-Control: no-cache
```



#### 3.10 分页查询 User

```http
### 分页查询 User
#/auth/user/list，支持name模糊查询
POST http://localhost:8300/auth/user/list
Accept: */*
Content-Type: application/json
Cache-Control: no-cache

{
  "name": "",
  "page": {
    "current": 1,
    "size": 500,
    "orders": [
      {
        "column": "id",
        "asc": false
      }
    ]
  }
}
```



#### 3.11 检测用户是否存在

```http
### 检测用户是否存在
#/auth/user/check/:name
GET http://localhost:8300/auth/user/check/pnoker
Accept: */*
Cache-Control: no-cache
```



#### 3.12 根据 ID 删除 User

```http
### 根据 ID 删除 User
#/auth/user/delete/:id
POST http://localhost:8300/auth/user/delete/1
Accept: */*
Content-Type: application/json
Cache-Control: no-cache
```



#### 3.13 新增 BlackIp

```http
### 新增 BlackIp
#/auth/blackIp/add
POST http://localhost:8300/auth/blackIp/add
Accept: */*
Content-Type: application/json
Cache-Control: no-cache

{
  "ip": "localhost",
  "description": "测试黑名单Ip"
}
```



#### 3.14 修改 BlackIp

```http
### 修改 BlackIp
#/auth/blackIp/update
POST http://localhost:8300/auth/blackIp/update
Accept: */*
Content-Type: application/json
Cache-Control: no-cache

{
  "id": 1,
  "ip": "192.168.0.2",
  "description": "测试黑名单Ip"
}
```



#### 3.15 根据 ID 查询 BlackIp

```http
### 根据 ID 查询 BlackIp
#/auth/blackIp/id/:id
GET http://localhost:8300/auth/blackIp/id/1
Accept: */*
Cache-Control: no-cache
```



#### 3.16 分页查询 BlackIp

```http
### 分页查询 BlackIp
#/auth/blackIp/list，支持ip模糊查询
POST http://localhost:8300/auth/blackIp/list
Accept: */*
Content-Type: application/json
Cache-Control: no-cache

{
  "ip": ".2",
  "page": {
    "current": 1,
    "size": 500,
    "orders": [
      {
        "column": "id",
        "asc": false
      }
    ]
  }
}
```



#### 3.17 检测BlackIp是否存在

```http
### 检测BlackIp是否存在
#/auth/blackIp/check/:ip
GET http://localhost:8300/auth/blackIp/check/192.168.0.1
Accept: */*
Cache-Control: no-cache
```



#### 3.18 根据 ID 删除 BlackIp

```http
### 根据 ID 删除 BlackIp
#/auth/blackIp/delete/:id
POST http://localhost:8300/auth/blackIp/delete/3
Accept: */*
Content-Type: application/json
Cache-Control: no-cache
```



### 计划

#### 1.生成用户 Token 令牌

- [x] 修改接口名称，调整为 /auth/token/generate
- [x] 优化：接口请求限制
- [x] 优化：Token 令牌失效逻辑（最新Token之前的历史Token均失效）
- [x] 优化：用户名和密码使用密文传输，md5+随机salt，获取salt需要使用用户名进行请求
- [x] 新增：新增一个生成随机salt的接口
- [x] 优化：用户密码错误次数限制，连续3次错误，需要5分钟之后尝试
- [x] 优化：用户密码验证需要记录远程IP，同一个IP累计超过15次错误，需要拉黑IP
- [x] 新增：提供IP黑名单管理



#### 2.检测用户 Token 令牌是否有效

- [x] 修改接口，调整为GET请求，/auth/token/check
- [x] 优化：Token存入redis，key=user，value=token，先从redis中检测token然后使用jwt验证token有效性
- [x] 优化：Token支持注销，仅保持最新Token的有效性，使用redis进行控制
- [x] 网关的所有请求需要携带token，并通过auth进行校验
- [x] 优化：验证Token的时候需要记录远程IP，网关实现并记录


