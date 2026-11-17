package com.example.libri.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.libri.data.LibraryRepository;
import com.example.libri.data.ProgressResult;
import com.example.libri.data.ResultCallback;
import com.example.libri.data.local.BookEntity;
import com.example.libri.data.local.ProgressLogEntity;

import java.util.Calendar;
import java.util.List;

/**
 * ViewModel compartido por las pantallas. Expone la lista de libros y los registros de avance
 * del usuario y recuerda el estado de la interfaz (buscador, filtro y mes de estadísticas),
 * así no se pierde al girar la pantalla.
 */
public class LibraryViewModel extends AndroidViewModel {

    // Fuente de los datos (base de datos e internet)
    private final LibraryRepository repository;
    // Libros y registros que la interfaz observa
    private final LiveData<List<BookEntity>> books;
    private final LiveData<List<ProgressLogEntity>> logs;

    // Estado de la Biblioteca
    private String libraryQuery = "";
    private String libraryStatus = null; // null = todos

    // Mes que se muestra en Estadísticas
    private int statsYear;
    private int statsMonth;

    public LibraryViewModel(@NonNull Application application) {
        super(application);
        repository = LibraryRepository.getInstance(application);
        books = repository.observeBooks();
        logs = repository.observeLogs();
        // Al empezar se muestra el mes actual
        Calendar now = Calendar.getInstance();
        statsYear = now.get(Calendar.YEAR);
        statsMonth = now.get(Calendar.MONTH);
    }

    public LiveData<List<BookEntity>> getBooks() {
        return books;
    }

    public LiveData<List<ProgressLogEntity>> getLogs() {
        return logs;
    }

    public LiveData<BookEntity> observeBook(long id) {
        return repository.observeBook(id);
    }

    public LiveData<List<ProgressLogEntity>> observeLogsOfBook(long id) {
        return repository.observeLogsOfBook(id);
    }

    public void addProgress(long id, int value, ResultCallback<ProgressResult> callback) {
        repository.addProgress(id, value, callback);
    }

    public void setRating(long id, int rating) {
        repository.setRating(id, rating);
    }

    public void setStatus(long id, String status) {
        repository.setStatus(id, status);
    }

    public void deleteBook(long id) {
        repository.deleteBook(id);
    }

    // ----- Estado de la interfaz -----

    public String getLibraryQuery() {
        return libraryQuery;
    }

    public void setLibraryQuery(String query) {
        this.libraryQuery = query == null ? "" : query;
    }

    public String getLibraryStatus() {
        return libraryStatus;
    }

    public void setLibraryStatus(String status) {
        this.libraryStatus = status;
    }

    public int getStatsYear() {
        return statsYear;
    }

    public int getStatsMonth() {
        return statsMonth;
    }

    /** Mueve el mes de Estadísticas hacia adelante (+1) o hacia atrás (-1). */
    public void moveStatsMonth(int step) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(statsYear, statsMonth, 1);
        c.add(Calendar.MONTH, step);
        statsYear = c.get(Calendar.YEAR);
        statsMonth = c.get(Calendar.MONTH);
    }
}
