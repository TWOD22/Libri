package com.example.libri.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Tabla "books": un libro de la biblioteca de un usuario.
 * Cada fila pertenece a un usuario (userId), por eso cada persona ve solo sus libros.
 */
@Entity(tableName = "books", indices = {@Index("userId")})
public class BookEntity {

    // Tipos de avance: por páginas físicas o por porcentaje (libro digital)
    public static final String TYPE_PAGES = "PAGES";
    public static final String TYPE_PERCENT = "PERCENT";

    // Estados del libro
    public static final String STATUS_WANT = "WANT";
    public static final String STATUS_READING = "READING";
    public static final String STATUS_DONE = "DONE";

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String userId = "";

    @NonNull
    public String title = "";

    public String author;

    public String genre;

    /** Portada: "asset:...", dirección de internet o ruta de un archivo (puede ser null). */
    public String coverPath;

    /** TYPE_PAGES o TYPE_PERCENT. */
    @NonNull
    public String progressType = TYPE_PAGES;

    /** Total de páginas (0 si no se sabe; en porcentaje es opcional). */
    public int totalPages;

    /** Avance actual: páginas leídas, o porcentaje (0 a 100) si el avance es por porcentaje. */
    public int current;

    /** STATUS_WANT, STATUS_READING o STATUS_DONE. */
    @NonNull
    public String status = STATUS_WANT;

    /** Fecha de inicio en milisegundos (0 = sin fecha). */
    public long startDate;

    /** Fecha en que se terminó en milisegundos (0 = sin fecha). */
    public long finishDate;

    /** Calificación en estrellas: 0 (sin calificar) a 5. */
    public int rating;

    /** Valor máximo del avance: total de páginas, o 100 si es por porcentaje. */
    public int maxValue() {
        return TYPE_PAGES.equals(progressType) ? totalPages : 100;
    }

    /** Porcentaje de avance (0 a 100) para mostrar en la barra. */
    public int percent() {
        int max = maxValue();
        if (max <= 0) {
            return 0;
        }
        return Math.min(100, Math.round(current * 100f / max));
    }

    /** true si el avance es por páginas. */
    public boolean isPages() {
        return TYPE_PAGES.equals(progressType);
    }
}
