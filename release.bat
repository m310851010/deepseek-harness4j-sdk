@echo off
rem ============================================================
rem  deepseek-harness4j-sdk 一键发布脚本(Maven Central)
rem  流程: 环境自检 -> 设置版本号 -> mvn clean deploy -Pcentral-release
rem        -> 自动创建 git 标签 v<版本号>
rem  用法: release.bat 0.0.2 [-y]
rem        0.0.2  本次发布版本号(x.y.z,不允许 -SNAPSHOT)
rem        -y     跳过发布前确认
rem  前置: ~/.m2/settings.xml 已配置 id=central 的 Portal Token,
rem        本机已有 GPG 私钥且公钥已上传公钥服务器
rem  注意: Central 上版本永久占用不可重发,请勿重复发布同一版本!
rem ============================================================
setlocal
set EXITCODE=0
chcp 65001 >nul
title deepseek-harness4j-sdk release

cd /d "%~dp0"

rem ---------- 参数检查 ----------
set "VERSION=%~1"
if "%VERSION%"=="" (
    echo [错误] 用法: release.bat ^<版本号^> [-y]    例如: release.bat 0.0.2
    goto :fail
)
echo %VERSION%| findstr /r "^[0-9][0-9]*\.[0-9][0-9]*\.[0-9][0-9]*$" >nul
if errorlevel 1 (
    echo [错误] 版本号格式应为 x.y.z 且不带 -SNAPSHOT: %VERSION%
    goto :fail
)

rem ---------- 配置构建使用的 JDK 1.8(与 install.bat 一致,可用 JDK8_HOME 覆盖) ----------
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
    goto :fail
)
for /f "tokens=1,* delims=(" %%a in ('mvn -version 2^>nul ^| findstr /i /c:"Apache Maven"') do echo [信息] %%a(%%b

rem ---------- 检查 GPG(maven-gpg-plugin 通过 PATH 调用,需确保 gpg 可被找到) ----------
rem 确保 gpg 能定位到用户密钥库(MSYS 版 gpg 依赖 HOME 环境变量)
set "HOME=%USERPROFILE%"
set "GPG_FOUND="
where gpg >nul 2>nul && set "GPG_FOUND=1"
if not defined GPG_FOUND if exist "%ProgramFiles%\Git\usr\bin\gpg.exe" (
    set "PATH=%ProgramFiles%\Git\usr\bin;%PATH%"
    set "GPG_FOUND=1"
)
if not defined GPG_FOUND if exist "%ProgramFiles%\GnuPG\bin\gpg.exe" (
    set "PATH=%ProgramFiles%\GnuPG\bin;%PATH%"
    set "GPG_FOUND=1"
)
if not defined GPG_FOUND (
    echo [错误] 未找到 gpg 命令,请安装 GnuPG 或 Git for Windows。
    goto :fail
)
gpg --list-secret-keys 2>nul | findstr /c:"sec" >nul
if errorlevel 1 (
    echo [错误] 未找到 GPG 私钥,请先创建密钥并将公钥上传到 keyserver.ubuntu.com。
    goto :fail
)
for /f "tokens=1,2 delims=:" %%a in ('gpg --list-secret-keys --with-colons 2^>nul ^| findstr /b /c:"fpr:"') do echo [信息] GPG 密钥指纹: %%b

rem ---------- 检查 Central Portal Token ----------
if not exist "%USERPROFILE%\.m2\settings.xml" (
    echo [错误] 缺少 %USERPROFILE%\.m2\settings.xml,请先配置 Portal Token。
    goto :fail
)
findstr /c:"<id>central</id>" "%USERPROFILE%\.m2\settings.xml" >nul 2>nul
if errorlevel 1 (
    echo [错误] settings.xml 未配置 id=central 的服务器凭据。
    goto :fail
)
findstr /c:"<username>tokenUsername</username>" "%USERPROFILE%\.m2\settings.xml" >nul 2>nul
if not errorlevel 1 (
    echo [错误] settings.xml 中仍是占位 token,请到 central.sonatype.com 生成 User Token 后填入。
    goto :fail
)
echo [信息] Central Portal Token 已配置
echo.

rem ---------- 发布确认(版本号发布后永久占用、不可重发) ----------
if not /i "%~2"=="-y" (
    echo 即将发布版本 %VERSION% 到 Maven Central,发布后该版本号将永久占用、无法覆盖重发!
    choice /c YN /m "确认发布"
    if errorlevel 2 (
        echo 已取消发布。
        goto :end
    )
)
echo.

rem ---------- 设置版本号 ----------
echo [信息] 设置项目版本为 %VERSION% ...
call mvn -q versions:set -DnewVersion=%VERSION% -DgenerateBackupPoms=false
if errorlevel 1 (
    echo [错误] 版本号设置失败,请检查上方 Maven 输出。
    goto :fail
)

rem ---------- 发布 ----------
echo [信息] 开始发布: mvn clean deploy -Pcentral-release -DskipTests
echo.
call mvn clean deploy -Pcentral-release -DskipTests
if errorlevel 1 (
    echo.
    echo [错误] 发布失败,请检查上方 Maven 输出。
    goto :fail
)

rem ---------- 创建 git 标签 ----------
git rev-parse --is-inside-work-tree >nul 2>nul
if not errorlevel 1 (
    git rev-parse -q --verify "refs/tags/v%VERSION%" >nul 2>nul
    if errorlevel 1 (
        git tag "v%VERSION%"
        echo [信息] 已创建 git 标签 v%VERSION%,推送远端请执行: git push origin v%VERSION%
    ) else (
        echo [提示] git 标签 v%VERSION% 已存在,跳过。
    )
)

echo.
echo ==================================================
echo   发布成功!版本 %VERSION% 已上传 Maven Central
echo   坐标: io.github.m310851010:deepseek-harness4j-sdk:%VERSION%
echo   进度: https://central.sonatype.com/publishing/deployments
echo   校验通过后将自动上架(约 30 分钟~2 小时)
echo ==================================================
echo   [提醒] 文档与 install.bat 中的版本号需同步更新为 %VERSION%
goto :end

:fail
set EXITCODE=1

:end
rem 仅在双击运行时暂停窗口,便于查看结果;命令行 / CI 调用时不暂停
echo "%cmdcmdline%" | findstr /i /c:"%~f0" >nul 2>&1 && pause
exit /b %EXITCODE%
