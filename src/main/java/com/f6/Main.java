package com.f6;

import com.f6.functions.Functions;
import com.f6.functions.Lector;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.InetAddress;
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
           final Socket socket = IO.socket("https://notilab.urbe.edu"); //websocket produccion
            // final Socket socket = IO.socket("http://localhost:3000"); //websocket local f6 pc desarrollo
//            final Socket socket = IO.socket("https://qr6b5gw0-3000.use2.devtunnels.ms/"); //Websocket llamado desde el cliente



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

            // * Evento para registrar la huella
            socket.on("escaneoHuella", new Emitter.Listener() {
                @Override
                public void call(Object... objects) {
                    try {
                        Object data = objects[0];
                        String idUsuario = "";

                        if (data instanceof JSONObject) {
                            idUsuario = String.valueOf(((JSONObject) data).get("idUsuario"));
                        } else if (data instanceof java.util.Map) {
                            idUsuario = String.valueOf(((java.util.Map) data).get("idUsuario"));
                        } else {
                            idUsuario = "desconocido";
                        }

                        logger.info("Escaneando para usuario ID: " + idUsuario);

                        String huella = Lector.scanFinger();
                        JSONObject response = new JSONObject();
                        response.put("idUsuario", idUsuario);
                        response.put("huella", huella);

                        // Validar de forma integral si la respuesta es un mensaje de fallo
                        if (huella == null || huella.isEmpty() || huella.startsWith("Error") || huella.startsWith("Timeout")) {
                            response.put("status", "ERROR");
                            logger.warning("Fallo en la captura de huella: " + huella);
                        } else {
                            response.put("status", "OK");
                            logger.info("Huella capturada correctamente para usuario " + idUsuario);
                        }

                        socket.emit("createHuellaPreparador", response);

                    } catch (Exception e) {
                        logger.severe("Error en el proceso de escaneo: " + e.getMessage());
                        JSONObject errorResponse = new JSONObject();
                        errorResponse.put("status", "ERROR");
                        errorResponse.put("huella", "Excepción: " + e.getMessage());
                        socket.emit("createHuellaPreparador", errorResponse);
                    }
                }
            });

            // * Evento de comparacion de huellas
            socket.on("comparacionHuellas", new Emitter.Listener() {
                @Override
                public void call(Object... objects) {
                    try {
                        logger.info("Inicio la comparacion de huellas");
                        JSONObject incomingData = (JSONObject) objects[0];

                        String idUsuario = String.valueOf(incomingData.get("idUsuario"));
                        String tipoUsuario = String.valueOf(incomingData.get("tipoUsuario"));
                        logger.info(idUsuario + " " + tipoUsuario );
                        JSONArray listaHuellas = incomingData.getJSONArray("huellas");

                        // Pasamos el array de la DB al método de verificación
                        String resultado = Lector.verifyFinger(listaHuellas);

                        // !!! ELIMINADO: Lector.CloseDevice(); <--- Ya no cerramos el hardware aquí

                        JSONObject response = new JSONObject();
                        response.put("idUsuario", idUsuario);
                        response.put("status", resultado);

                        if(tipoUsuario.equals("profesor")){
                            socket.emit("compararHuellaProfesor", response);
                        } else {
                            socket.emit("compararHuellaPreparador", response);
                        }

                    } catch (Exception e) {
                        logger.info("Error: " + e.getMessage());
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
