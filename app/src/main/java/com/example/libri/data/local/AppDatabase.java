package com.example.libri.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Base de datos local de Libri (Room sobre SQLite). Se crea una sola vez y se reutiliza.
 * Tablas: books (libros) y progress_log (registros de avance).
 */
@Database(entities = {BookEntity.class, ProgressLogEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract BookDao bookDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "libri.db").build();
                }
            }
        }
        return instance;
    }
}
