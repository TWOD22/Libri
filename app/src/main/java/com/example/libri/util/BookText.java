package com.example.libri.util;

import android.content.Context;

import com.example.libri.R;
import com.example.libri.data.local.BookEntity;

/**
 * Textos listos para mostrar (estado y avance de un libro). Así todas las pantallas dicen lo mismo.
 */
public final class BookText {

    private BookText() {
        // Clase de utilidades: no se instancia
    }

    /** "Quiero leer", "Leyendo" o "Terminado". */
    public static String statusLabel(Context context, String status) {
        if (BookEntity.STATUS_READING.equals(status)) {
            return context.getString(R.string.status_reading);
        }
        if (BookEntity.STATUS_DONE.equals(status)) {
            return context.getString(R.string.status_done);
        }
        return context.getString(R.string.status_want);
    }

    /** Ejemplo: "320 / 1056 págs · 30%" (por páginas) o "45%" (por porcentaje). */
    public static String progressText(Context context, BookEntity book) {
        if (book.isPages()) {
            return context.getString(R.string.progress_pages_fmt, book.current, book.totalPages, book.percent());
        }
        return context.getString(R.string.progress_percent_fmt, book.current);
    }

    /** "Autor" o un texto por defecto cuando no se escribió el autor. */
    public static String authorOrUnknown(Context context, BookEntity book) {
        if (book.author == null || book.author.trim().isEmpty()) {
            return context.getString(R.string.author_unknown);
        }
        return book.author;
    }
}
