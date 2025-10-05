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
IoT DC3はSpring Cloud上に構築された完全オープンソースの分散型IoT（モノのインターネット）プラットフォームです。
IoTプロジェクト開発を加速し、IoTデバイス管理を簡素化し、堅牢なIoTシステム構築のための包括的なソリューションを提供します。
すべてのコンポーネントとコードはオープンソースであり、透明性、柔軟性、コミュニティ主導のイノベーションを保証します。
</strong>
</p>

---

![iot-dc3-architecture](dc3/images/architecture-en.png)

# 1 アーキテクチャ

- **ドライバー層**: 標準および独自プロトコルを使用して物理デバイスとのシームレスな接続を容易にするSDKを提供します。この層は南向きのデータ取得とコマンド実行を担当し、包括的なSDKを通じて迅速なドライバー開発を可能にします。
- **データ層**: デバイスデータの収集、保存、取得を管理し、効率的なデータ処理を確保するための堅牢なデータ管理インターフェースサービスを提供します。
- **管理層**: マイクロサービス間の相互作用の中核ハブとして機能し、マイクロサービス登録、デバイスコマンドインターフェース、デバイス登録とペアリング、集中データ管理システムなどの重要なサービスを提供します。さまざまな構成データを監視し、シームレスな統合のための外部インターフェースサービスを提供します。
- **アプリケーション層**: データの公開性、タスクスケジューリング、アラームとメッセージ通知、ログ管理、サードパーティプラットフォームとの統合能力など、高度な機能を提供し、プラットフォームの多様性と使いやすさを向上させます。

# 2 目標

- **スケーラビリティ**: 主要なオープンソース技術であるSpring Cloudの力を活用し、水平スケーリング用に設計されています。
- **耐障害性**: 単一障害点がなく、各クラスターノードが同一で交換可能です。
- **パフォーマンス**: 特定のユースケースに応じて、単一のサーバーノードで数十万のデバイスを処理できます。
- **カスタマイズ性**: 新しいデバイスプロトコルを簡単に統合し、サービスセンター内に登録できます。
- **クロスプラットフォーム互換性**: Java環境と完全に互換性があり、複数のプラットフォームにわたるシームレスな分散デプロイメントを可能にします。
- **デプロイメントの柔軟性**: プライベートクラウド、パブリッククラウド、エッジデプロイメントをサポートし、インフラストラクチャを完全に制御できます。
- **効率性**: デバイスのオンボーディング、登録、権限検証プロセスを合理化します。
- **セキュリティ**: データ送信が暗号化され、機密情報を保護します。
- **マルチテナンシー**: 名前空間とマルチテナンシーをサポートし、多様なユーザー環境に最適です。
- **クラウドネイティブ**: Kubernetesに最適化され、最新のクラウドインフラストラクチャとのスムーズな統合を保証します。
- **コンテナ化**: Dockerで完全にコンテナ化され、デプロイメントと管理が簡素化されています。

# 3 開発

## 3.1 起動依存関係

> いずれかを選択してください
>
> データベースSQLスクリプトが必要な場合は、コンテナ内で起動したデータベースに直接接続してエクスポートしてください

```bash
# 標準Dockerレジストリサービスによるグローバルアクセス
docker-compose -f dc3/docker-compose-db.yml up -d

# 中国本土のユーザー向けに最適化されたレジストリサービス
docker-compose -f dc3/docker-compose-db-aliyun.yml up -d
```

## 3.2 準備

```bash
source dc3/env/dev.env.sh
mvn clean package
```

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

# 仮想ドライバー
java -jar dc3-driver/dc3-driver-virtual/target/dc3-driver-virtual.jar

# その他のドライバー: リスニング仮想ドライバー、Modbus TCPドライバー、MQTTドライバー、OPC DAドライバー、OPC UAドライバー、Siemens S7ドライバー
```

# 4 技術スタック

- [Java 21](https://www.java.com)
- [Spring Boot 3.5.5](https://spring.io/projects/spring-boot)
- [Spring Cloud 2025.0.0](https://spring.io/projects/spring-cloud)

# 5 貢献

- **ブランチ作成**: `main`ブランチから新しいブランチを作成することから始めます。ブランチを作成する前に`main`ブランチが最新であることを確認してください。
- **ブランチ命名**: 新しいブランチには次の命名規則に従ってください: `feature/your_name/feature_description`。例: `feature/pnoker/mqtt_driver`
- **コードとドキュメント**: 新しいブランチでコードまたはドキュメントに変更を加えます。完了したら、変更をコミットします。
- **プルリクエスト**: `develop`ブランチに変更をマージするための`Pull Request`（PR）を提出します。PRはメンテナによってレビューされ、マージされます。

# 6 ライセンス

`IoT DC3`オープンソースプラットフォームは[AGPL 3.0 License](./LICENSE-AGPL.txt)の下でライセンスされています。