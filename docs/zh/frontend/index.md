---
title: 前端开发
---

# 前端开发

IoT DC3 前端基于 **Vue 3 + TypeScript + Vite + Element Plus** 构建，源码位于仓库的 `dc3-web/` 目录。

## 环境准备

| 工具      | 最低版本   | 说明                               |
|---------|--------|----------------------------------|
| Node.js | 20 LTS | 推荐使用 fnm/nvm 管理版本                |
| pnpm    | 9+     | 包管理器，项目 `packageManager` 字段已锁定版本 |

```bash
# 安装 pnpm（如未安装）
corepack enable && corepack prepare pnpm@latest --activate

# 确认版本
node -v   # ≥ v20
pnpm -v   # ≥ 9
```

## 快速启动

```bash
# 1. 进入前端项目目录
cd dc3-web

# 2. 安装依赖
pnpm install

# 3. 启动开发服务器（默认 http://localhost:8080）
pnpm dev
```

开发服务器启动后：

- 前端页面：`http://localhost:8080`
- 默认代理后端 API 到 `http://localhost:8000`（网关端口）
- 修改后端地址：编辑 `vite.config.ts` 中的 proxy 配置

::: tip 后端依赖
前端开发需要后端服务运行。至少需要起网关 `dc3-gateway`（端口 8000）。推荐用 docker-compose 起全栈：

```bash
# 在仓库根目录
make up-dev    # 起网关 + 4 个中心 + 常用驱动
```

:::

## 项目结构

```
dc3-web/
├── src/
│   ├── api/           # REST API 请求封装
│   ├── components/    # 可复用组件
│   │   ├── card/      #   InfoCard 模式（单实体表单 + 保存/重置）
│   │   ├── chart/     #   图表组件（AntV G2/G6）
│   │   ├── entity/    #   实体详情/列表组件
│   │   ├── layout/    #   布局、菜单、导航栏
│   │   └── agentic/   #   AI 对话组件
│   ├── composables/   # Vue Composables
│   ├── config/        # 应用配置
│   │   ├── axios/     #   Axios 实例与拦截器
│   │   ├── i18n/      #   国际化（zh / en）
│   │   ├── router/    #   路由定义
│   │   └── types/     #   实体类型定义
│   ├── store/         # Pinia 状态管理
│   ├── styles/        # 全局样式
│   ├── utils/         # 工具函数
│   └── views/         # 页面组件
│       ├── device/    #   设备管理
│       ├── driver/    #   驱动管理
│       ├── home/      #   仪表盘
│       ├── login/     #   登录
│       ├── point/     #   位号管理
│       ├── profile/   #   模板管理
│       └── settings/  #   系统设置
├── tests/             # 测试（Vitest + Playwright）
├── vite.config.ts     # Vite 配置
├── tsconfig.json      # TypeScript 配置
└── package.json       # 依赖与脚本
```

## 菜单系统

前端菜单由两层控制：

1. **后端数据库** `dc3_menu` 表 — 存储菜单项的定义和权限
2. **前端路由配置** `src/config/router/` — 将菜单映射到 Vue 页面组件

新增一个菜单项的完整链路：

```
dc3_menu 表写入 → 前端路由注册 → i18n 翻译 → 权限点绑定
```

四层必须全部更新，缺一不可。详见 [贡献指南](../community/contributing)。

## 常用命令

| 命令                | 说明                    |
|-------------------|-----------------------|
| `pnpm dev`        | 启动开发服务器               |
| `pnpm build`      | 生产构建                  |
| `pnpm preview`    | 预览生产构建                |
| `pnpm test`       | 运行单元测试                |
| `pnpm test:e2e`   | 运行 E2E 测试（Playwright） |
| `pnpm lint`       | ESLint 检查             |
| `pnpm type-check` | TypeScript 类型检查       |

## 测试

项目包含三层测试：

- **单元测试**（Vitest）：`tests/unit/` 和 `tests/component/`
- **API 契约测试**：`tests/api/` — 快照测试确保 API 封装接口不变
- **E2E 测试**（Playwright）：`tests/e2e/` — 浏览器端到端测试

CI 门禁：`pnpm lint && pnpm type-check && pnpm test && pnpm build`

详见 [测试调试指南](./test-debugging)。

## 环境变量

前端环境变量在 `src/config/env/` 下按模式组织：

```typescript
// .env.development
VITE_API_BASE_URL=http://localhost:8000
VITE_APP_TITLE=IoT DC3 (Dev)
```

修改后端地址后需重启 dev server。
