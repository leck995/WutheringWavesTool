@echo off
echo ��ȡ����ԱȨ��
if exist "%SystemRoot%\SysWOW64" path %path%;%windir%\SysNative;%SystemRoot%\SysWOW64;%~dp0
bcdedit >nul
if '%errorlevel%' NEQ '0' (goto UACPrompt) else (goto UACAdmin)
:UACPrompt
%1 start "" mshta vbscript:createobject("shell.application").shellexecute("""%~0""","::",,"runas",1)(window.close)&exit
exit /B
:UACAdmin
cd /d "%~dp0"
echo ��ǰ����·���ǣ�%CD%
echo �ѻ�ȡ����ԱȨ��
setlocal

rem ɱ�� WutheringWavesTool.exe ����
taskkill /IM "WutheringWavesTool.exe" /F

:: �������������ҵ����̺ţ�Ȼ��ɱ����
set JPS=jps
set MAIN_CLASS=cn.tealc.wutheringwavestool.MainApplication

for /f "tokens=1" %%a in ('%JPS% -l ^| findstr /i "%MAIN_CLASS%"') do (
    set PID=%%a
)

if defined PID (
    echo Stopping %MAIN_CLASS% PID %PID%...
    taskkill /f /pid %PID%
    echo %MAIN_CLASS% is stopped.
) else (
    echo %MAIN_CLASS% is not running.
)

rem �ȴ� 1 ��
timeout /t 1 /nobreak > nul

rem ��ȡ������ű�����Ŀ¼
set "baseDir=%~dp0"
set "sourceDir=%baseDir%update"
set "targetDir=%baseDir%"

rem ���ԴĿ¼�Ƿ����
if exist "%sourceDir%" (
rem �����ļ���Ŀ¼��������
robocopy %sourceDir% %targetDir% /MOVE /E /V
)

if exist "%baseDir%update.zip" (
del "%baseDir%update.zip"
)
echo baseDir: %~dp0
echo Executable path: "%baseDir%WutheringWavesTool.exe"
rem ���� WutheringWavesTool.exe
if exist "%baseDir%WutheringWavesTool.exe" (
    echo Starting WutheringWavesTool.exe...
    start "" "%baseDir%WutheringWavesTool.exe"
) else (
    echo WutheringWavesTool.exe �������� %baseDir%
)
del "%~f0"
exit
endlocal
