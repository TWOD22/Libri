package com.example.libri.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Tabla "progress_log": cada vez que el usuario registra su avance se guarda una fila.
 * Con estas filas se arma el historial del libro, la racha y las estadísticas del mes.
 */
@Entity(tableName = "progress_log", indices = {@Index("userId"), @Index("bookId")})
public class ProgressLogEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long bookId;

    @NonNull
    public String userId = "";

    /** Momento del registro en milisegundos. */
    public long date;

    /** Cuánto se avanzó (páginas, o puntos de porcentaje). */
    public int delta;

    /** Cuántas páginas representa ese avance (0 si no se sabe). */
    public int pagesEquivalent;

    /** Avance total del libro después de este registro. */
    public int valueAfter;
}
