# IoT DC3 Documentation

> 大部分文档已迁移到项目文档站。本目录仅保留 release 流程依赖的源文件。

## 在线文档

- 在线文档：<https://docs.dc3.site/>
- 文档站源文件仓库：[pnoker/iot-dc3-docs](https://github.com/pnoker/iot-dc3-docs)

## 仍保留在本目录的文件（被 CI 直接引用，请勿迁移）

| 文件                       | 用途                                                                              |
|----------------------------|-----------------------------------------------------------------------------------|
| [`TITLE.md`](./TITLE.md)   | release 标题与简介，由 `.github/workflows/docker-ci.yml` 拼入 GitHub Release body |
| [`CHANGE.md`](./CHANGE.md) | 版本变更日志，由 `dc3/bin/changelog.py` 写入，docker-ci 拼入 release body         |
| [`USAGE.md`](./USAGE.md)   | 镜像使用说明，docker-ci 拼入 release body                                         |

文档站里的 [变更日志](https://docs.dc3.site/zh/development/changelog)
与 [镜像与部署](https://docs.dc3.site/zh/guide/usage) 通过 VitePress 的
`<!--@include:-->` 语法直接引用上述源文件，保持单一信源。

## 已迁移到文档站的旧路径对照

| 旧路径                        | 新位置                                                            |
|-------------------------------|-------------------------------------------------------------------|
| `dc3/doc/QUICKSTART.md`       | [快速开始](https://docs.dc3.site/zh/quickstart/)                  |
| `dc3/doc/ENVIRONMENT.md`      | [环境变量](https://docs.dc3.site/zh/quickstart/environment)       |
| `dc3/doc/LOGGING.md`          | [日志规范](https://docs.dc3.site/zh/guide/logging)                |
| `dc3/doc/TROUBLESHOOTING.md`  | [故障排查](https://docs.dc3.site/zh/guide/troubleshooting)        |
| `dc3/doc/MODULES.md`          | [模块与依赖](https://docs.dc3.site/zh/architecture/modules)       |
| `dc3/doc/DRIVER-AUTHORING.md` | [驱动开发](https://docs.dc3.site/zh/development/driver-authoring) |
| `dc3/doc/TESTING.md`          | [测试](https://docs.dc3.site/zh/development/testing)              |

如果你是从外部链接跳转到此页面，请更新书签到对应的新地址。
