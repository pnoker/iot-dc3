:boom: `IoT DC3` 新版管理平台，迭代开发中... :boom:

:rocket: 非常欢迎广大兴趣爱好者的加入，你的 `Star` 是我们开发的动力 ！

:seedling: 该 `Web UI` ，需要借助 `DC3` 平台服务，`https://gitee.com/pnoker/iot-dc3` 或 `https://github.com/pnoker/iot-dc3`

---

## 一、准备

- `nodejs` >= 12
- `git`
- `Web Storm` 或者 `Visual Studio Code`

### 配置国内 `npm` 源

> 在用户目录下创建 `.npmrc` 文件，并写入以下内容

```txt
registry=https://registry.npm.taobao.org
sass_binary_site=https://npm.taobao.org/mirrors/node-sass
```

### 安装 `yarn`

```bash
npm install -g yarn
```

## 二、源码

```bash
git clone https://github.com/pnoker/iot-dc3-web.git
```

## 三、开发

```bash
cd iot-dc3-web

# install
yarn

# run
yarn run serve
```

---
## Visual Studio Code 配置

> 可以参考setting.json

```json
{
    "workbench.tree.indent": 16,
    "workbench.editor.wrapTabs": true,
    "workbench.editor.enablePreview": false,
    "workbench.colorTheme": "Default Dark+",
    "workbench.iconTheme": "vscode-jetbrains-icon-theme",
    "workbench.tree.renderIndentGuides": "always",
    "editor.hover.enabled": false,
    "editor.fontFamily": "Monaco, Consolas, 'Courier New', monospace",
    "editor.tabSize": 4,
    "editor.formatOnSave": true,
    "editor.formatOnPaste": true,
    "editor.codeActionsOnSave": {
        "source.fixAll.eslint": true
    },
    "editor.detectIndentation": false,
    "files.associations": {
        "*.ttml": "html",
        "*.ttss": "css",
        "*.wxss": "css",
        "*.wxml": "html"
    },
    "search.exclude": {
        "**/node_modules": true,
        "**/bower_components": true,
        "**/target": true,
        "**/logs": true,
    },
    "extensions.ignoreRecommendations": true,
    "markdown.preview.openMarkdownLinks": "inEditor",
    "typescript.updateImportsOnFileMove.enabled": "always",
    "git.autofetch": true,
    "git.enableSmartCommit": true,
    "terminal.integrated.cursorBlinking": true,
    "terminal.integrated.cursorStyle": "line",
    "terminal.integrated.defaultProfile.windows": "GitBash",
    "terminal.integrated.profiles.windows": {
        "GitBash": {
            "path": "D:\\Program Files\\Git\\bin\\bash.exe",
            "args": [
                "-li"
            ]
        }
    },
    "[json]": {
        "editor.defaultFormatter": "vscode.json-language-features"
    },
    "[jsonc]": {
        "editor.defaultFormatter": "vscode.json-language-features"
    },
    "[html]": {
        "editor.defaultFormatter": "vscode.html-language-features"
    },
    "[javascript]": {
        "editor.defaultFormatter": "vscode.typescript-language-features"
    },
    "[typescript]": {
        "editor.defaultFormatter": "vscode.typescript-language-features"
    },
    "[vue]": {
        "editor.defaultFormatter": "octref.vetur"
    },
    "[scss]": {
        "editor.defaultFormatter": "vscode.css-language-features"
    },
    "vetur.format.options.useTabs": true,
    "vetur.format.defaultFormatterOptions": {
        "js-beautify-html": {
            "wrap_attributes": "force-aligned"
        },
        "prettier": {
            "printWidth": 180,
            "semi": false,
            "singleQuote": true,
            "wrapAttributes": true,
            "sortAttributes": true,
            "eslintIntegration": true
        }
    }
}
```
