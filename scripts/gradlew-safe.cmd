@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0gradlew-safe.ps1" %*
