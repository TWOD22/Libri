package com.example.libri.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * Consultas a la base de datos local. Room escribe el código de cada una a partir de estas anotaciones.
 * Las consultas que devuelven LiveData avisan solas a la pantalla cuando los datos cambian.
 */
@Dao
public interface BookDao {

    // ----- Libros -----

    @Insert
    // Inserta un libro y devuelve su id
    long insert(BookEntity book);

    @Update
    // Actualiza un libro existente
    void update(BookEntity book);

    @Query("DELETE FROM books WHERE id = :id AND userId = :userId")
    void deleteBook(long id, String userId);

    @Query("SELECT * FROM books WHERE id = :id AND userId = :userId LIMIT 1")
    BookEntity getBook(long id, String userId);

    @Query("SELECT * FROM books WHERE id = :id AND userId = :userId LIMIT 1")
    LiveData<BookEntity> observeBook(long id, String userId);

    @Query("SELECT * FROM books WHERE userId = :userId ORDER BY title COLLATE NOCASE")
    // LiveData: la pantalla se actualiza sola cuando cambian los libros
    LiveData<List<BookEntity>> observeBooks(String userId);

    @Query("SELECT COUNT(*) FROM books WHERE userId = :userId")
    int countBooks(String userId);

    // ----- Registros de avance -----

    @Insert
    void insertLog(ProgressLogEntity log);

    @Query("DELETE FROM progress_log WHERE bookId = :bookId")
    void deleteLogsOfBook(long bookId);

    @Query("SELECT * FROM progress_log WHERE bookId = :bookId ORDER BY date DESC, id DESC")
    LiveData<List<ProgressLogEntity>> observeLogsOfBook(long bookId);

    @Query("SELECT * FROM progress_log WHERE userId = :userId")
    LiveData<List<ProgressLogEntity>> observeLogs(String userId);
}
