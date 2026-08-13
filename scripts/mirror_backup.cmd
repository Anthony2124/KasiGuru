@echo off
rem KasiGuru off-site backup mirror:
rem   C:\KasiGuru\KasiGuruBackups  ->  OneDrive\KasiGuruBackups
rem Called from the KasiGuruBackup.cmd startup task after backup_firestore.js.
rem Robocopy exit codes 0-7 are success; never fail the caller chain.
set SRC=C:\KasiGuru\KasiGuruBackups
set DST=C:\Users\U S E R - P C\OneDrive\KasiGuruBackups

if not exist "%SRC%" (
  echo [mirror] Source %SRC% missing - nothing to mirror.
  exit /b 0
)

robocopy "%SRC%" "%DST%" /MIR /R:2 /W:5 /NP >> "C:\KasiGuru\backup_task.log" 2>&1
set RC=%ERRORLEVEL%
if %RC% LSS 8 (
  echo [mirror] OK - KasiGuruBackups mirrored to OneDrive.
) else (
  echo [mirror] FAILED with code %RC%
)
exit /b 0
