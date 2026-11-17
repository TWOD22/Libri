package com.example.libri.util;

import android.content.Context;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Utilidades de interfaz.
 */
public final class UiUtils {

    private UiUtils() {
        // Clase de utilidades: no se instancia
    }

    /**
     * En Android 15+ la app se dibuja debajo de la barra de estado y la barra de gestos.
     * Este método agrega relleno (padding) a la vista raíz para que el contenido
     * no quede tapado por esas barras ni por la muesca de la cámara.
     *
     * @param root       vista raíz de la pantalla
     * @param includeIme true para considerar también el teclado (útil en formularios)
     */
    public static void applyWindowInsets(View root, boolean includeIme) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            int types = WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout();
            if (includeIme) {
                types |= WindowInsetsCompat.Type.ime();
            }
            Insets bars = windowInsets.getInsets(types);
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    /** Convierte dp (unidad independiente de la pantalla) a píxeles. */
    public static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
