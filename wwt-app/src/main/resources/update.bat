@echo off
echo 正在获取管理员权限
if exist "%SystemRoot%\SysWOW64" path %path%;%windir%\SysNative;%SystemRoot%\SysWOW64;%~dp0
bcdedit >nul
if '%errorlevel%' NEQ '0' (goto UACPrompt) else (goto UACAdmin)

:UACPrompt
%1 start "" mshta vbscript:createobject("shell.application").shellexecute("""%~0""","::",,"runas",1)(window.close)&exit
exit /B

:UACAdmin
cd /d "%~dp0"
echo 当前路径为：%CD%
echo 获取管理员权限中...
setlocal

rem 强制结束 WutheringWavesTool.exe 进程
taskkill /IM "WutheringWavesTool.exe" /F

rem 等待 2 秒
timeout /t 2 /nobreak > nul

rem 获取路径并移除末尾反斜杠
set "baseDir=%~dp0"
set "sourceDir=%baseDir%update"
set "targetDir=%~dp0"
set "targetDir=%targetDir:~0,-1%"  & rem 修复点：去掉末尾的\
echo 检测到助手安装目录="%targetDir%"

rem 更新逻辑
set "updateSuccess=false"
if exist "%sourceDir%" (
    echo 开始更新。。。
    robocopy "%sourceDir%" "%targetDir%" /MOVE /E /V /R:0 /W:0
    if %errorlevel% LEQ 1 (
        set "updateSuccess=true"
        echo 更新成功！
    ) else (
        echo 更新失败，错误代码：%errorlevel%
    )
) else (
    echo 更新失败，"%sourceDir%" 不存在
)

rem 获取更新文件的路径
set "baseDir=%~dp0"
set "sourceDir=%baseDir%update"
set "targetDir=%~dp0"
echo 检测到助手安装目录=%targetDir%


if exist "%baseDir%update.zip" (
    del "%baseDir%update.zip"
)

rem 删除更新缓存目录
if exist "%sourceDir%" (
    echo 正在删除 "%sourceDir%" 及其所有子文件和子目录...
    rmdir /S /Q "%sourceDir%"
    if exist "%sourceDir%" (
        echo 删除失败，请检查权限或文件占用！
    ) else (
        echo 成功删除 "%sourceDir%"
    )
) else (
    echo "%sourceDir%" 不存在，无需删除。
)

echo baseDir: %~dp0
echo 可执行文件路径: "%baseDir%WutheringWavesTool.exe"

rem 启动 WutheringWavesTool.exe（仅在更新成功时执行）
if "%updateSuccess%"=="true" (
    if exist "%baseDir%WutheringWavesTool.exe" (
        echo 正在启动 WutheringWavesTool.exe...
        start "" "%baseDir%WutheringWavesTool.exe"
    ) else (
        echo WutheringWavesTool.exe 不存在于 %baseDir%
    )
) else (
    echo 由于更新失败，未启动 WutheringWavesTool.exe
)

rem 不再自动删除脚本，方便用户查看日志
echo 更新流程结束，请按任意键退出...
pause > nul
endlocal
exit