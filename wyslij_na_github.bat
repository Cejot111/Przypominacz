@echo off
title KardioMed - Wyslij na GitHub i Zbuduj APK
chcp 65001 >nul
cd /d "%~dp0"

echo ====================================================================
echo             KardioMed - Wysylanie projektu na GitHub
echo ====================================================================
echo.
echo Twoj profil GitHub: https://github.com/Cejot111
echo.
echo Jesli jeszcze nie masz repozytorium na ten projekt, utworz je teraz:
echo   1. Otworz link w przegladarce: https://github.com/new
echo   2. Wpisz nazwe: kardiomed
echo   3. Pozostaw Publiczne i kliknij [Create repository]
echo.
echo ====================================================================
set /p REPONAME="Podaj nazwe repozytorium na Twoim GitHubie [domyslnie: przypominacz]: "
if "%REPONAME%"=="" set REPONAME=przypominacz

set REPO_URL=https://github.com/Cejot111/%REPONAME%.git
echo.
echo Repozytorium docelowe: %REPO_URL%
echo ====================================================================
echo.

if not exist .git (
    echo [1/4] Inicjalizacja repozytorium Git...
    git init
    git branch -M main
)

echo [2/4] Konfiguracja adresu zdalnego...
git remote remove origin 2>nul
git remote add origin %REPO_URL%

echo [3/4] Dodawanie plikow i tworzenie commita...
git add .
git commit -m "KardioMed v1.0.0 - Notatnik, inteligentna tabela cisnien z autouzupelnianiem i glosny budzik z wyborem dzwiekow"

echo [4/4] Wysylanie projektu na Twojego GitHuba...
git push -u origin main

if %errorlevel% equ 0 (
    echo.
    echo ====================================================================
    echo  SUKCES! Kod zostal wyslany na Twojego GitHuba!
    echo.
    echo  GitHub Actions automatycznie rozpoczal budowanie gotowego pliku APK!
    echo.
    echo  Gdzie pobrac plik APK na telefon:
    echo  1. Zakladka Actions:
    echo     https://github.com/Cejot111/%REPONAME%/actions
    echo.
    echo  2. Zakladka Releases (gotowa paczka do pobrania):
    echo     https://github.com/Cejot111/%REPONAME%/releases
    echo.
    echo  Gdy kompilacja sie zakonczy (okolo 2 min), plik KardioMed-v1.0.0.apk
    echo  bedzie gotowy do pobrania prosto na Twoj telefon.
    echo ====================================================================
) else (
    echo.
    echo ====================================================================
    echo  [UWAGA] Wystapil problem z wyslaniem.
    echo  Upewnij sie, ze:
    echo  1. Utworzyles puste repozytorium o nazwie '%REPONAME%' na https://github.com/new
    echo  2. Jesli wyskoczy okno logowania GitHub w Windowsie, zatwierdz autoryzacje.
    echo ====================================================================
)

echo.
pause
