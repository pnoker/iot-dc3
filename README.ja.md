<p align="right">
  <a href="./README.md">English</a> | <a href="./README.zh.md">中文</a> | <a href="./README.ja.md">日本語</a> | <a href="./README.vi.md">Tiếng Việt</a>
</p>

<p align="center">
	<img src="dc3/images/logo-blue.png" width="400" alt="IoT DC3 Logo">
<br>
<a href='https://gitee.com/pnoker/iot-dc3/stargazers'>
    <img src='https://gitee.com/pnoker/iot-dc3/badge/star.svg?theme=gvp' alt='star'/>
</a>
<a href='https://gitee.com/pnoker/iot-dc3/members'>
    <img src='https://gitee.com/pnoker/iot-dc3/badge/fork.svg?theme=gvp' alt='fork'/>
</a>
<br>
<strong>
IoT DC3は、完全オープンソースのAI-Readyな分散型IoTプラットフォームです。
デバイスを接続し、AIが活用できる形でデータを収集し、クローズドループを統括します——インテリジェンスを洞察に留めず、行動に変えるのです。
</strong>
</p>

---

![iot-dc3-architecture](dc3/images/architecture-en.png)

# 1 アーキテクチャ

このアーキテクチャは、デバイス接続、データサービス、運用管理、拡張可能なアプリケーション統合までを含む、エンドツーエンドのIoT機能を実現するために設計されています。

- **ドライバー層**: 標準/独自プロトコルを通じて物理デバイスへ接続するSDKを提供し、南向きのデータ収集とコマンド実行を担います。
- **データ層**: デバイスデータの収集・保存・参照を信頼性高く提供し、リアルタイム/履歴データサービスを支えます。
- **管理層**: 分散マイクロサービス連携の中核として、サービス登録、デバイス/ドライバー管理、コマンドオーケストレーション、設定ガバナンスを担います。
- **アプリケーション層**: データ公開、スケジューリング、アラート通知、ログ管理、サードパーティ統合、AI拡張自動化シナリオを実現します。

# 2 目標

- **スケーラビリティ**: Spring Cloudにより水平スケールを実現し、高スループットな分散IoTワークロードに対応します。
- **レジリエンス**: 交換可能なサービスノードと耐障害設計により、単一障害点リスクを最小化します。
- **パフォーマンス**: 大規模デバイス接続とテレメトリ処理に対応します。
- **拡張性**: SDKとサービス登録により、新規プロトコルやカスタムドライバー統合を高速化します。
- **デプロイ柔軟性**: プライベートクラウド、パブリッククラウド、エッジで動作し、Java互換性を維持します。
- **運用効率**: デバイスのオンボーディング、登録、権限検証を効率化します。
- **セキュリティとマルチテナンシー**: 暗号化通信、名前空間分離、テナント分離をサポートします。
- **クラウドネイティブ提供**: Kubernetes最適化とDockerコンテナ化で一貫したデプロイを実現します。
- **AI-Ready進化**: インテリジェント自動化とデータドリブン運用の統合を可能にします。

# 3 開発

## 3.1 起動依存関係

> いずれかを選択してください
>
> この基本依存スタックでは PostgreSQL と RabbitMQ を起動します。データベースSQLスクリプトが必要な場合は、コンテナ内で起動したデータベースに直接接続してエクスポートしてください

```bash
# 標準Dockerレジストリサービスによるグローバルアクセス
podman compose -f dc3/docker-compose-db.yml up -d

# 中国本土のユーザー向けに最適化されたレジストリサービス
DC3_IMAGE_REGISTRY=registry.cn-beijing.aliyuncs.com/dc3 podman compose -f dc3/docker-compose-db.yml up -d
```

利用しやすい `make` ショートカット:

```bash
make dev-db
make dev-optional
make dev
make dev-all
```

中国本土向けのイメージレジストリを使う場合は `REGISTRY=cn` を指定してください:

```bash
make dev-db REGISTRY=cn
make dev-all REGISTRY=cn
make app-all REGISTRY=cn
make compose-up STACK=optional REGISTRY=cn
make compose-logs STACK=dev REGISTRY=global
```

### Compose 環境変数の上書き

公開ポート、イメージタグ、またはオブザーバビリティ設定を変更する前に、まずテンプレートファイルをコピーしてください:

```bash
cp .env.example .env
```

リポジトリ直下の `.env` は `dc3/` 配下の Compose ファイル用の変数展開に使用されます。アプリケーション実行時の環境変数は引き続き
`dc3/env/dev.env` または `dc3/env/dev.env.sh`
で管理されます。

Compose は、Compose ファイルで明示的に参照された変数のみをコンテナへ渡します。たとえばイメージレジストリ、イメージタグ、公開ポート、
ログ設定、任意の監視スタック設定です。Agentic provider は通常データベースに保存されます。フォールバックが必要な場合のみ
`AGENTIC_FALLBACK_OPENAI_BASE_URL`、`AGENTIC_FALLBACK_OPENAI_API_KEY`、`AGENTIC_FALLBACK_OPENAI_MODEL` を設定してください。

リポジトリ直下の `.env` と `dc3/env/dev.env(.sh)` の違いは [`dc3/doc/ENVIRONMENT.md`](dc3/doc/ENVIRONMENT.md) を参照してください。

## 3.2 準備

```bash
source dc3/env/dev.env.sh
mvn -s .mvn/settings.xml clean package
```

> **モジュール概要**: モジュールの依存関係マップとランタイムフロー図は [`dc3/doc/MODULES.md`](dc3/doc/MODULES.md)
> を参照してください。

> **ローカル開発ガイド**: ワンストップのローカルセットアップ手順は [`dc3/doc/QUICKSTART.md`](dc3/doc/QUICKSTART.md)
> を参照してください。

> **トラブルシューティング**: よくあるビルド/ランタイムの問題と解決策は [
`dc3/doc/TROUBLESHOOTING.md`](dc3/doc/TROUBLESHOOTING.md) を参照してください。

## 3.3 サービスの起動

> 順番に起動してください

```bash
# ゲートウェイ
java -jar dc3-gateway/target/dc3-gateway.jar

# 認証センター
java -jar dc3-center/dc3-center-auth/target/dc3-center-auth.jar

# データセンター
java -jar dc3-center/dc3-center-data/target/dc3-center-data.jar

# マネージャーセンター
java -jar dc3-center/dc3-center-manager/target/dc3-center-manager.jar

# Agenticセンター
java -jar dc3-center/dc3-center-agentic/target/dc3-center-agentic.jar

# 仮想ドライバー
java -jar dc3-driver/dc3-driver-virtual/target/dc3-driver-virtual.jar

# その他のドライバー: リスニング仮想ドライバー、Modbus TCPドライバー、MQTTドライバー、OPC DAドライバー、OPC UAドライバー、Siemens S7ドライバー
```

# 4 技術スタック

- [Java 21](https://www.java.com)
- [Spring Boot 3.5.5](https://spring.io/projects/spring-boot)
- [Spring Cloud 2025.0.0](https://spring.io/projects/spring-cloud)

# 5 貢献

- **ブランチ作成**: `main`ブランチから新しいブランチを作成することから始めます。ブランチを作成する前に`main`
  ブランチが最新であることを確認してください。
- **ブランチ命名**: 新しいブランチには次の命名規則に従ってください: `feature/your_name/feature_description`。例:
  `feature/pnoker/mqtt_driver`
- **コードとドキュメント**: 新しいブランチでコードまたはドキュメントに変更を加えます。完了したら、変更をコミットします。
- **プルリクエスト**: `develop`ブランチに変更をマージするための`Pull Request`（PR）を提出します。PRはメンテナによってレビューされ、マージされます。

# 6 ライセンス

`IoT DC3`オープンソースプラットフォームは[AGPL 3.0 License](./LICENSE-AGPL.txt)の下でライセンスされています。
リポジトリのライセンス通知と商用ライセンスの説明は [LICENSE.txt](./LICENSE.txt) を参照してください。
