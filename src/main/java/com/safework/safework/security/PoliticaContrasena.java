package com.safework.safework.security;

import java.nio.charset.StandardCharsets;

/** Política única para claves nuevas; las claves existentes siguen siendo válidas al entrar. */
public final class PoliticaContrasena {
    private PoliticaContrasena() {}
    public static void validar(String clave) {
        if (clave == null || clave.isBlank() || clave.codePointCount(0, clave.length()) < 12
                || clave.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("La contraseña debe tener al menos 12 caracteres y como máximo 72 bytes UTF-8.");
    }
}
