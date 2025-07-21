# AroundFloorPlugin

<div align="center">

![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21-brightgreen)
![Paper API](https://img.shields.io/badge/Paper%20API-1.21+-blue)
![Java Version](https://img.shields.io/badge/Java-21+-orange)
![License](https://img.shields.io/badge/License-MIT-yellow)

**プレイヤー周囲のブロック可視化プラグイン**

地下構造を地上に動的表示する革新的なMinecraftプラグイン

</div>

## 📖 概要

AroundFloorPluginは、プレイヤーの周囲半径10m以内のエリアに限定してブロックを可視化するMinecraftプラグインです。  
地下に設置されたブロック構造を、プレイヤーが近づいた時に地上にリアルタイムでコピー表示し、プレイヤーが離れると自動的に消去します。

### ✨ 主な特徴

- 🎯 **プレイヤー中心の可視化**: 半径10mの円形範囲で動的表示
- 🏗️ **高さ構造保持**: 建造物の形状を正確に再現
- 👥 **マルチプレイヤー対応**: 複数プレイヤーが同時使用可能
- ⚡ **リアルタイム更新**: プレイヤーの移動に連動した即座の反映
- 🛠️ **設定可能**: 範囲、高さ、更新間隔をカスタマイズ
- 🔧 **管理コマンド**: 統計情報表示と設定再読み込み

## 🎮 動作例

```
プレイヤーがy=-40に建てた家 → プレイヤーが近づくとy=60に表示
地下迷路 (y=-50～-30) → 地上迷路 (y=50～70) として可視化
地下要塞構造 → 地上に完全再現され、離れると消失
```

![動作例](./assets/play.gif)

## 📋 必要環境

- **Minecraft**: 1.20.1 以降
- **サーバー**: Paper / Spigot
- **Java**: 8 以降
- **メモリ**: 最低2GB推奨

## 🚀 インストール

1. **プラグインファイルのダウンロード**
   ```bash
   # リリースページからAroundFloorPlugin.jarをダウンロード
   ```

2. **サーバーへの配置**
   ```bash
   # pluginsフォルダにjarファイルを配置
   cp AroundFloorPlugin.jar /path/to/server/plugins/
   ```

3. **サーバー再起動**
   ```bash
   # サーバーを再起動してプラグインを読み込み
   ```

4. **設定ファイル確認**
   ```
   plugins/AroundFloorPlugin/config.yml が自動生成されます
   ```

## ⚙️ 設定

### config.yml

```yaml
aroundfloor:
  # 可視化範囲（ブロック単位）
  visibility-range: 10
  # 対象Y座標範囲
  source-y-min: -50
  source-y-max: 319
  # 表示Y座標範囲
  display-y-min: 50
  display-y-max: 419
  # Y座標オフセット
  y-offset: 100
  # 更新間隔（tick）
  update-interval: 20
  # デバッグモード
  debug: false
```

### 設定項目詳細

| 項目 | 説明 | デフォルト値 | 推奨範囲 |
|------|------|-------------|----------|
| `visibility-range` | 可視化範囲（ブロック） | 10 | 5-20 |
| `source-y-min` | 対象エリア最低高度 | -50 | -64～100 |
| `source-y-max` | 対象エリア最高高度 | 319 | 100～319 |
| `display-y-min` | 表示エリア最低高度 | 50 | 0～150 |
| `display-y-max` | 表示エリア最高高度 | 419 | 150～419 |
| `y-offset` | Y座標変換オフセット | 100 | 50～200 |
| `update-interval` | 更新間隔（tick） | 20 | 10～60 |
| `debug` | デバッグ情報出力 | false | true/false |

## 🎯 コマンド

### 基本コマンド

```
/aroundfloor [サブコマンド]
エイリアス: /af
```

### サブコマンド一覧

| コマンド | 説明 | 権限 |
|----------|------|------|
| `/aroundfloor reload` | 設定ファイルを再読み込み | `aroundfloor.admin` |
| `/aroundfloor stats` | 統計情報を表示 | `aroundfloor.admin` |
| `/aroundfloor help` | ヘルプを表示 | `aroundfloor.admin` |

### 使用例

```bash
# 設定を再読み込み
/af reload

# 現在の統計情報を確認
/af stats

# ヘルプを表示
/af help
```

## 🔑 権限

| 権限ノード | 説明 | デフォルト |
|------------|------|-----------|
| `aroundfloor.admin` | プラグイン管理権限 | `op` |

## 🎨 使用方法

### 基本的な使い方

1. **地下構造の作成**
   ```
   y=-50～y=319の範囲にブロックで構造を建築
   ```

2. **地上での確認**
   ```
   建築した地点の真上（y=50～y=419）に移動
   ```

3. **自動表示**
   ```
   プレイヤーが半径10m以内に近づくと自動的に表示
   離れると自動的に消去
   ```

### 応用例

- **地下都市の可視化**: 大規模な地下都市を地上で確認
- **建築プレビュー**: 地下で建築した構造の地上プレビュー
- **探索ゲーム**: 地下迷路の地上マップ表示
- **教育用途**: 地層構造や建築技法の学習

## 📊 パフォーマンス

### 最適化機能

- **差分更新**: 変更されたブロックのみ処理
- **範囲制限**: 処理範囲を円形に限定
- **チャンク最適化**: 未読み込みチャンクの処理スキップ
- **参照カウント**: 複数プレイヤー対応の効率的管理

### パフォーマンス指標

| 項目 | 推奨値 | 最大値 |
|------|--------|--------|
| 同時アクティブプレイヤー | 20人 | 50人 |
| 可視化範囲 | 10ブロック | 20ブロック |
| 更新間隔 | 20tick | 10tick |
| 処理ブロック数/プレイヤー | 500個 | 1000個 |

## 🔧 開発情報

### 技術仕様

- **言語**: Java 8+
- **API**: Paper API 1.20.1+
- **ビルドツール**: Maven 3.6+
- **アーキテクチャ**: イベント駆動型プラグイン

### ビルド方法

```bash
# リポジトリのクローン
git clone https://github.com/your-username/AroundFloorPlugin.git
cd AroundFloorPlugin

# Maven ビルド
mvn clean package

# 生成されるjar
target/AroundFloorPlugin-1.0-SNAPSHOT.jar
```

### プロジェクト構造

```
src/
├── main/
│   ├── java/com/kamesuta/aroundfloor/
│   │   ├── AroundFloorPlugin.java      # メインクラス
│   │   ├── Config.java                 # 設定管理
│   │   ├── BlockManager.java           # ブロック管理システム
│   │   └── PlayerListener.java         # イベント処理
│   └── resources/
│       ├── plugin.yml                  # プラグイン定義
│       └── config.yml                  # デフォルト設定
├── Spec.md                             # 技術仕様書
├── README.md                           # このファイル
└── LICENSE                             # MITライセンス
```

## 🐛 トラブルシューティング

### よくある問題

#### ❌ ブロックが表示されない
```
原因: 対象Y範囲外 or チャンク未読み込み
解決: 設定ファイルの範囲確認、該当エリアを読み込み
```

#### ❌ パフォーマンス低下
```
原因: 可視化範囲が大きすぎる
解決: visibility-rangeを10以下に設定
```

#### ❌ 設定が反映されない
```
原因: 設定ファイル構文エラー or 再読み込み未実行
解決: /af reload コマンド実行
```

### デバッグ方法

1. **デバッグモード有効化**
   ```yaml
   debug: true
   ```

2. **統計情報確認**
   ```
   /af stats
   ```

3. **ログ確認**
   ```
   logs/latest.log でエラー情報確認
   ```

## 📄 ライセンス

このプロジェクトは [MIT License](LICENSE) の下で公開されています。

## 👨‍💻 作者

**Kamesuta** - [GitHub](https://github.com/kamesuta)
