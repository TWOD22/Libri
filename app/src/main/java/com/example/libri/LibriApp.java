package com.example.libri;

import android.app.Application;

import com.example.libri.util.ThemePrefs;

/**
 * Clase que Android crea una sola vez al abrir la aplicación.
 * Aquí se aplica el tema elegido (automático, claro u oscuro) antes de mostrar cualquier pantalla.
 */
public class LibriApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ThemePrefs.apply(this);
    }
}
