package com.example.libri.util;

import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.example.libri.R;

import java.io.File;

/**
 * Muestra la portada de un libro en un ImageView.
 * La ruta de la portada puede ser de tres tipos:
 *  - "asset:covers/x.png"  -> imagen incluida dentro de la app (libros de ejemplo)
 *  - "https://..."         -> portada descargada de internet (Open Library)
 *  - "/data/.../x.jpg"     -> imagen elegida de la galería (copiada al almacenamiento de la app)
 * Si no hay portada, se muestra una imagen por defecto.
 */
public final class CoverLoader {

    private CoverLoader() {
        // Clase de utilidades: no se instancia
    }

    public static void load(ImageView view, String coverPath) {
        if (coverPath == null || coverPath.isEmpty()) {
            view.setImageResource(R.drawable.cover_placeholder);
            return;
        }
        if (coverPath.startsWith("asset:")) {
            String uri = "file:///android_asset/" + coverPath.substring("asset:".length());
            Glide.with(view)
                    .load(uri)
                    .placeholder(R.drawable.cover_placeholder)
                    .error(R.drawable.cover_placeholder)
                    .into(view);
        } else if (coverPath.startsWith("http")) {
            Glide.with(view)
                    .load(coverPath)
                    .placeholder(R.drawable.cover_placeholder)
                    .error(R.drawable.cover_placeholder)
                    .into(view);
        } else {
            Glide.with(view)
                    .load(new File(coverPath))
                    .placeholder(R.drawable.cover_placeholder)
                    .error(R.drawable.cover_placeholder)
                    .into(view);
        }
    }
}
