@ECHO OFF

SETLOCAL

REM ===========================================================================

SET SRC_DIR=src
SET BUILD_DIR=build
SET RES_DIR=res
SET DOC_DIR=doc
SET LIBS_DIR=libs

SET JVM_FLAGS=-ea -Xms1024m -Xmx1024m -XX:+AlwaysPreTouch -XX:+UseG1GC
SET COMPILER_FLAGS=-Xlint:all -Xlint:unchecked -Xdiags:verbose -Xmaxerrs 5 -g -encoding UTF8
SET ENTRY_POINT=Main
SET EXE_NAME=Seeker
SET LIBS=

REM ===========================================================================

SET ARGC=0
FOR %%x IN (%*) DO SET /A ARGC+=1
IF %ARGC% GTR 1 (
    ECHO Too many arguments! Try executing 'build help'.
    EXIT /B 1
)

IF "%1"==""         GOTO build
IF "%1"=="release"  GOTO release
IF "%1"=="help"     GOTO help
IF "%1"=="run"      GOTO run
IF "%1"=="doc"      GOTO doc
IF "%1"=="bytecode" GOTO bytecode
IF "%1"=="clean"    GOTO clean
IF "%1"=="info"     GOTO info
ECHO Bad argument! Try executing 'build help'.
EXIT /B 1

REM ===========================================================================

:help

ECHO build:          Compiles your source tree.
ECHO build release:  Compiles and builds an executable jar out of your source tree.
ECHO build run:      Runs the compiled output. It will run the executable jar if available.
ECHO build doc:      Generates javadoc out of your source tree.
ECHO build bytecode: Creates human readable versions of the compiled .class files.
ECHO build clean:    Deletes every directory and tmp file which has been created by this script.
ECHO build info:     Prints the version of the java tools which will be used by this script.
ECHO build help:     Prints this message.
EXIT /B 0

REM ===========================================================================

:info

ECHO JAVA:
java --version
ECHO.
ECHO JAVAC:
javac --version
ECHO.
ECHO JAR:
jar --version
ECHO.
ECHO JAVADOC:
javadoc --version
ECHO.
ECHO JAVAP:
javap -version

EXIT /B 0

REM ===========================================================================

:clean

IF EXIST %DOC_DIR% RMDIR /S /Q %DOC_DIR%
IF EXIST %BUILD_DIR% RMDIR /S /Q %BUILD_DIR%
IF EXIST sources.txt DEL /Q sources.txt
EXIT /B 0

REM ===========================================================================

:build

WHERE /Q javac
IF %ERRORLEVEL% NEQ 0 (
    ECHO You need to have the javac compiler executable available in your PATH variable.
    EXIT /B 1
)

IF NOT EXIST %SRC_DIR% (
    ECHO Specified src directory does not exist.
    EXIT /B 1
)

IF EXIST %BUILD_DIR% RMDIR /S /Q %BUILD_DIR%

IF EXIST %RES_DIR% (
    MKDIR %BUILD_DIR%\%RES_DIR%
    XCOPY /E %RES_DIR%\*.* %BUILD_DIR%\%RES_DIR% >nul 2>&1
)

IF EXIST %LIBS_DIR% (
    MKDIR %BUILD_DIR%\libs >nul 2>&1
    COPY %LIBS_DIR%\*.jar %BUILD_DIR%\libs >nul 2>&1
)

DIR /Q /S /B %SRC_DIR%\*.java > sources.txt
javac -cp %LIBS% %COMPILER_FLAGS% -d %BUILD_DIR% @sources.txt

IF %ERRORLEVEL% EQU 0 (
    ECHO Compilation successful.
    DEL /Q sources.txt
    EXIT /B 0
) ELSE (
    ECHO Compilation failed.
    DEL /Q sources.txt
    EXIT /B 2
)

REM ===========================================================================

:release

WHERE /Q javac
IF %ERRORLEVEL% NEQ 0 (
    ECHO You need to have the javac compiler executable available in your PATH variable.
    EXIT /B 1
)
WHERE /Q jar
IF %ERRORLEVEL% NEQ 0 (
    ECHO You need to have jar executable available in your PATH variable.
    EXIT /B 1
)
WHERE /Q powershell
IF %ERRORLEVEL% NEQ 0 (
    ECHO You need to have powershell.exe available in your PATH variable.
    EXIT /B 1
)

IF NOT EXIST %SRC_DIR% (
    ECHO Specified src directory does not exist.
    EXIT /B 1
)

IF EXIST %BUILD_DIR% RMDIR /S /Q %BUILD_DIR%

DIR /Q /S /B %SRC_DIR%\*.java > sources.txt
javac -cp %LIBS% %COMPILER_FLAGS% -d %BUILD_DIR% @sources.txt

IF %ERRORLEVEL% EQU 0 (

    IF EXIST %LIBS_DIR% (
        MKDIR %BUILD_DIR%\libs >nul 2>&1
        COPY %LIBS_DIR%\*.jar %BUILD_DIR%\libs >nul 2>&1
    )

    PUSHD %BUILD_DIR%

    ECHO Manifest-Version: 1.0 > MANIFEST.MF
    ECHO Main-Class: %ENTRY_POINT% >> MANIFEST.MF
    SET CLASS_PATH=%LIBS:;= %
    ECHO Class-Path: %CLASS_PATH% >> MANIFEST.MF

    powershell.exe "Get-ChildItem -Recurse *.class | Resolve-Path -Relative" > binaries.txt

    jar cfm %EXE_NAME%.jar MANIFEST.MF @binaries.txt
    DEL /Q binaries.txt

    POPD

    IF EXIST %RES_DIR% (
        MKDIR %BUILD_DIR%\%RES_DIR%
        XCOPY /E  %RES_DIR%\*.* %BUILD_DIR%\%RES_DIR% >nul 2>&1
    )

    ECHO Compilation successful.
    DEL /Q sources.txt
    EXIT /B 0
) ELSE (
    ECHO Compilation failed.
    DEL /Q sources.txt
    EXIT /B 2
)

REM ===========================================================================

:run

WHERE /Q java
IF %ERRORLEVEL% NEQ 0 (
    ECHO You need to have the java executable available in your PATH variable.
    EXIT /B 1
)

IF EXIST %BUILD_DIR%\%EXE_NAME%.jar (
    PUSHD %BUILD_DIR%
    java %JVM_FLAGS% -jar %EXE_NAME%.jar
    POPD
) ELSE (
    IF NOT EXIST %BUILD_DIR% (
        ECHO No binaries to execute found. Have you executed 'build' beforehand?
        EXIT /B 1
    ) ELSE (
        PUSHD %BUILD_DIR%
        java %JVM_FLAGS% %ENTRY_POINT%
        POPD
    )
)


EXIT/B 0

REM ===========================================================================

:doc

WHERE /Q javadoc
IF %ERRORLEVEL% NEQ 0 (
    ECHO You need to have the javadoc executable available in your PATH variable.
    EXIT /B 1
)

IF NOT EXIST %SRC_DIR% (
    ECHO Specified src directory does not exist.
    EXIT /B 1
)

IF NOT EXIST %DOC_DIR% MKDIR %DOC_DIR% >nul 2>&1
IF EXIST %DOC_DIR% RMDIR /S /Q %DOC_DIR%

DIR /Q /S /B %SRC_DIR%\*.java > sources.txt
IF "%LIBS%"=="" (
    javadoc -d %DOC_DIR% @sources.txt
) ELSE (
    javadoc -d %DOC_DIR% -cp %LIBS% @sources.txt
)

DEL /Q sources.txt
EXIT /B 0

REM ===========================================================================

:bytecode

WHERE /Q javap
IF %ERRORLEVEL% NEQ 0 (
    ECHO You need to have the javap executable available in your PATH variable.
    EXIT /B 1
)

IF NOT EXIST %BUILD_DIR% (
    ECHO No build directory available. Did you execute the 'build' command beforehand?
    EXIT /B 1
)

PUSHD %BUILD_DIR%
ECHO Generating human readable bytecode for:
FOR /R %%f IN (*.class) DO (
    ECHO    ^> %%f
    javap -l -p -c -v %%f > %%f.txt
)
POPD

EXIT /B 0

ENDLOCAL
