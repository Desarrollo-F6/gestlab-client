package com.f6.functions;

import java.io.IOException;

import org.tinylog.Logger;

/** Acciones sobre el equipo local (apagado, reinicio, bloqueo, PowerShell). */
public class Functions {

    private final CommandRunner runner;

    public Functions(CommandRunner runner) {
        this.runner = runner;
    }

    // Apagado de computadora
    public void apagarComputadora() throws IOException {
        Logger.info("Apagando el equipo");
        runner.run("shutdown", "/s", "/f", "/t", "0");
    }

    // Reinicio de computadora
    public void reiniciarComputadora() throws IOException {
        Logger.info("Reiniciando el equipo");
        runner.run("shutdown", "/r", "/f", "/t", "0");
    }

    // Bloqueo de computadora
    public void bloquearComputadora() throws IOException {
        Logger.info("Bloqueando el equipo");
        runner.run("rundll32.exe", "user32.dll,LockWorkStation");
    }

    // Ejecutar Powershell
    public void ejecutarComandoPowershell(String comando) throws IOException {
        if (comando == null || comando.trim().isEmpty()) {
            Logger.warn("Comando PowerShell vacio, se ignora");
            return;
        }
        Logger.info("Ejecutando comando PowerShell remoto");
        runner.run("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", comando);
    }
}
