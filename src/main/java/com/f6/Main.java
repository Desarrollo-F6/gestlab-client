package com.f6;

import com.f6.functions.Functions;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.ls.LSOutput;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    public static void main(String[] args) {
        try {

            FileHandler fileHandler = new FileHandler("app.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);


            if (!SystemTray.isSupported()) {
                JOptionPane.showMessageDialog(null, "SystemTray is not supported", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            SystemTray tray = SystemTray.getSystemTray();
            Image image = Toolkit.getDefaultToolkit().createImage("icon.png"); // Replace with your icon path
            TrayIcon trayIcon = new TrayIcon(image, "Order Computer Listener");
            trayIcon.setImageAutoSize(true);
            trayIcon.setToolTip("Waiting for order-computer event");
            tray.add(trayIcon);
            logger.info("Activado el SystemTray");

            //Cambiar segun se necesite
//           final Socket socket = IO.socket("https://notilab-ws.urbe.edu/"); //websocket produccion
            final Socket socket = IO.socket("http://10.200.20.83:3002"); //websocket local f6



            logger.info("Conexion exitosa: " + socket.io());

            // Se obtiene el nombre del pc y se guarda en un json
            String machineName = InetAddress.getLocalHost().getHostName();
            JSONObject json = new JSONObject();
            json.put("name", machineName);


            logger.info("Check-in event emitted with machine name: " + machineName);



            socket.on(socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... objects) {
                    System.out.println("Intentando conectar al servidor ws");
                    JSONObject data = new JSONObject();
                    try{
                        data.put("origen", machineName);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    socket.emit("pcCheck", data);
                }
            });

            socket.connect();
        }catch (IOException e) {
            logger.severe("Error setting up logger: " + e.getMessage());
            e.printStackTrace();
        }catch (Exception e) {
            logger.severe("Error in main: " + e.getMessage());
            e.printStackTrace();
        }
    }
}