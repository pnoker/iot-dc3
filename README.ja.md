<p align="right">
  <a href="./README.md">English</a> | <a href="./README.zh.md">中文</a> | <a href="./README.ja.md">日本語</a> | <a href="./README.vi.md">Tiếng Việt</a> | <a href="./README.ko.md">한국어</a> | <a href="./README.es.md">Español</a> | <a href="./README.ru.md">Русский</a>
</p>

> **AI アシスタント：** IoT DC3 の簡潔な AI 向け概要については、最初に [README.ai.md](./README.ai.md) をお読みください。

<p align="center">
  <img src="docs/public/images/logo.png" width="240" alt="IoT DC3">
</p>

<p align="center">
  <a href="https://github.com/pnoker/iot-dc3/stargazers">
    <img src="https://img.shields.io/github/stars/pnoker/iot-dc3?style=flat&logo=github&color=green" alt="GitHub Stars">
  </a>
  <a href="https://gitee.com/pnoker/iot-dc3/stargazers">
    <img src="https://gitee.com/pnoker/iot-dc3/badge/star.svg?theme=gvp" alt="Gitee Star">
  </a>
  <a href="https://gitee.com/pnoker/iot-dc3/members">
    <img src="https://gitee.com/pnoker/iot-dc3/badge/fork.svg?theme=gvp" alt="Gitee Fork">
  </a>
  <a href="https://github.com/pnoker/iot-dc3/graphs/contributors">
    <img src="https://img.shields.io/github/contributors/pnoker/iot-dc3?label=contributors&color=orange" alt="Contributors">
  </a>
  <img src="https://img.shields.io/badge/License-AGPL%203.0-blue" alt="License">
  <img src="https://img.shields.io/badge/Java-21-orange?logo=openjdk" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring%20Boot-4.0-6DB33F?logo=springboot" alt="Spring Boot 4">
</p>

<p align="center">
  <strong>
    IoT DC3 — マルチプロトコル接続・AI 活用・クラウドネイティブなオープンソース産業 IoT プラットフォーム<br>
    クラウドネイティブマイクロサービス · マルチプロトコル接続 · AI 支援運用 · 28 個のすぐ使えるドライバー
  </strong>
</p>

<p align="center">
  <a href="https://docs.dc3.site">https://docs.dc3.site</a>
</p>

<p align="center">
  🔌 <strong>マルチプロトコル接続</strong> &nbsp;·&nbsp;
  🤖 <strong>AI Agentic Center</strong> &nbsp;·&nbsp;
  ☁️ <strong>クラウドネイティブマイクロサービス</strong>
</p>

---

## 📸 プロダクトプレビュー

<table>
  <tr>
    <th width="33%">📸 プラットフォーム概要</th>
    <th width="33%">📸 デバイス管理</th>
    <th width="33%">📸 AI チャット</th>
  </tr>
  <tr>
    <td align="center">
      <img src="docs/public/images/screenshot-overview.png" alt="プラットフォームダッシュボード" width="100%">
      <br>
      <strong>ホーム / ダッシュボード</strong><br>
      <em>システム概要 · デバイスオンライン統計 · データトレンドチャート</em>
    </td>
    <td align="center">
      <img src="docs/public/images/screenshot-device.png" alt="デバイス管理ページ" width="100%">
      <br>
      <strong>デバイス管理</strong><br>
      <em>デバイス一覧 · オンライン状態 · 検索とフィルタ</em>
    </td>
    <td align="center">
      <img src="docs/public/images/screenshot-ai.png" alt="AI チャットページ" width="100%">
      <br>
      <strong>AI チャット</strong><br>
      <em>自然言語によるデバイス照会 · データ分析 · インテリジェント支援</em>
    </td>
  </tr>
</table>

## 🏗️ アーキテクチャ概要

### 製品アーキテクチャ全景

![IoT DC3 Architecture Panorama](docs/public/images/architecture-panorama-ja.png)

6層マイクロサービスアーキテクチャの全体像：クライアント → ゲートウェイ → 4つのセンターサービス → メッセージバス → 28
プロトコルドライバー → フィールドデバイス。PostgreSQL（TimescaleDB + pgvector +
AGE）永続層とオプションの可観測性スタック（ELK +
Prometheus + Grafana）を一望できます。

🧱 **設計原則** — サービス間呼び出しは常に Facade インターフェース経由；DO/BO/VO の三層モデルで永続化・ビジネス・API
の形を厳密に分離；テナント分離をデータベース・キャッシュ・API パスまで一貫して適用。境界が明確で、サービスとチームの規模拡大に強い設計です。

> 📖 完全なアーキテクチャドキュメントについては、
> [システムアーキテクチャ概要](https://docs.dc3.site/en/architecture/)を参照してください。

## ✨ 主な機能

### 🔌 マルチプロトコルデバイス接続

IoT DC3 は **28 個の接続ドライバーモジュール**を内蔵し、産業オートメーション、IoT
通信、データブリッジ、基本通信、シミュレーションとデバッグのシナリオをカバーします。一般的なデバイスやデータソースの接続コストを下げます。

| 分類                   | ドライバーモジュール                                                                                                                                         |
|----------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| 🏭 **産業プロトコル**       | Modbus TCP · Modbus RTU · OPC UA · OPC DA · Siemens S7 · BACnet/IP · EtherNet/IP · Omron FINS · Mitsubishi MELSEC · IEC 60870-5-104 · SL651 · DLMS |
| 📡 **IoT プロトコル**     | MQTT · CoAP · LwM2M · HTTP · BLE · Zigbee                                                                                                          |
| 🗄️ **データブリッジ**      | MySQL · PostgreSQL · Oracle · SQL Server                                                                                                           |
| 🔧 **基本通信とネットワーク管理** | TCP/UDP · Serial · SNMP · CAN                                                                                                                      |
| 🧪 **シミュレーションとデバッグ** | Virtual · Listening Virtual                                                                                                                        |

**Driver SDK** により、カスタムプロトコルドライバーをすばやく開発し、実行中のプラットフォームへ登録できます。

### 🤖 AI 機能統合

**Spring AI** ベースの Agentic Center により、大規模言語モデルを IoT 運用ワークフローへ接続します。

- **自然言語による運用支援** - LLM が Tool Calling を通じて、権限管理のもとでデバイス照会、ポイント読み書き、コマンド実行を支援します
- **インテリジェントなアラーム分析** - AI が原因分析と対応提案を支援します
- **データインサイト** - 自然言語でデバイスデータを照会し、可視化チャートを生成します
- **複数モデル対応** - OpenAI API 互換プロバイダーや GPT、Claude、DeepSeek、Qwen などの主要モデルに対応します
- **会話メモリ** - 複数ターンの会話とコンテキストメモリをデータベースへ永続化します

### 🏗️ クラウドネイティブマイクロサービス

**Spring Boot 4 + Spring Cloud 2025** を基盤とする分散マイクロサービスアーキテクチャです。

- **サービスガバナンス** - Spring Cloud Gateway を統一入口とし、静的ルーティングと環境変数で柔軟に設定できます
- **効率的な通信** - gRPC によるサービス間呼び出しと Protobuf シリアライズ
- **水平スケール** - ステートレス設計により、業務負荷に応じてサービスを個別にスケールできます
- **レジリエンス** - 交換可能なサービスノードと障害分離

### 📊 リアルタイムデータエンジン

- **データ収集** - ドライバー層がデバイステレメトリを収集し、RabbitMQ 経由で非同期に転送します
- **時系列ストレージ** - リアルタイムデータと履歴データを効率的にクエリできます
- **ルールエンジン** - 柔軟なアラームルール、多段階アラーム、通知をサポートします
- **イベント追跡** - コマンドとイベントの履歴を保持します

### 🔐 エンタープライズセキュリティとマルチテナンシー

- **テナント分離** - データベース、キャッシュ、API パスでテナント単位の分離を行います
- **認証と認可** - JWT + Spring Security、RBAC 権限モデル
- **通信暗号化** - TLS/SSL 通信をサポートします
- **監査追跡** - ユーザー操作とシステムイベントのログを保持します

### 🧩 開発者フレンドリー

- **Driver SDK** -
  充実したドライバー開発ツールキットです。[ドライバー開発ガイド](https://docs.dc3.site/en/development/driver-authoring)
  を参照してください
- **フロントエンド / バックエンド分離** - Vue 3 + TypeScript フロントエンド、RESTful + gRPC API
- **コンテナ化デプロイ** - Podman / Docker Compose でワンコマンド起動でき、Kubernetes などのコンテナプラットフォームへ移行しやすい構成です
- **ドキュメント整備** - オンラインドキュメント、クイックスタート、トラブルシューティングガイド

## ⚡ クイックスタート

ソースからローカル開発する場合は、PostgreSQL と RabbitMQ を起動し、ローカル環境変数を読み込んでからビルドします。

```bash
make up-db
source dc3/env/dev.env.sh
mvn -s .mvn/settings.xml clean package
```

中国大陸向けの Alibaba Cloud レジストリを使う場合は `make up-db-cn` を利用してください。

> 📖 サービスの起動順序、IDE 設定、検証コマンド、よくある落とし穴については、
> [完全なクイックスタート](https://docs.dc3.site/en/quickstart/)を参照してください。

## 🛠️ 技術スタック

IoT DC3 は Java 21、Spring Boot 4、Spring Cloud 2025、Spring AI 2、PostgreSQL、RabbitMQ、gRPC、Vue 3、
TypeScript、Vite を基盤に構築されています。

各コンポーネントの役割と詳細は [Technology Stack](https://docs.dc3.site/en/introduction/technology-stack) を参照してください。

## 📖 ドキュメントとコミュニティ

| リソース           | リンク                                                                        |
|----------------|----------------------------------------------------------------------------|
| 📚 オンラインドキュメント | [docs.dc3.site](https://docs.dc3.site/)                                    |
| 🚀 クイックスタート    | [クイックスタートガイド](https://docs.dc3.site/en/quickstart/)                        |
| 🛠️ 技術スタック     | [Technology Stack](https://docs.dc3.site/en/introduction/technology-stack) |
| 🏗️ アーキテクチャ    | [モジュールと依存関係](https://docs.dc3.site/en/architecture/modules)                |
| 🔧 ドライバー開発     | [ドライバー開発ガイド](https://docs.dc3.site/en/development/driver-authoring)        |
| 🐛 トラブルシューティング | [よくある問題と解決策](https://docs.dc3.site/en/guide/troubleshooting)               |
| 📋 変更履歴        | [リリース変更履歴](https://docs.dc3.site/en/development/changelog)                 |
| 🐛 問題報告        | [GitHub Issues](https://github.com/pnoker/iot-dc3/issues)                  |
| 🇨🇳 Gitee ミラー | [Gitee GVP プロジェクト](https://gitee.com/pnoker/iot-dc3)                       |

## 🌍 ユースケース

<table>
  <tr>
    <td align="center" width="60">🏭</td>
    <td><strong>スマートファクトリー</strong></td>
    <td>生産ライン設備の状態監視、工程パラメータ収集、予知保全、OEE 分析</td>
  </tr>
  <tr>
    <td align="center">⚡</td>
    <td><strong>エネルギー監視</strong></td>
    <td>電力 / 水道 / ガスの遠隔検針、エネルギー傾向分析、異常アラーム</td>
  </tr>
  <tr>
    <td align="center">🌾</td>
    <td><strong>スマート農業</strong></td>
    <td>温室環境監視、自動灌漑制御、病害虫警告、収量予測</td>
  </tr>
  <tr>
    <td align="center">🏙️</td>
    <td><strong>スマートシティ</strong></td>
    <td>街路灯管理、環境品質監視、公共施設運用、安全監視</td>
  </tr>
</table>

## 🤝 コントリビューション

あらゆる形のコントリビューションを歓迎します。以下の流れに従ってください。

1. **Fork とブランチ作成** - `main` からブランチを作成し、`feature/your_name/feature_description` 形式で命名します
   （例: `feature/pnoker/mqtt_driver`）
2. **開発とコミット** - 新しいブランチで変更を完了し、[Conventional Commits](https://www.conventionalcommits.org/)
   仕様に従います
3. **PR 作成** - `develop` ブランチへ Pull Request を提出し、メンテナーのレビューとマージを受けます

## 📄 ライセンス

IoT DC3 は [AGPL 3.0](./LICENSE-AGPL.txt) ライセンスの下でオープンソースとして公開されています。

- ✅ **個人学習、研究、内部利用** - 無料
- ✅ **コードを変更し、その変更をオープンソース化すること** - 歓迎します
- ⚠️ **変更を公開せず第三者向け商用サービスとして提供する場合** - 商用ライセンスが必要です

商用ライセンスの詳細は [LICENSE.txt](./LICENSE.txt) を参照してください。

## ⭐ Star 履歴

[![Star History Chart](https://api.star-history.com/svg?repos=pnoker/iot-dc3&type=Date)](https://star-history.com/#pnoker/iot-dc3&Date)
