# csv-groovy

CSV から指定した列だけを切り出して出力する Groovy ツールです。

## 必要環境

- [Groovy](https://groovy.apache.org/)（`groovy` コマンドが PATH に通っていること）
- Java

## ファイル構成

| ファイル | 説明 |
|----------|------|
| `ExtractCsvColumns.groovy` | 列切り出し本体 |
| `ExtractCsvColumns.bat` | ドラッグ＆ドロップ用ランチャー |
| `sample.csv` | 動作確認用サンプル |

## 主な仕様

- ヘッダー行あり前提（出力にもヘッダーを含める）
- 列は **ヘッダー名** または **0 始まりのインデックス** で指定
- 囲み文字（`"`）内の改行に対応
- 出力時はフィールド内の改行をスペースに置換し、1行にまとめる
- 文字コード指定可能（省略時は UTF-8）
- 列指定なしのときは利用可能な列を一覧表示

## Groovy を直接使う

### 利用可能な列を表示

```bash
groovy ExtractCsvColumns.groovy sample.csv
groovy ExtractCsvColumns.groovy sample.csv MS932
```

表示例:

```
利用可能な列:
0:id
1:name
2:age
3:email
4:city
```

### 列を切り出して出力

```bash
# ヘッダー名で指定
groovy ExtractCsvColumns.groovy sample.csv out.csv name,email

# インデックスで指定
groovy ExtractCsvColumns.groovy sample.csv out.csv 1,3

# 文字コード指定（入力のみ / 入力と出力）
groovy ExtractCsvColumns.groovy in.csv out.csv name,email MS932
groovy ExtractCsvColumns.groovy in.csv out.csv name,email MS932 UTF-8
```

#### 引数

```
groovy ExtractCsvColumns.groovy <入力CSV> [入力文字コード]
groovy ExtractCsvColumns.groovy <入力CSV> <出力CSV> <列指定> [入力文字コード] [出力文字コード]
```

| 引数 | 説明 |
|------|------|
| 入力CSV | 対象ファイル |
| 出力CSV | 切り出し結果の保存先 |
| 列指定 | `name,email` または `1,3`（カンマ区切り） |
| 入力文字コード | 省略時 `UTF-8`（例: `MS932`, `Shift_JIS`, `EUC-JP`） |
| 出力文字コード | 省略時は入力と同じ |

## bat / ショートカットで使う（推奨）

CSV をショートカットにドラッグ＆ドロップすると、すぐに切り出しを実行します。

### セットアップ

1. `ExtractCsvColumns.bat` のショートカットを作成する
2. ショートカットを右クリック → **プロパティ**
3. **リンク先** にパラメータを追加する

```
"C:\path\to\ExtractCsvColumns.bat" "name,email" MS932 UTF-8 _extracted
```

列指定にカンマを含む場合は、必ずダブルクォートで囲んでください。

### 引数

```
ExtractCsvColumns.bat "列指定" [入力文字コード] [出力文字コード] [付加文字] ＋（D&Dで）入力CSV
```

| 引数 | 説明 | 省略時 |
|------|------|--------|
| 列指定 | 切り出す列（必須） | — |
| 入力文字コード | 入力 CSV の文字コード | `UTF-8` |
| 出力文字コード | 出力 CSV の文字コード | 入力と同じ（3引数時）/ `UTF-8` |
| 付加文字 | 出力ファイル名に付ける文字列 | `_extracted` |

### 出力ファイル名

```
（入力ファイル名）+（付加文字）+.csv
```

例: `data.csv` をドロップし付加文字が `_extracted` のとき → `data_extracted.csv`  
（入力ファイルと同じフォルダに出力）

### リンク先例

```bat
REM 列だけ指定（文字コード UTF-8、出力名 *_extracted.csv）
"C:\path\to\ExtractCsvColumns.bat" "name,email"

REM 文字コードも指定
"C:\path\to\ExtractCsvColumns.bat" "name,email" MS932 UTF-8

REM 出力名の付加文字も指定（data.csv → data_cut.csv）
"C:\path\to\ExtractCsvColumns.bat" "name,email" MS932 UTF-8 _cut
```

処理後はキー入力待ちでコマンドプロンプトが開いたままになります。

## 囲み文字・改行の扱い

入力例:

```csv
id,comment
1,"hello
world"
```

`comment` 列を切り出した出力:

```csv
comment
hello world
```
