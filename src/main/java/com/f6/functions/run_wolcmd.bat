@echo off
rem Uso: run_wolcmd.bat <MAC> <IP>   (WolCmd.exe debe estar junto a este .bat)
"%~dp0WolCmd.exe" %1 %2 255.255.252.0 9
