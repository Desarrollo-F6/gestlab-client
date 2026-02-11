package com.f6.functions;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import javax.swing.JOptionPane;

public class Functions {

    private static TrayIcon trayIcon;




    public Functions() {
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();
            Image image = Toolkit.getDefaultToolkit().createImage("icon.png"); // Replace with your icon path
            trayIcon = new TrayIcon(image, "Order Computer Listener");
            trayIcon.setImageAutoSize(true);
            trayIcon.setToolTip("Order Computer Listener");
            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                e.printStackTrace();
            }
        }
    }

    public void mostrarNotifiacionPrueba(String mensaje) throws Exception{
        if(SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();

            Image image = Toolkit.getDefaultToolkit().createImage("icon.png"); // Replace with your icon path
            TrayIcon trayIcon = new TrayIcon(image, "Mensaje Prueba");

            trayIcon.setImageAutoSize(true);
            trayIcon.setToolTip("Mensaje Prueba");
            tray.add(trayIcon);

            trayIcon.displayMessage("Mensaje Prueba", mensaje, TrayIcon.MessageType.INFO);

        }
    }





    // Method 1: Force shutdown this computer
    public static void apagarComputadora() throws IOException {
        Runtime.getRuntime().exec("shutdown /s /f /t 0");
    }

    // Method 2: Force restart this computer
    public static void reiniciarComputadora() throws IOException {
        Runtime.getRuntime().exec("shutdown /r /f /t 0");
    }

    // Method 3: Lock the computer as if using Windows + L
    public static void bloquearComputadora() throws IOException {
        Runtime.getRuntime().exec("rundll32.exe user32.dll,LockWorkStation");
    }

    // Method 4: Show a message using Windows notifications
    public static String mostrarMensaje(String mensaje) throws IOException {
        //String exePath = "C:\\Program Files\\GestlabClient\\app\\messageNotilab.exe";
        String exePath = "C:\\Program Files\\GestlabClient\\app\\messageNotilab.exe";
        Runtime.getRuntime().exec(exePath + " a asdasdasd");


        // Display the message using JOptionPane
        JOptionPane.showMessageDialog(null, mensaje, "Notification", JOptionPane.INFORMATION_MESSAGE);
        if (trayIcon != null) {
            JOptionPane.showMessageDialog(null, "NOSE", "Notification", JOptionPane.INFORMATION_MESSAGE);
            EventQueue.invokeLater(() -> trayIcon.displayMessage("Notification", mensaje, TrayIcon.MessageType.INFO));
        }
        return mensaje;
    }

    // Method 5: Execute a command in PowerShell
    public void ejecutarComandoPowershell(String comando) throws IOException {
        Runtime.getRuntime().exec("powershell.exe " + comando);
    }




    /*
    // Method 1: Force shutdown this computer
    public void apagarComputadora() throws IOException {
        Runtime.getRuntime().exec("shutdown /s /f /t 0");
    }

    // Method 2: Force restart this computer
    public void reiniciarComputadora() throws IOException {
        Runtime.getRuntime().exec("shutdown /r /f /t 0");
    }

    // Method 3: Lock the computer as if using Windows + L
    public void bloquearComputadora() throws IOException {
        Runtime.getRuntime().exec("rundll32.exe user32.dll,LockWorkStation");
    }


    */


}