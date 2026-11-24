package com.f6.socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.f6.functions.Functions;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

@ExtendWith(MockitoExtension.class)
class SocketHandlersTest {

    @Mock
    private Socket socket;
    @Mock
    private Functions functions;

    private final Map<String, Emitter.Listener> listeners = new HashMap<>();

    @BeforeEach
    void setUp() {
        when(socket.on(any(String.class), any(Emitter.Listener.class))).thenAnswer(inv -> {
            listeners.put(inv.getArgument(0), inv.getArgument(1));
            return socket;
        });
        new SocketHandlers(functions, "LAB-PC-01").register(socket);
    }

    private void fire(String event, Object... args) {
        listeners.get(event).call(args);
    }

    @Test
    void alConectarEnviaPcCheckConElNombreDelEquipo() {
        fire(Socket.EVENT_CONNECT);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(socket).emit(eq(SocketHandlers.EVT_PC_CHECK), captor.capture());
        assertEquals("LAB-PC-01", ((JSONObject) captor.getValue()).getString("origen"));
    }

    @Test
    void delegaApagadoReinicioYBloqueo() throws IOException {
        fire(SocketHandlers.EVT_APAGADO);
        fire(SocketHandlers.EVT_REINICIO);
        fire(SocketHandlers.EVT_BLOQUEO);

        verify(functions).apagarComputadora();
        verify(functions).reiniciarComputadora();
        verify(functions).bloquearComputadora();
    }

    @Test
    void delegaComandoPowershell() throws IOException {
        fire(SocketHandlers.EVT_COMANDO, "Get-Date");

        verify(functions).ejecutarComandoPowershell("Get-Date");
    }

    @Test
    void comandoConPayloadNoStringPasaNull() throws IOException {
        fire(SocketHandlers.EVT_COMANDO, new JSONObject());

        verify(functions).ejecutarComandoPowershell(null);
    }

    @Test
    void unaIOExceptionNoTumbaElListener() throws IOException {
        doThrow(new IOException("boom")).when(functions).apagarComputadora();

        fire(SocketHandlers.EVT_APAGADO); // no debe lanzar
        verify(functions).apagarComputadora();
    }
}
