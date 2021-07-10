@ECHO OFF

SETLOCAL

SET SOURCES=*.java *.form
SET SRC_DIR=src

IF NOT EXIST %SRC_DIR% (
    ECHO The specified src directory does not exist.
    ECHO Script will abort.
    EXIT /B 1
)

SETLOCAL ENABLEDELAYEDEXPANSION

SET /A minloc=-1

PUSHD %SRC_DIR%
FOR /R %%f IN (%SOURCES%) DO (

    FOR /F %%a IN ('TYPE "%%f"^|FIND /C /v  "" ') DO SET /A loc+=%%a&SET /A current=%%a
    ECHO    ^> %%f ^(!current!^)
    SET /A files+=1

    IF !current! GTR !maxloc! SET /A maxloc=!current!

    IF !minloc! EQU -1 SET /A minloc=!current!
    IF !current! LSS !minloc! SET /A minloc=!current!
)
SET /A avg=!loc!/!files!
POPD

ECHO.
ECHO Total amount of files  : !files!
ECHO Min lines of code      : !minloc!
ECHO Max lines of code      : !maxloc!
ECHO Average lines per file : !avg!
ECHO Total lines of code    : !loc!

SETLOCAL DISABLEDELAYEDEXPANSION

ENDLOCAL
