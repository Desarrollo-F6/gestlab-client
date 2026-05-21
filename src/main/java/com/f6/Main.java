package com.f6;

import com.f6.functions.Functions;
import com.f6.functions.Lector;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.ls.LSOutput;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
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
//           final Socket socket = IO.socket("https://notilab.urbe.edu"); //websocket produccion
//            final Socket socket = IO.socket("http://localhost:3000"); //websocket local f6 pc desarrollo
            final Socket socket = IO.socket("https://qr6b5gw0-3000.use2.devtunnels.ms/"); //Websocket llamado desde el cliente



            logger.info("Conexion exitosa: " + socket.io());

            // Se obtiene el nombre del pc y se guarda en un json
            String machineName = InetAddress.getLocalHost().getHostName();
            JSONObject json = new JSONObject();
            json.put("name", machineName);


            logger.info("Check-in event emitted with machine name: " + machineName);


            // * Evento pcCheck
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

            // * Evento Bloqueo Equipo
            socket.on("pcBloqueoObjetivo", new Emitter.Listener() {
                @Override
                public void call(Object... objects) {
                    try{
                        Functions.bloquearComputadora();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            // * Evento Reinicio Equipo
            socket.on("pcReinicioObjetivo", new Emitter.Listener() {
                @Override
                public void call(Object... objects) {
                    try{
                        Functions.reiniciarComputadora();
                    } catch (RuntimeException | IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            // * Evento Apagado Computadora
            socket.on("pcApagadoObjetivo", new Emitter.Listener() {
                @Override
                public void call(Object... objects) {
                    try{
                        Functions.apagarComputadora();
                    } catch (RuntimeException | IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            // * Evento Reinicio computadora
            socket.on("pcReinicioObjetivo", new Emitter.Listener() {
                @Override
                public void call(Object... objects) {
                    try{
                        Functions.reiniciarComputadora();
                    } catch (RuntimeException | IOException e){
                        e.printStackTrace();
                    }
                }
            });

            // * Evento Registrar una huella
            socket.on("escaneoHuella", new Emitter.Listener() {
                @Override
                public void call(Object... objects) {
                    try {
                        // NestJS envía los datos en el primer argumento
                        Object data = objects[0];
                        String idUsuario = "";

                        if (data instanceof JSONObject) {
                            // Si es JSONObject, extraemos con seguridad
                            idUsuario = String.valueOf(((JSONObject) data).get("idUsuario"));
                        } else if (data instanceof java.util.Map) {
                            // A veces Socket.io lo mapea automáticamente a un Map
                            idUsuario = String.valueOf(((java.util.Map) data).get("idUsuario"));
                        } else {
                            idUsuario = "desconocido";
                        }

                        logger.info("Escaneando para usuario ID: " + idUsuario);

                        // Realizar el escaneo síncrono[cite: 12]
                        String huella = Lector.scanFinger();
//                        System.out.println(huella);

                        if(huella.equalsIgnoreCase("Timeout: No se detectó huella")){
                            JSONObject response = new JSONObject();
                            response.put("idUsuario", idUsuario);
                            response.put("huella", huella);
                            response.put("status", "ERROR");
//                            logger.info(response.toString());
                            socket.emit("createHuellaPreparador", response);
//                            return;
                        }

                         else if(!huella.isEmpty()){
                            JSONObject response = new JSONObject();
                            response.put("idUsuario", idUsuario);
                            response.put("huella", huella);
                            response.put("status", "OK");
                            socket.emit("createHuellaPreparador", response);
                        }




                    } catch (Exception e) {
                        logger.info("Error en el proceso de escaneo: " + e.getMessage());
                        // Enviar error al servidor para que no se quede colgado
                        socket.emit("createHuellaPreparadorError", e.getMessage());
                    }
                }
            });

            // * Evento de comparacion de huellas
            socket.on("comparacionHuellas", new Emitter.Listener() {
                @Override
                public void call(Object... objects) {
                    try {
                        logger.info("Inicio la comparacion de huellas");
                        // Nest envía un objeto: { idUsuario: "...", huellas: [...] }
                        JSONObject incomingData = (JSONObject) objects[0];

//                        logger.info("incomingData: " + incomingData.toString());
                        String idUsuario = String.valueOf(incomingData.get("idUsuario"));
                        JSONArray listaHuellas = incomingData.getJSONArray("huellas");

//                        logger.info(listaHuellas.toString());
//                        logger.info("Comparando huellas para usuario: " + idUsuario);

                        // Pasamos el array de la DB al método de verificación
                        String resultado = Lector.verifyFinger(listaHuellas);
//                        logger.info("resultado: "+ resultado);
                        Lector.CloseDevice();

                        // DEVOLVEMOS UN OBJETO con el ID para que Nest no lo pierda
                        JSONObject response = new JSONObject();
                        response.put("idUsuario", idUsuario);
                        response.put("status", resultado);
//                        logger.info(response.toString());

                        socket.emit("compararHuellaPreparador", response);

                    } catch (Exception e) {
                        logger.info("Error: " + e.getMessage());
                        Lector.CloseDevice();
                        socket.emit("createHuellaPreparadorError", e.getMessage());
                    }
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
