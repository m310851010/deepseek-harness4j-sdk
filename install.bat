@echo off
rem ============================================================
rem  deepseek-harness4j-sdk 一键安装脚本
rem  功能: 使用 JDK 1.8 检查环境后执行 mvn clean install,
rem        将所有模块(sdk / spring-boot-starter / spring-boot-example)
rem        构建并安装到本地 Maven 仓库。
rem  用法: install.bat                    构建并安装到本地仓库(默认跳过测试)
rem        install.bat -DskipTests=false  透传 Maven 参数,例如重新启用测试
rem ============================================================
setlocal
set EXITCODE=0
chcp 65001 >nul
title deepseek-harness4j-sdk installer

cd /d "%~dp0"

echo ==================================================
echo   deepseek-harness4j-sdk 安装脚本
echo   groupId:    io.github.m310851010
echo   artifactId: deepseek-harness4j-sdk
echo ==================================================
echo.

rem ---------- 配置构建使用的 JDK 1.8 ----------
rem 默认使用本机 JDK 8 目录;如需更换,可设置环境变量 JDK8_HOME 指向其他 JDK 安装目录
set "SDK_JAVA_HOME=D:\Develop\Java\jdk1.8.0_40-b25_x64"
if defined JDK8_HOME set "SDK_JAVA_HOME=%JDK8_HOME%"
if exist "%SDK_JAVA_HOME%\bin\java.exe" (
    set "JAVA_HOME=%SDK_JAVA_HOME%"
    echo [信息] 使用 JDK: %SDK_JAVA_HOME%
) else (
    echo [提示] 未找到 JDK 目录 %SDK_JAVA_HOME%,将使用系统当前 Java 环境。
)
set "PATH=%JAVA_HOME%\bin;%PATH%"

rem ---------- 检查 Maven ----------
where mvn >nul 2>nul
if errorlevel 1 (
    echo [错误] 未找到 mvn 命令,请先安装 Apache Maven 并将其 bin 目录加入 PATH。
    echo        下载地址: https://maven.apache.org/download.cgi
    goto :fail
)
for /f "tokens=1,* delims=(" %%a in ('mvn -version 2^>nul ^| findstr /i /c:"Apache Maven"') do echo [信息] %%a(%%b

rem ---------- 检查 Java ----------
java -version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未找到 java 命令,请先安装 JDK 8 及以上版本并设置 JAVA_HOME。
    goto :fail
)
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do set JAVA_VER=%%v
echo [信息] Java 版本: %JAVA_VER%
echo.

rem ---------- 执行构建安装(额外参数透传给 Maven) ----------
echo [信息] 开始构建并安装到本地 Maven 仓库...
echo [信息] 执行命令: mvn clean install -DskipTests %*
echo.
call mvn clean install -DskipTests %*
if errorlevel 1 (
    echo.
    echo [错误] 构建失败,请检查上方 Maven 输出信息。
    goto :fail
)

echo.
echo ==================================================
echo   构建成功!所有模块已安装到本地 Maven 仓库:
echo   io.github.m310851010:deepseek-harness4j-sdk:0.0.1
echo ==================================================
goto :end

:fail
set EXITCODE=1

:end
rem 仅在双击运行时暂停窗口,便于查看结果;命令行 / CI 调用时不暂停
echo "%cmdcmdline%" | findstr /i /c:"%~f0" >nul 2>&1 && pause
exit /b %EXITCODE%
