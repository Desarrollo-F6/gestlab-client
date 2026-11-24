package com.f6;

import java.awt.Image;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.net.InetAddress;
import java.net.URL;

import javax.swing.JOptionPane;

import org.tinylog.Logger;

import com.f6.config.AppConfig;
import com.f6.functions.CommandRunner;
import com.f6.functions.Functions;
import com.f6.socket.SocketHandlers;

import io.socket.client.IO;
import io.socket.client.Socket;


public class Main {

    public static void main(String[] args) {
        try {
            if (!installTrayIcon()) {
                return;
            }

            AppConfig config = AppConfig.load();
            String machineName = InetAddress.getLocalHost().getHostName();

            Socket socket = IO.socket(config.getServerUrl());
            SocketHandlers handlers = new SocketHandlers(
                    new Functions(CommandRunner.system()),
                    machineName);
            handlers.register(socket);

            socket.connect();
            Logger.info("Conectando a {} como {}", config.getServerUrl(), machineName);
        } catch (Exception e) {
            Logger.error(e, "Error en main");
        }
    }

    private static boolean installTrayIcon() throws Exception {
        if (!SystemTray.isSupported()) {
            Logger.error("SystemTray no soportado");
            JOptionPane.showMessageDialog(null, "SystemTray is not supported", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        URL iconUrl = Main.class.getResource("/icon.png");
        if (iconUrl == null) {
            Logger.warn("icon.png no encontrado en el classpath");
        }
        Image image = iconUrl != null
                ? Toolkit.getDefaultToolkit().createImage(iconUrl)
                : new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);

        TrayIcon trayIcon = new TrayIcon(image, "Gestlab Client");
        trayIcon.setImageAutoSize(true);
        trayIcon.setToolTip("Gestlab Client - esperando ordenes del servidor");
        SystemTray.getSystemTray().add(trayIcon);
        Logger.info("Activado el SystemTray");
        return true;
    }
}
