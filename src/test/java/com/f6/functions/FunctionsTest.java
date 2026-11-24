package com.f6.functions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FunctionsTest {

    @Mock
    private CommandRunner runner;

    @InjectMocks
    private Functions functions;

    @Test
    void apagaConShutdownForzado() throws IOException {
        functions.apagarComputadora();
        verify(runner).run("shutdown", "/s", "/f", "/t", "0");
    }

    @Test
    void reiniciaConShutdownForzado() throws IOException {
        functions.reiniciarComputadora();
        verify(runner).run("shutdown", "/r", "/f", "/t", "0");
    }

    @Test
    void bloqueaConLockWorkStation() throws IOException {
        functions.bloquearComputadora();
        verify(runner).run("rundll32.exe", "user32.dll,LockWorkStation");
    }

    @Test
    void pasaElComandoPowershellComoUnSoloArgumento() throws IOException {
        functions.ejecutarComandoPowershell("Get-Process | Select -First 1");
        verify(runner).run("powershell.exe", "-NoProfile", "-NonInteractive", "-Command",
                "Get-Process | Select -First 1");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void ignoraComandoVacio(String comando) throws IOException {
        functions.ejecutarComandoPowershell(comando);
        verifyNoInteractions(runner);
    }
}
