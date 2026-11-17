package com.example.libri.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Copia la imagen elegida en la galería al almacenamiento privado de la app.
 * Así la portada sigue existiendo aunque la foto original se borre de la galería.
 * La imagen se reduce (máximo 900 píxeles) para que no ocupe demasiado espacio.
 */
public final class ImageStorage {

    private static final int MAX_SIZE = 900;

    private ImageStorage() {
        // Clase de utilidades: no se instancia
    }

    /**
     * Guarda la imagen y devuelve la ruta del archivo creado.
     * Debe llamarse fuera del hilo principal.
     */
    public static String saveCover(Context context, Uri uri) throws IOException {
        // 1) Se lee solo el tamaño de la imagen para saber cuánto reducirla
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) {
                throw new IOException("No se pudo abrir la imagen");
            }
            BitmapFactory.decodeStream(in, null, bounds);
        }

        int sample = 1;
        while (bounds.outWidth / sample > MAX_SIZE * 2 || bounds.outHeight / sample > MAX_SIZE * 2) {
            sample *= 2;
        }

        // 2) Se carga la imagen ya reducida
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        Bitmap bitmap;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) {
                throw new IOException("No se pudo abrir la imagen");
            }
            bitmap = BitmapFactory.decodeStream(in, null, options);
        }
        if (bitmap == null) {
            throw new IOException("La imagen no es válida");
        }

        // 3) Se guarda como JPG dentro de la carpeta privada de la app
        File dir = new File(context.getFilesDir(), "covers");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta de portadas");
        }
        File file = new File(dir, "cover_" + System.currentTimeMillis() + ".jpg");
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out);
        }
        bitmap.recycle();
        return file.getAbsolutePath();
    }
}
