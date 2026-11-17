package com.example.libri.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Maneja el inicio de sesión de los usuarios de prueba.
 * No hay servidor: los usuarios están definidos aquí y la sesión se recuerda en SharedPreferences.
 * Cada usuario tiene su propia biblioteca (los libros se separan por el nombre de usuario).
 */
public final class SessionManager {

    // Usuarios de prueba (las tres listas van en el mismo orden)
    private static final String[] USERS = {"prueba", "prueba2"};
    private static final String[] PASSWORDS = {"1234", "1234"};
    private static final String[] DISPLAY_NAMES = {"Prueba", "Prueba 2"};

    private static final String PREFS_NAME = "libri_session";
    private static final String KEY_USER = "current_user";

    private SessionManager() {
        // Clase de utilidades: no se instancia
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** Valida usuario y contraseña; si son correctos guarda la sesión y devuelve true. */
    public static boolean login(Context context, String user, String password) {
        String name = user.trim().toLowerCase();
        for (int i = 0; i < USERS.length; i++) {
            if (USERS[i].equals(name) && PASSWORDS[i].equals(password)) {
                prefs(context).edit().putString(KEY_USER, USERS[i]).apply();
                return true;
            }
        }
        return false;
    }

    /** true si ya hay una sesión iniciada. */
    public static boolean isLoggedIn(Context context) {
        return prefs(context).getString(KEY_USER, null) != null;
    }

    /** Nombre de usuario de la sesión actual (clave que separa las bibliotecas). */
    public static String getUserId(Context context) {
        String user = prefs(context).getString(KEY_USER, null);
        return user == null ? USERS[0] : user;
    }

    /** Nombre para mostrar del usuario actual. */
    public static String getDisplayName(Context context) {
        String user = getUserId(context);
        for (int i = 0; i < USERS.length; i++) {
            if (USERS[i].equals(user)) {
                return DISPLAY_NAMES[i];
            }
        }
        return DISPLAY_NAMES[0];
    }

    /** Cierra la sesión. */
    public static void logout(Context context) {
        prefs(context).edit().remove(KEY_USER).apply();
    }
}
