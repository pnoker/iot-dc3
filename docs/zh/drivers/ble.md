---
title: BLE 驱动
---

# BLE 驱动

`dc3-driver-ble` 把蓝牙低功耗（BLE）设备接入 IoT DC3。它作为 BLE 主机（central），通过主机上的蓝牙适配器连到外设，周期性读取
GATT 特征的字节、按配置格式解析成[位号值](../introduction/concepts/point-value)
，并支持把命令值写回特征。读完你能在[设备](../introduction/concepts/device)上配好 `adapterName`/`deviceAddress`
、在[位号](../introduction/concepts/point)上配好服务/特征 UUID 与解析格式，并定位常见的"设备一直离线/读不到值"问题。

## 协议背景

BLE（Bluetooth Low Energy，蓝牙低功耗）是可穿戴、环境传感器、信标、便携仪表上最常见的近场无线协议——典型十米级覆盖、低速率、极省电，靠纽扣电池可跑数月到数年。在物联网四层参考架构里，BLE
属于[网络层](../foundations/iot-protocols)中的**无线接入**一侧：它只负责"信号怎么在空中传"，本身不规定上层消息怎么组织，因此
BLE 设备通常需要一个主机或网关做中继才能把数据送上互联网——本驱动就扮演这个 central 主机角色。

BLE 设备把数据组织成 **GATT**（Generic Attribute Profile）树状结构：一个外设（peripheral）包含若干 **Service（服务）**，每个服务下挂若干
**Characteristic（特征）**，每个特征用一个 **UUID** 唯一标识，特征值就是一段原始字节。主机要读一个数据点，就向"服务 UUID + 特征
UUID"定位到的特征发起 read；要写，就把字节 write 进目标特征。本驱动据此把每个[位号](../introduction/concepts/point)
映射到外设上的一个特征，按位号配置的 UUID 读字节、写字节，再按配置的格式把字节解析成位号值。

::: info GATT 决定"怎么寻址一个数据点"
Modbus 用"功能码 + 寄存器地址"寻址，BLE 用"服务 UUID + 特征 UUID"寻址——两者都是"先连到设备、再定位到设备内部的一个数据点"
。理解这层映射，配置时就不会把 Service 和 Characteristic 搞混。
:::

底层传输上，本驱动用 Sputnikdev Bluetooth Manager 框架搭配 TinyB 传输，由它在运行主机的物理蓝牙适配器（默认 `hci0`
）之上完成扫描、连接与 GATT 读写。

## 属性配置

接入一台 BLE 设备，需要在三个层面填[属性](../introduction/concepts/attribute-config)：设备级的连接参数（`driver-attribute`
）、每个采集位号的寻址与解析参数（`point-attribute`）、每个可写位号的写命令参数（`command-attribute`）。下面各属性、类型、默认值均取自驱动的
`application.yml`（`dc3-driver-ble` 模块）。

### 驱动属性（`driver-attribute`）

驱动属性回答"用哪个适配器连到哪台外设"。`adapterName` 指定运行主机上的蓝牙适配器名（Linux 下通常是 `hci0`、`hci1`）；
`deviceAddress` 是外设的 MAC 地址，作为外设的唯一标识。`connectionTimeout` 虽在 `application.yml`
里声明，但当前驱动代码并未读取它（见下方提示）。在[设备](../introduction/concepts/device)上为每台 BLE 设备填一组：

| 属性                 | code                | 类型     | 默认值     | 说明                                   |
|--------------------|---------------------|--------|---------|--------------------------------------|
| Adapter Name       | `adapterName`       | STRING | `hci0`  | 主机蓝牙适配器名                             |
| Device Address     | `deviceAddress`     | STRING | （空）     | BLE 设备 MAC 地址（如 `AA:BB:CC:DD:EE:FF`） |
| Connection Timeout | `connectionTimeout` | INT    | `10000` | 连接超时（毫秒）；**当前实现未读取**，见下方提示           |

::: info `connectionTimeout` 当前未生效
`connectionTimeout` 在 `application.yml` 中声明、可在设备上填写，但 `dc3-driver-ble` 代码从未读取该值——建链由
`bluetoothManager.getCharacteristicGovernor(charUrl, true)` 等待 governor ready 完成，不传任何超时参数。改这个值不会影响连接行为，它是预留属性。
:::

::: tip 一个外设 = 一个设备
`deviceAddress` 是外设的唯一标识，一台 BLE 外设对应平台里一个[设备](../introduction/concepts/device)。同一适配器（`hci0`
）可同时连多台外设，由各设备的 `deviceAddress` 区分；驱动按 `deviceId` 缓存每台外设的连接控制器（governor），首次读写时建链。
:::

### 位号属性（`point-attribute`）

位号属性回答"读这台外设的哪一个特征、读回来的字节怎么解析"。每个采集[位号](../introduction/concepts/point)填一组：

| 属性                  | code                 | 类型     | 默认值    | 说明                                |
|---------------------|----------------------|--------|--------|-----------------------------------|
| Service UUID        | `serviceUuid`        | STRING | （空）    | GATT Service UUID                 |
| Characteristic UUID | `characteristicUuid` | STRING | （空）    | GATT Characteristic UUID          |
| Read Format         | `readFormat`         | STRING | `UTF8` | 数据格式（UTF8、HEX、INT16、UINT16、FLOAT） |
| Byte Order          | `byteOrder`          | STRING | `BIG`  | 字节序（BIG、LITTLE）                   |

`serviceUuid` + `characteristicUuid` 共同定位到外设上的一个特征。特征读回来的是一段原始字节，驱动按 `readFormat` 解析：
`UTF8` 当字符串（默认）、`HEX` 转十六进制串、`INT16`/`UINT16`/`FLOAT` 按数值解析。`INT16`/`UINT16`/`FLOAT` 这三种数值格式还受
`byteOrder` 影响（`BIG` 大端、`LITTLE` 小端）；`UTF8`、`HEX` 与字节序无关。

::: tip 解析格式按外设手册定
读哪种 `readFormat`、用哪种 `byteOrder`，取决于外设固件在特征里实际放的是什么——例如某温度计把温度以小端 4 字节浮点写在某特征里，就配
`readFormat=FLOAT`、`byteOrder=LITTLE`。配错格式不会报错，只会把字节解析成无意义的值，所以先查外设的 GATT 规格再填。
:::

### 写命令属性（`command-attribute`）

`application.yml` 在 `command-attribute` 下声明了 `serviceUuid`/`characteristicUuid`，但**写入路径并不读取它们**：

| 属性                  | code                 | 类型     | 默认值 | 说明                        |
|---------------------|----------------------|--------|-----|---------------------------|
| Service UUID        | `serviceUuid`        | STRING | （空） | GATT Service UUID（当前未被消费） |
| Characteristic UUID | `characteristicUuid` | STRING | （空） | 写入目标特征的 UUID（当前未被消费）      |

::: warning 写入复用位号上的 UUID，命令属性当前不生效
BLE 的 `write()` 与 `read()` 一样从位号属性（`point-attribute`）读取 `serviceUuid`/`characteristicUuid`，写哪个特征由位号决定。驱动并未覆写
`execute()`，而 `command-attribute` 只在 `execute()` 路径才会被消费，因此上表声明的写命令属性是占位配置、当前写路径不读取。*
*可写位号无需在写命令上重复填 UUID**，把它配在位号上即可。
:::

::: warning 写入按 UTF-8 字节下发，不做格式转换
读路径有 `readFormat`/`byteOrder` 把字节解析成值，但写路径没有对称的逆变换——驱动把命令值按 UTF-8 编码成字节直接 write
进特征。要写数值或十六进制，必须在上层把它表达成目标特征接受的字符串形式（驱动不会替你把 `25.5` 转成小端浮点字节）。
:::

### 采集与健康

- **采集周期**：默认 cron `0/30 * * * * ?`，每 30 秒读一轮全部位号。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`。设备在线 = BLE 链路已连接且可达（驱动判定
  `governor.isOnline() && governor.isConnected()`）；驱动整体在线 =
  蓝牙管理器已初始化。在线状态机制见[设备](../introduction/concepts/device)。

::: info `application.yml` 里的 `custom` schedule 当前为空实现
yml 中虽配了一条 `custom` cron（`0/5 * * * * ?`），但驱动的 `schedule()` 方法体为空，不执行任何自定义逻辑。实际只有 `read`（
`0/30`）和 `health`（`0/15`）两条调度生效。
:::

## 故障排查

1. **设备一直离线（最常见）**。运行主机没有可用的蓝牙适配器、或 `adapterName` 填错（默认 `hci0`），`governor.isOnline()`
   永远为假，设备一直显示离线。先在主机上用 `hciconfig`/`bluetoothctl` 确认适配器存在且 up，再核对 `adapterName`。
2. **容器里连不上蓝牙**。本驱动依赖运行主机的**物理**蓝牙适配器和 TinyB 本地库。容器化部署时若没把宿主蓝牙能力透传进容器（如未挂载
   D-Bus/适配器、未给特权），驱动初始化虽不报错（`withIgnoreTransportInitErrors(true)` 会吞掉传输初始化错误），但设备始终连不上。
3. **特征找不到、读写失败**。`serviceUuid`/`characteristicUuid` 要和外设实际暴露的 GATT 逐字一致（含短/长格式、大小写）。UUID
   写错时定位不到特征，读会抛 `ReadPointException`（被驱动捕获为读取失败）、写抛 `WritePointException`。先用 BLE 扫描工具（
   `bluetoothctl`、nRF Connect 等）确认外设的 service/characteristic UUID 再填。
4. **读到的值像乱码或数值离谱**。多半是 `readFormat`/`byteOrder` 与外设实际编码不符——例如外设用小端浮点而你配了 `UTF8`
   ，或字节序配反。对照外设 GATT 规格调整这两项；读空字节（`data.length == 0`）时驱动返回 `null`，不会落库。
5. **连接超时/连不上**。当前驱动不使用 `connectionTimeout`（见上文提示），调大该值无效；连接失败只与外设信号弱、距离远、正被别的主机占用连接有关。BLE
   同一时刻通常只允许一个 central 连一个外设，确认没有手机 App 或其他网关抢占连接。
6. **写命令"没生效"**。命令值不是目标特征接受的字符串形式（见上文写入语义），或该特征不可写。先确认特征的 GATT 属性含
   Write，再确认上层下发的字符串与设备约定一致。

## 在 IoT DC3 中如何落地

- **`dc3.driver.code`**：`BleDriver`（驱动名 `Bluetooth LE Driver`，类型 `DRIVER_CLIENT`，由驱动主动连外设）。这是稳定的路由标识，不应随意更改。
- **能力**：读 ✓、写 ✓、订阅 —。与[驱动能力矩阵](./matrix)一致——BLE 为请求-响应式主动读写，驱动周期性 read 特征、命令式 write
  特征，不订阅 GATT notify/indication 被动上报。
- **实现状态**：可用。`read()`/`write()`/`initial()`/`health()` 均为完整实现，基于 Sputnikdev Bluetooth Manager + TinyB
  完成连接与 GATT 读写。

::: warning 落地前提：主机有可用蓝牙适配器 + TinyB 本地库
代码已就绪，但能否跑通取决于部署环境：运行主机必须有物理蓝牙适配器（默认 `hci0`）和 TinyB
本地库。这是纯软件之外的硬件/环境前提，缺一不可——详见上文故障排查第 1、2 条。
:::

### 最小接入示例

把 MAC `AA:BB:CC:DD:EE:FF` 的一台 BLE 温度计接进来：

1. 选 `Bluetooth LE Driver` 创建[设备](../introduction/concepts/device)，driver 属性填 `adapterName=hci0`、
   `deviceAddress=AA:BB:CC:DD:EE:FF`（`connectionTimeout` 当前未被读取，留默认即可）。
2. 给设备绑定的[模板](../introduction/concepts/profile)加一个温度[位号](../introduction/concepts/point)（`READ_ONLY`
   ），point 属性填 `serviceUuid`、`characteristicUuid` 为该温度特征的 UUID，`readFormat=FLOAT`、`byteOrder=LITTLE`（按外设手册定）。
3. 启动驱动，30 秒内就能在[位号值](../introduction/concepts/point-value)里看到采集值。
4. 若该位号需可写，给它配写[命令](../introduction/concepts/command)即可——写入复用该位号上已配的 `serviceUuid`/
   `characteristicUuid`，无需在命令上重复配置；只需在上层把命令值表达成特征接受的字符串。

## 延伸阅读

- [驱动总览](./index) — 全部 28 个驱动的分组与选型
- [驱动能力矩阵](./matrix) — 各驱动读/写/订阅能力一览
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [IoT 协议与无线网络](../foundations/iot-protocols) — BLE 在网络层无线接入侧的定位与权衡
