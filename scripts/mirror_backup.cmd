@echo off
rem KasiGuru off-site backup mirror:
rem   C:\KasiGuru\KasiGuruBackups  ->  OneDrive\KasiGuruBackups
rem Called from the KasiGuruBackup.cmd startup task after backup_firestore.js.
rem Robocopy exit codes 0-7 are success; never fail the caller chain.
rem /E, not /MIR: /MIR also deletes what is gone from the source, so anyone who emptied the local
rem backup folder emptied the off-site copy at the next run. New setups use the scheduled task from
rem scripts\register_backup_task.ps1, which copies and rotates through functions\backup_daily.js.
set SRC=C:\KasiGuru\KasiGuruBackups
set DST=C:\Users\U S E R - P C\OneDrive\KasiGuruBackups

if not exist "%SRC%" (
  echo [mirror] Source %SRC% missing - nothing to mirror.
  exit /b 0
)

robocopy "%SRC%" "%DST%" /E /R:2 /W:5 /NP >> "C:\KasiGuru\backup_task.log" 2>&1
set RC=%ERRORLEVEL%
if %RC% LSS 8 (
  echo [mirror] OK - KasiGuruBackups copied to OneDrive.
) else (
  echo [mirror] FAILED with code %RC%
)
exit /b 0
