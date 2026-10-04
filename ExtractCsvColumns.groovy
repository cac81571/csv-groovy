#!/usr/bin/env groovy
import java.nio.charset.Charset

/**
 * CSV から指定した列だけを切り出して出力する。
 *
 * 使い方:
 *   groovy ExtractCsvColumns.groovy input.csv
 *   groovy ExtractCsvColumns.groovy input.csv MS932
 *   groovy ExtractCsvColumns.groovy input.csv output.csv col1,col3,col5
 *   groovy ExtractCsvColumns.groovy input.csv output.csv 0,2,4
 *   groovy ExtractCsvColumns.groovy input.csv output.csv name,email MS932
 *   groovy ExtractCsvColumns.groovy input.csv output.csv name,email MS932 UTF-8
 *
 * 列指定なし（または空）のときは利用可能な列を表示して終了する。
 * 列指定はヘッダー名（カンマ区切り）または 0 始まりのインデックス。
 * ヘッダー行あり前提。出力にもヘッダーを含める。
 *
 * 囲み文字（"）内の改行はレコードの区切りとみなさない。
 * 出力時はフィールド内の改行をスペースに置換し、1行にまとめる。
 *
 * 文字コード（省略時は UTF-8）:
 *   列一覧時: 第2引数 = 入力の文字コード
 *   切り出し時: 第4引数 = 入力 / 第5引数 = 出力（省略時は入力と同じ）
 *   例: UTF-8, MS932, Shift_JIS, Windows-31J, EUC-JP
 */

if (args.length < 1) {
    System.err.println('''使い方:
  groovy ExtractCsvColumns.groovy <入力CSV> [入力文字コード]
  groovy ExtractCsvColumns.groovy <入力CSV> <出力CSV> <列指定> [入力文字コード] [出力文字コード]

列指定なしの場合は利用可能な列を表示する。

列指定の例:
  name,age,email     … ヘッダー名
  0,2,4              … 列インデックス（0始まり）

文字コードの例:
  UTF-8（省略時の既定値）
  MS932 / Windows-31J / Shift_JIS
  EUC-JP
''')
    System.exit(1)
}

def inputPath = args[0]
def listOnly = args.length < 3 || !args[2]?.trim()
def outputPath = null
def columnSpec = null
def inputCharset = 'UTF-8'
def outputCharset = null

if (listOnly) {
    // 列一覧: input.csv [文字コード]
    // または input.csv output.csv / input.csv output.csv "" でも一覧表示
    if (args.length >= 4 && Charset.isSupported(args[3])) {
        inputCharset = args[3]
    } else if (args.length == 2 && Charset.isSupported(args[1])) {
        inputCharset = args[1]
    }
} else {
    outputPath = args[1]
    columnSpec = args[2]
    inputCharset = args.length >= 4 ? args[3] : 'UTF-8'
    outputCharset = args.length >= 5 ? args[4] : inputCharset
}

def inputFile = new File(inputPath)
if (!inputFile.exists()) {
    System.err.println("入力ファイルが見つかりません: ${inputPath}")
    System.exit(1)
}

def text = inputFile.getText(inputCharset)
if (text.startsWith('\uFEFF')) {
    text = text.substring(1)
}
if (!text?.trim()) {
    System.err.println('入力CSVが空です')
    System.exit(1)
}

/**
 * 囲み文字内の改行を含む CSV をレコード（フィールド配列）のリストにパースする。
 */
def parseCsvRecords = { String csv ->
    def records = []
    def fields = []
    def current = new StringBuilder()
    def inQuotes = false

    for (int i = 0; i < csv.length(); i++) {
        def c = csv.charAt(i)
        if (c == '"' as char) {
            if (inQuotes && i + 1 < csv.length() && csv.charAt(i + 1) == '"' as char) {
                current.append('"')
                i++
            } else {
                inQuotes = !inQuotes
            }
        } else if (c == ',' as char && !inQuotes) {
            fields << current.toString()
            current = new StringBuilder()
        } else if ((c == '\n' as char || c == '\r' as char) && !inQuotes) {
            if (c == '\r' as char && i + 1 < csv.length() && csv.charAt(i + 1) == '\n' as char) {
                i++
            }
            fields << current.toString()
            current = new StringBuilder()
            if (!(fields.size() == 1 && fields[0].isEmpty())) {
                records << fields
            }
            fields = []
        } else {
            current.append(c)
        }
    }

    if (current.length() > 0 || !fields.isEmpty()) {
        fields << current.toString()
        if (!(fields.size() == 1 && fields[0].isEmpty())) {
            records << fields
        }
    }

    records
}

/** フィールド内の改行をスペースに置換して1行化する */
def flattenNewlines = { String field ->
    field.replaceAll(/\r\n|\r|\n/, ' ')
}

def escapeCsvField = { String field ->
    def value = flattenNewlines(field)
    if (value.contains(',') || value.contains('"')) {
        return '"' + value.replace('"', '""') + '"'
    }
    value
}

def printAvailableColumns = { List headers ->
    println '利用可能な列:'
    headers.eachWithIndex { name, i ->
        println "${i}:${name}"
    }
}

def records = parseCsvRecords(text)
if (records.isEmpty()) {
    System.err.println('入力CSVが空です')
    System.exit(1)
}

def headers = records[0]

if (listOnly) {
    printAvailableColumns(headers)
    System.exit(0)
}

def requested = columnSpec.split(',').collect { it.trim() }.findAll { it }

if (requested.isEmpty()) {
    printAvailableColumns(headers)
    System.exit(0)
}

def indices = requested.collect { spec ->
    if (spec.isInteger()) {
        def idx = spec as int
        if (idx < 0 || idx >= headers.size()) {
            System.err.println("列インデックスが範囲外です: ${idx} (列数=${headers.size()})")
            printAvailableColumns(headers)
            System.exit(1)
        }
        return idx
    }
    def idx = headers.indexOf(spec)
    if (idx < 0) {
        System.err.println("列が見つかりません: ${spec}")
        printAvailableColumns(headers)
        System.exit(1)
    }
    idx
}

def dataRows = records.drop(1)
def output = new File(outputPath)
output.withWriter(outputCharset) { writer ->
    writer.println(indices.collect { escapeCsvField(headers[it]) }.join(','))
    dataRows.each { cols ->
        writer.println(indices.collect { i ->
            escapeCsvField(i < cols.size() ? cols[i] : '')
        }.join(','))
    }
}

println "完了: ${outputPath} （${indices.size()} 列 × ${dataRows.size()} 行） [${inputCharset} → ${outputCharset}]"
