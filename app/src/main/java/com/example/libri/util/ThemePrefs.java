package com.example.libri.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Guarda y aplica el tema de la aplicación: automático (como el celular), claro u oscuro.
 */
public final class ThemePrefs {

    public static final int MODE_AUTO = 0;
    public static final int MODE_LIGHT = 1;
    public static final int MODE_DARK = 2;

    private static final String PREFS_NAME = "libri_theme";
    private static final String KEY_MODE = "mode";

    private ThemePrefs() {
        // Clase de utilidades: no se instancia
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** Modo guardado (por defecto, automático). */
    public static int getMode(Context context) {
        return prefs(context).getInt(KEY_MODE, MODE_AUTO);
    }

    /** Guarda el modo elegido y lo aplica de inmediato (Android recrea la pantalla). */
    public static void setMode(Context context, int mode) {
        prefs(context).edit().putInt(KEY_MODE, mode).apply();
        applyMode(mode);
    }

    /** Aplica el modo guardado; se llama al abrir la aplicación. */
    public static void apply(Context context) {
        applyMode(getMode(context));
    }

    private static void applyMode(int mode) {
        if (mode == MODE_LIGHT) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (mode == MODE_DARK) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }
}
