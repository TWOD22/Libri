package com.example.libri.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.example.libri.data.local.AppDatabase;
import com.example.libri.data.local.BookDao;
import com.example.libri.data.local.BookEntity;
import com.example.libri.data.local.ProgressLogEntity;
import com.example.libri.data.remote.OpenLibraryApi;
import com.example.libri.data.remote.OpenLibraryModels;
import com.example.libri.util.DateUtils;
import com.example.libri.util.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Repositorio: es el único que sabe de dónde salen los datos.
 * Las pantallas le piden cosas (libros, avances, búsquedas) y él decide si las lee de la
 * base de datos local (Room) o las pide a internet (Open Library).
 * Todo lo que toca el disco se hace en un hilo aparte para no congelar la pantalla.
 */
public final class LibraryRepository {

    private static final String FIELDS =
            "key,title,author_name,cover_i,number_of_pages_median,first_publish_year,subject";

    private static volatile LibraryRepository instance;

    private final Context appContext;
    private final BookDao dao;
    private final ExecutorService diskIO = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private OpenLibraryApi api;

    private LibraryRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.dao = AppDatabase.getInstance(appContext).bookDao();
    }

    public static LibraryRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (LibraryRepository.class) {
                if (instance == null) {
                    instance = new LibraryRepository(context);
                }
            }
        }
        return instance;
    }

    /** Usuario que tiene la sesión iniciada. */
    private String currentUser() {
        return SessionManager.getUserId(appContext);
    }

    // =====================================================================
    // Lecturas (LiveData: la pantalla se actualiza sola cuando algo cambia)
    // =====================================================================

    public LiveData<List<BookEntity>> observeBooks() {
        return dao.observeBooks(currentUser());
    }

    public LiveData<List<ProgressLogEntity>> observeLogs() {
        return dao.observeLogs(currentUser());
    }

    public LiveData<BookEntity> observeBook(long id) {
        return dao.observeBook(id, currentUser());
    }

    public LiveData<List<ProgressLogEntity>> observeLogsOfBook(long id) {
        return dao.observeLogsOfBook(id);
    }

    /** Busca un libro por su id (por ejemplo, para llenar el formulario al editar). */
    public void getBook(long id, ResultCallback<BookEntity> callback) {
        String user = currentUser();
        diskIO.execute(() -> {
            BookEntity book = dao.getBook(id, user);
            mainHandler.post(() -> callback.onResult(book));
        });
    }

    // =====================================================================
    // Escrituras
    // =====================================================================

    /** Crea un libro nuevo (id = 0) o actualiza uno existente. Devuelve el id por el callback. */
    public void saveBook(BookEntity book, ResultCallback<Long> callback) {
        book.userId = currentUser();
        diskIO.execute(() -> {
            // Si el avance por páginas supera el total (se editó el total), se ajusta
            if (book.isPages() && book.totalPages > 0 && book.current > book.totalPages) {
                book.current = book.totalPages;
            }
            // El estado elegido decide las fechas y, si es "terminado", el avance completo
            applyStatus(book, book.status);

            long id;
            if (book.id == 0) {
                id = dao.insert(book);
            } else {
                dao.update(book);
                id = book.id;
            }
            if (callback != null) {
                mainHandler.post(() -> callback.onResult(id));
            }
        });
    }

    /** Borra un libro y todo su historial de avances. */
    public void deleteBook(long id) {
        String user = currentUser();
        diskIO.execute(() -> {
            dao.deleteLogsOfBook(id);
            dao.deleteBook(id, user);
        });
    }

    /** Guarda las estrellas (0 a 5) de un libro. */
    public void setRating(long id, int rating) {
        String user = currentUser();
        diskIO.execute(() -> {
            BookEntity book = dao.getBook(id, user);
            if (book != null) {
                book.rating = Math.max(0, Math.min(5, rating));
                dao.update(book);
            }
        });
    }

    /** Cambia el estado de un libro (quiero leer / leyendo / terminado). */
    public void setStatus(long id, String status) {
        String user = currentUser();
        diskIO.execute(() -> {
            BookEntity book = dao.getBook(id, user);
            if (book != null) {
                applyStatus(book, status);
                dao.update(book);
            }
        });
    }

    /**
     * Registra el avance de lectura: el usuario escribe en qué página (o porcentaje) va.
     * - Si el valor es mayor al actual: se guarda un registro en el historial.
     * - Si es menor: se toma como una corrección y solo se cambia el valor actual.
     * - Si llega al total: el libro pasa a "terminado" y la pantalla muestra el mensaje de logro.
     */
    public void addProgress(long id, int newValue, ResultCallback<ProgressResult> callback) {
        String user = currentUser();
        diskIO.execute(() -> {
            ProgressResult result = new ProgressResult();
            BookEntity book = dao.getBook(id, user);
            if (book == null) {
                mainHandler.post(() -> callback.onResult(result));
                return;
            }
            result.title = book.title;

            int max = book.maxValue();
            int value = Math.max(0, newValue);
            if (max > 0) {
                value = Math.min(value, max);
            }

            if (value > book.current) {
                int delta = value - book.current;
                ProgressLogEntity log = new ProgressLogEntity();
                log.bookId = book.id;
                log.userId = user;
                log.date = System.currentTimeMillis();
                log.delta = delta;
                log.valueAfter = value;
                log.pagesEquivalent = pagesFor(book, delta);
                dao.insertLog(log);

                book.current = value;
                if (max > 0 && value >= max) {
                    applyStatus(book, BookEntity.STATUS_DONE);
                    result.finished = true;
                } else {
                    applyStatus(book, BookEntity.STATUS_READING);
                }
                dao.update(book);
                result.changed = true;
            } else if (value < book.current) {
                // Corrección: no cuenta como lectura, solo se arregla el número
                book.current = value;
                if (BookEntity.STATUS_DONE.equals(book.status) && (max <= 0 || value < max)) {
                    applyStatus(book, BookEntity.STATUS_READING);
                }
                dao.update(book);
                result.corrected = true;
            }
            mainHandler.post(() -> callback.onResult(result));
        });
    }

    /** Páginas que representa un avance: directo si es por páginas, o calculado si es por porcentaje. */
    private int pagesFor(BookEntity book, int delta) {
        if (book.isPages()) {
            return delta;
        }
        if (book.totalPages > 0) {
            return Math.round(delta * book.totalPages / 100f);
        }
        return 0;
    }

    /** Pone el estado y arregla las fechas y el avance para que sean coherentes. */
    private void applyStatus(BookEntity book, String status) {
        long now = System.currentTimeMillis();
        book.status = status;
        if (BookEntity.STATUS_DONE.equals(status)) {
            int max = book.maxValue();
            if (max > 0) {
                book.current = max;
            }
            if (book.startDate == 0) {
                book.startDate = now;
            }
            if (book.finishDate == 0) {
                book.finishDate = now;
            }
        } else {
            book.finishDate = 0;
            if (BookEntity.STATUS_READING.equals(status) && book.startDate == 0) {
                book.startDate = now;
            }
        }
    }

    // =====================================================================
    // Búsqueda en internet (Open Library)
    // =====================================================================

    private OpenLibraryApi getApi() {
        if (api == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    // Open Library pide identificar la aplicación con un User-Agent
                    .addInterceptor(chain -> chain.proceed(chain.request().newBuilder()
                            .header("User-Agent", "Libri/1.0 (app academica de lectura)")
                            .build()))
                    .build();
            api = new Retrofit.Builder()
                    .baseUrl("https://openlibrary.org/")
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(OpenLibraryApi.class);
        }
        return api;
    }

    /**
     * Busca libros por título o autor. Los callbacks se ejecutan en el hilo principal.
     * onError recibe un mensaje listo para mostrar al usuario.
     */
    public void searchOnline(String query, ResultCallback<List<BookSuggestion>> onOk,
                             ResultCallback<String> onError) {
        getApi().search(query, 20, FIELDS).enqueue(new Callback<OpenLibraryModels.SearchResponse>() {
            @Override
            public void onResponse(Call<OpenLibraryModels.SearchResponse> call,
                                   Response<OpenLibraryModels.SearchResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    onError.onResult("No se pudo consultar Open Library. Intenta de nuevo.");
                    return;
                }
                onOk.onResult(toSuggestions(response.body()));
            }

            @Override
            public void onFailure(Call<OpenLibraryModels.SearchResponse> call, Throwable t) {
                onError.onResult("Sin conexión a internet. Revisa tu red e intenta de nuevo.");
            }
        });
    }

    /** Convierte la respuesta de la API en la lista simple que usa la pantalla. */
    private List<BookSuggestion> toSuggestions(OpenLibraryModels.SearchResponse body) {
        List<BookSuggestion> list = new ArrayList<>();
        if (body.docs == null) {
            return list;
        }
        for (OpenLibraryModels.Doc doc : body.docs) {
            if (doc.title == null || doc.title.trim().isEmpty()) {
                continue;
            }
            BookSuggestion s = new BookSuggestion();
            s.title = doc.title.trim();
            s.author = (doc.authorName != null && !doc.authorName.isEmpty()) ? doc.authorName.get(0) : "";
            s.pages = doc.pages != null ? doc.pages : 0;
            s.year = doc.year != null ? doc.year : 0;
            s.genre = pickGenre(doc.subject);
            s.coverUrl = doc.coverId != null
                    ? "https://covers.openlibrary.org/b/id/" + doc.coverId + "-M.jpg"
                    : null;
            list.add(s);
        }
        return list;
    }

    /** Elige un tema corto de la lista de temas como género (el usuario lo puede cambiar). */
    private String pickGenre(List<String> subjects) {
        if (subjects == null || subjects.isEmpty()) {
            return "";
        }
        for (String subject : subjects) {
            if (subject != null && subject.length() <= 24) {
                return subject;
            }
        }
        String first = subjects.get(0);
        return first == null ? "" : first.substring(0, Math.min(24, first.length()));
    }

    // =====================================================================
    // Libros de ejemplo (para poder probar la app sin internet)
    // =====================================================================

    /** La primera vez que entra un usuario, se le cargan 4 libros de ejemplo con su historial. */
    public void seedIfNeeded() {
        String user = currentUser();
        SharedPreferences prefs = appContext.getSharedPreferences("libri_seed", Context.MODE_PRIVATE);
        String key = "seeded_" + user;
        if (prefs.getBoolean(key, false)) {
            return;
        }
        diskIO.execute(() -> {
            if (dao.countBooks(user) == 0) {
                // Cada usuario de prueba recibe libros distintos
                if ("prueba2".equals(user)) {
                    insertSamplesUser2(user);
                } else {
                    insertSamples(user);
                }
            }
            prefs.edit().putBoolean(key, true).apply();
        });
    }

    private void insertSamples(String user) {
        // 1) Leyendo: Don Quijote (por páginas) con avance repartido en varios días
        long quijote = addSample(user, "Don Quijote de la Mancha", "Miguel de Cervantes", "Novela",
                "asset:covers/quijote.png", BookEntity.TYPE_PAGES, 1056, 320,
                BookEntity.STATUS_READING, 10, 0, 0);
        addLog(user, quijote, 9, 60, 60, 60);
        addLog(user, quijote, 8, 50, 110, 50);
        addLog(user, quijote, 6, 40, 150, 40);
        addLog(user, quijote, 5, 45, 195, 45);
        addLog(user, quijote, 3, 35, 230, 35);
        addLog(user, quijote, 2, 40, 270, 40);
        addLog(user, quijote, 1, 50, 320, 50);

        // 2) Leyendo: Orgullo y prejuicio (por porcentaje, libro digital)
        long orgullo = addSample(user, "Orgullo y prejuicio", "Jane Austen", "Romance",
                "asset:covers/orgullo.png", BookEntity.TYPE_PERCENT, 432, 45,
                BookEntity.STATUS_READING, 4, 0, 0);
        addLog(user, orgullo, 3, 10, 10, 43);
        addLog(user, orgullo, 2, 15, 25, 65);
        addLog(user, orgullo, 1, 20, 45, 86);

        // 3) Terminado: Frankenstein, con 5 estrellas
        long franken = addSample(user, "Frankenstein", "Mary Shelley", "Terror",
                "asset:covers/frankenstein.png", BookEntity.TYPE_PAGES, 280, 280,
                BookEntity.STATUS_DONE, 28, 25, 5);
        addLog(user, franken, 28, 70, 70, 70);
        addLog(user, franken, 27, 70, 140, 70);
        addLog(user, franken, 26, 60, 200, 60);
        addLog(user, franken, 25, 80, 280, 80);

        // 4) Quiero leer: El retrato de Dorian Gray
        addSample(user, "El retrato de Dorian Gray", "Oscar Wilde", "Novela",
                "asset:covers/dorian.png", BookEntity.TYPE_PERCENT, 0, 0,
                BookEntity.STATUS_WANT, 0, 0, 0);
    }

    /** Libros de ejemplo del segundo usuario (prueba2): distintos a los de prueba. */
    private void insertSamplesUser2(String user) {
        // 1) Leyendo: Cien años de soledad (por páginas)
        long cien = addSample(user, "Cien años de soledad", "Gabriel García Márquez", "Realismo mágico",
                "asset:covers/cien_anos.png", BookEntity.TYPE_PAGES, 471, 188,
                BookEntity.STATUS_READING, 7, 0, 0);
        addLog(user, cien, 6, 40, 40, 40);
        addLog(user, cien, 5, 35, 75, 35);
        addLog(user, cien, 4, 30, 105, 30);
        addLog(user, cien, 2, 45, 150, 45);
        addLog(user, cien, 0, 38, 188, 38);

        // 2) Leyendo: Crimen y castigo (por porcentaje, libro digital)
        long crimen = addSample(user, "Crimen y castigo", "Fiódor Dostoyevski", "Novela",
                "asset:covers/crimen.png", BookEntity.TYPE_PERCENT, 671, 22,
                BookEntity.STATUS_READING, 3, 0, 0);
        addLog(user, crimen, 2, 8, 8, 54);
        addLog(user, crimen, 1, 14, 22, 94);

        // 3) Terminado: El principito, con 4 estrellas
        long principito = addSample(user, "El principito", "Antoine de Saint-Exupéry", "Fábula",
                "asset:covers/principito.png", BookEntity.TYPE_PAGES, 96, 96,
                BookEntity.STATUS_DONE, 15, 12, 4);
        addLog(user, principito, 15, 50, 50, 50);
        addLog(user, principito, 12, 46, 96, 46);

        // 4) Quiero leer: Drácula
        addSample(user, "Drácula", "Bram Stoker", "Terror",
                "asset:covers/dracula.png", BookEntity.TYPE_PAGES, 418, 0,
                BookEntity.STATUS_WANT, 0, 0, 0);
    }

    private long addSample(String user, String title, String author, String genre, String cover,
                           String type, int total, int current, String status,
                           int startDaysAgo, int finishDaysAgo, int rating) {
        BookEntity b = new BookEntity();
        b.userId = user;
        b.title = title;
        b.author = author;
        b.genre = genre;
        b.coverPath = cover;
        b.progressType = type;
        b.totalPages = total;
        b.current = current;
        b.status = status;
        b.startDate = startDaysAgo > 0 ? DateUtils.millisDaysAgo(startDaysAgo) : 0;
        b.finishDate = BookEntity.STATUS_DONE.equals(status) ? DateUtils.millisDaysAgo(finishDaysAgo) : 0;
        b.rating = rating;
        return dao.insert(b);
    }

    private void addLog(String user, long bookId, int daysAgo, int delta, int valueAfter, int pages) {
        ProgressLogEntity log = new ProgressLogEntity();
        log.bookId = bookId;
        log.userId = user;
        log.date = DateUtils.millisDaysAgo(daysAgo);
        log.delta = delta;
        log.valueAfter = valueAfter;
        log.pagesEquivalent = pages;
        dao.insertLog(log);
    }
}
