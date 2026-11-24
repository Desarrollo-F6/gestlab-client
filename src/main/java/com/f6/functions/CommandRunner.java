package com.f6.functions;

import java.io.IOException;

/** Abstraccion sobre la ejecucion de procesos del SO, para poder mockearla en tests. */
public interface CommandRunner {

    void run(String... command) throws IOException;

    /** Implementacion real: lanza el proceso sin esperar a que termine. */
    static CommandRunner system() {
        return command -> new ProcessBuilder(command).inheritIO().start();
    }
}
