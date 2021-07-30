@echo off
rem it seems %* did not preserve quotes, switch to this alternative method:

set args=%1
shift
:start
if [%1] == [] goto done
set args=%args% %1
shift
goto start
:done

.\bin\java.exe -jar least-blurred-image-1.0.0.jar %args%