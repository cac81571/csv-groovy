@echo off
chcp 65001 >nul
cd /d "%~dp0"

if "%~1"=="" (
    echo ショートカットのリンク先例:
    echo   "%~f0" "name,email" MS932 UTF-8 _extracted
    echo.
    echo 引数: "列指定" [入力文字コード] [出力文字コード] [付加文字]
    echo 出力: 入力名 + 付加文字 + .csv  （省略時の付加文字は _extracted）
    echo CSV をショートカットにドラッグ＆ドロップして実行します。
    pause
    exit /b 1
)

REM 引数: "列指定" [入力文字コード] [出力文字コード] [付加文字]  ＋ 末尾に入力CSV
call :last %*
set "IN=%LAST%"
set "COLS=%~1"
set "ICS=UTF-8"
set "OCS=UTF-8"
set "SFX=_extracted"
if not "%~5"=="" (
    set "ICS=%~2"
    set "OCS=%~3"
    set "SFX=%~4"
) else if not "%~4"=="" (
    set "ICS=%~2"
    set "OCS=%~3"
) else if not "%~3"=="" (
    set "ICS=%~2"
    set "OCS=%~2"
)

for %%I in ("%IN%") do set "OUT=%%~dpnI%SFX%.csv"
call groovy "%~dp0ExtractCsvColumns.groovy" "%IN%" "%OUT%" "%COLS%" %ICS% %OCS%
echo.
pause
exit /b

:last
if "%~1"=="" exit /b
set "LAST=%~1"
shift
goto last
