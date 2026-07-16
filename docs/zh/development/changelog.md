---
title: 变更日志
---

<script setup>
import ChangelogDiagram from '../../.vitepress/theme/components/ChangelogDiagram.vue'
</script>


# 变更日志

下面这份变更日志不是手写的——它由 `make changelog` 从 git 提交历史按 Conventional Commits
规则自动归类生成。这页先讲清它是怎么来的、怎么按版本读、以及版本号和 tag 的规则，然后内联完整清单。

> 你在这里：想了解某个版本改了什么，或想知道这份清单如何维护。写代码请先看 [开发概览与规范](./)
> ，提交规范见 [贡献指南](../community/contributing)。

## 这份清单从哪来

平台不维护手写的 `CHANGELOG`。所有改动都通过规范化的提交信息留痕，发布时由脚本扫描 git
历史、解析每条提交的类型与作用域，再聚合成下面这份按版本分组的清单。换句话说，**提交信息就是变更日志的原始数据**——一条含糊的
`update` 或 `fix bug` 会变成一行没有价值的发布说明，所以提交规范本身就是这份文档质量的前提。

生成器是 `dc3/bin/changelog.py`（Python，无第三方依赖），由 Makefile 目标 `make changelog` 驱动，产出文件为
`dc3/doc/CHANGE.md`——也就是本页底部内联的那份。整条生成链路如下：

<ChangelogDiagram lang="zh" />

链路是单向的：提交历史是唯一原始数据，脚本聚合产物落在 `CHANGE.md`，本页只是把它内联展示——所以**不要在本页手工编辑条目**
，改动会在下次 `make changelog` 时被覆盖。

::: code-group

```bash [默认（上一个发布 tag → HEAD）]
# 在 iot-dc3/ 目录下执行
make changelog
```

```bash [指定范围与版本号]
# FROM/TO 接受任意 git ref（tag、分支、commit）；VERSION 写入分组标题
make changelog FROM=dc3.release.20251005.00 TO=HEAD VERSION=2026.5.17
```

:::

不传参数时，生成器会自动找到上一个匹配 `dc3.release.*` 的 tag 作为起点、`HEAD` 作为终点，并从 `pom.xml` 的 `dc3.version`
读取版本号。生成结果会覆盖写回 `dc3/doc/CHANGE.md`，再由本页的 include 指令内联展示。

::: info 改了它要单独提交
变更日志是从历史生成的产物。当 `CHANGE.md` 本身被重新生成需要提交时，使用约定的提交信息
`docs(release): update generated changelog`——这是仓库为"仅变更日志"保留的固定 subject。生成器会识别并跳过这类提交（
`docs(release):` 与 `chore(release):` 两种前缀均匹配），避免变更日志里出现"更新变更日志"的噪声条目。
:::

## 怎么读：按版本，再按类别

清单的最外层按**版本**分组，每个版本一个 `### <版本号>` 标题，下面一行 `_Generated on <日期>._` 标明该段生成时间，再往下是一段
Summary（覆盖的提交数、各类别计数、最活跃的作用域、若干 Highlights），最后才是按类别展开的逐条提交。

每个版本段内的类别顺序是固定的，从最该被注意的到最琐碎的：

| 顺序 | 类别               | 来源提交类型                       |
|----|------------------|------------------------------|
| 1  | Breaking Changes | 任意类型带 `!`（如 `feat!:`）        |
| 2  | Security         | `security`                   |
| 3  | Features         | `feat` / `feature`           |
| 4  | Bug Fixes        | `fix`                        |
| 5  | Performance      | `perf`                       |
| 6  | Refactoring      | `refactor`                   |
| 7  | Documentation    | `docs` / `doc`               |
| 8  | Build            | `build`                      |
| 9  | CI               | `ci`                         |
| 10 | Tests            | `test` / `tests`             |
| 11 | Chores           | `chore` / `style` / `revert` |
| 12 | Other Changes    | 不符合 Conventional Commits 的提交 |

::: info Security 类别还会按关键词提升
上表是 type → 类别的映射。除此之外，生成器对**任意**提交，只要其类型、作用域或摘要里出现 `security` / `vulnerability` /
`cve` / `auth bypass` 关键词（不区分大小写），也会把它提升到 Security 类别——即便它的提交类型并非 `security`。这样安全相关改动不会因为被提成
`fix`/`refactor` 而埋没在普通类别里。
:::

每条目形如 `**<scope>**: <summary> (<short-hash>)`，scope 来自提交信息里的 `(<scope>)`，括号里的短哈希指向具体
commit。想读最新一版改了什么，跳到清单顶部的第一个 `###` 段即可；想对比两版之间的差异，看两个版本段的 Summary 行的提交计数与
Highlights 最快。

::: tip 提交规范决定输出质量
解析规则是 `<type>(<scope>): <英文祈使句摘要>`。type 必须是约定集合之一（`feat`/`fix`/`perf`/`refactor`/`docs`/`build`/
`ci`/`test`/`chore`/`style`/`security`/`revert`），否则该提交会落入 Other Changes
而不带类别。完整规范见 [贡献指南](../community/contributing)。
:::

## 版本号与 tag 规则

清单里的版本号对应 git tag，由 `make tag`（`dc3/bin/tag.sh`）生成，格式是 `dc3.<type>.<YYYYMMDD>.<NN>`：

- `<type>` 由当前分支推断——`develop` 分支打 `develop` tag，`release` / `main` 分支打 `release` tag；其它分支不允许打 tag。
- `<YYYYMMDD>` 是当天日期。
- `<NN>` 是当天该类型已有 tag 数量的两位补零序号，从 `00` 起。所以同一天第一个发布 tag 是 `dc3.release.20260622.00`，第二个是
  `dc3.release.20260622.01`（示例值）。

```bash
# 在 iot-dc3/ 目录下，release 分支上
make tag
# → 生成形如 dc3.release.20260622.00 的 tag 并 push 到 origin
```

`make changelog` 默认就是以上一个 `dc3.release.*` tag 为起点扫描到 `HEAD`，所以正常发布流程是：先 `make tag` 打出新 tag，再
`make changelog` 生成这一段的变更，提交回 `CHANGE.md`。

::: warning tag 会推送到远端
`make tag` 末尾会执行 `git push origin --tags`——这是对外操作，确认在正确分支、当天序号无误后再执行。
:::

## 完整变更清单

以下内容由 `dc3/doc/CHANGE.md` 内联，每次 `make changelog` 后随之更新；不要在本页手工编辑条目，改动会在下次生成时被覆盖。

<!--@include: ../../../dc3/doc/CHANGE.md-->

## 延伸阅读

- [开发概览与规范](./) — 二次开发的整体地图与编码约定
- [贡献指南](../community/contributing) — 提交信息规范、commit-msg 钩子与贡献流程
