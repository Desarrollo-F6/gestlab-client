package com.f6.socket;

import java.io.IOException;

import org.json.JSONObject;
import org.tinylog.Logger;

import com.f6.functions.Functions;

import io.socket.client.Socket;

/** Registra los listeners de eventos del servidor Notilab. */
public class SocketHandlers {

    static final String EVT_PC_CHECK = "pcCheck";
    static final String EVT_APAGADO = "pcApagadoObjetivo";
    static final String EVT_REINICIO = "pcReinicioObjetivo";
    static final String EVT_BLOQUEO = "pcBloqueoObjetivo";
    static final String EVT_COMANDO = "pcComandoObjetivo";

    private final Functions functions;
    private final String machineName;

    public SocketHandlers(Functions functions, String machineName) {
        this.functions = functions;
        this.machineName = machineName;
    }

    public void register(Socket socket) {
        socket.on(Socket.EVENT_CONNECT, args -> {
            Logger.info("Conectado al servidor ws, enviando {}", EVT_PC_CHECK);
            JSONObject data = new JSONObject();
            data.put("origen", machineName);
            socket.emit(EVT_PC_CHECK, data);
        });
        socket.on(Socket.EVENT_DISCONNECT, args -> Logger.warn("Desconectado del servidor ws"));

        socket.on(EVT_APAGADO, args -> runSafely(EVT_APAGADO, functions::apagarComputadora));
        socket.on(EVT_REINICIO, args -> runSafely(EVT_REINICIO, functions::reiniciarComputadora));
        socket.on(EVT_BLOQUEO, args -> runSafely(EVT_BLOQUEO, functions::bloquearComputadora));
        socket.on(EVT_COMANDO, args -> runSafely(EVT_COMANDO,
                () -> functions.ejecutarComandoPowershell(firstString(args))));
    }

    private static String firstString(Object[] args) {
        return args.length > 0 && args[0] instanceof String ? (String) args[0] : null;
    }

    private static void runSafely(String event, IoAction action) {
        try {
            action.run();
        } catch (IOException | RuntimeException e) {
            Logger.error(e, "Error procesando evento {}", event);
        }
    }

    @FunctionalInterface
    interface IoAction {
        void run() throws IOException;
    }
}
