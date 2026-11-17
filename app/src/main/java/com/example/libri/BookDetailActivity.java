package com.example.libri;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.libri.data.local.BookEntity;
import com.example.libri.data.local.ProgressLogEntity;
import com.example.libri.ui.LibraryViewModel;
import com.example.libri.ui.LogAdapter;
import com.example.libri.ui.ProgressDialogs;
import com.example.libri.ui.StarRatingView;
import com.example.libri.util.BookText;
import com.example.libri.util.CoverLoader;
import com.example.libri.util.DateUtils;
import com.example.libri.util.UiUtils;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;

/**
 * Detalle de un libro: portada, avance con barra, cambio de estado, calificación con estrellas
 * e historial de lectura. Desde aquí también se edita o se borra el libro.
 */
public class BookDetailActivity extends AppCompatActivity {

    public static final String EXTRA_BOOK_ID = "book_id";

    // ViewModel con los datos y las acciones sobre el libro
    private LibraryViewModel viewModel;
    // Identificador del libro que llega desde la pantalla anterior
    private long bookId;
    // Último dato recibido del libro
    private BookEntity book;

    // Vistas de la pantalla
    private ImageView ivCover;
    private TextView tvTitle;
    private TextView tvAuthor;
    private TextView tvGenre;
    private TextView tvProgress;
    private TextView tvDates;
    private TextView tvLogsEmpty;
    private LinearProgressIndicator progressBar;
    private MaterialButtonToggleGroup statusToggle;
    private StarRatingView stars;
    private LogAdapter logAdapter;

    // true mientras la app cambia los botones por código (para no confundirlo con un clic del usuario)
    private boolean updatingUi = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // El diseño cambia solo: una columna en celular y dos en tablet u horizontal
        setContentView(R.layout.activity_book_detail);
        UiUtils.applyWindowInsets(findViewById(R.id.detailRoot), false);

        bookId = getIntent().getLongExtra(EXTRA_BOOK_ID, 0);
        viewModel = new ViewModelProvider(this).get(LibraryViewModel.class);

        ivCover = findViewById(R.id.ivCover);
        tvTitle = findViewById(R.id.tvTitle);
        tvAuthor = findViewById(R.id.tvAuthor);
        tvGenre = findViewById(R.id.tvGenre);
        tvProgress = findViewById(R.id.tvProgress);
        tvDates = findViewById(R.id.tvDates);
        tvLogsEmpty = findViewById(R.id.tvLogsEmpty);
        progressBar = findViewById(R.id.progressBar);
        statusToggle = findViewById(R.id.statusToggle);
        stars = findViewById(R.id.starRating);
        Button btnAdd = findViewById(R.id.btnAddProgress);
        RecyclerView rvLogs = findViewById(R.id.rvLogs);

        // Lista del historial de lectura
        logAdapter = new LogAdapter();
        rvLogs.setLayoutManager(new LinearLayoutManager(this));
        rvLogs.setNestedScrollingEnabled(false);
        rvLogs.setAdapter(logAdapter);

        // Botones de la barra superior: volver, editar y borrar
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnEdit).setOnClickListener(v -> {
            Intent intent = new Intent(this, BookFormActivity.class);
            intent.putExtra(BookFormActivity.EXTRA_BOOK_ID, bookId);
            startActivity(intent);
        });
        findViewById(R.id.btnDelete).setOnClickListener(v -> confirmDelete());
        // Abre la ventana para sumar avance
        btnAdd.setOnClickListener(v -> {
            if (book != null) {
                ProgressDialogs.showAddProgress(this, book, viewModel);
            }
        });

        // Cambio manual de estado
        statusToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked || updatingUi || book == null) {
                return;
            }
            String newStatus = checkedId == R.id.btnStDone ? BookEntity.STATUS_DONE
                    : checkedId == R.id.btnStReading ? BookEntity.STATUS_READING
                    : BookEntity.STATUS_WANT;
            if (!newStatus.equals(book.status)) {
                boolean becameDone = BookEntity.STATUS_DONE.equals(newStatus);
                viewModel.setStatus(book.id, newStatus);
                if (becameDone) {
                    ProgressDialogs.showFinished(this, book.title);
                }
            }
        });

        // Calificación con estrellas
        stars.setOnRatingChangeListener(rating -> {
            if (book != null) {
                viewModel.setRating(book.id, rating);
            }
        });

        // Se observa el libro: si cambia (avance, estado, estrellas) la pantalla se actualiza sola
        viewModel.observeBook(bookId).observe(this, b -> {
            if (b == null) {
                // El libro se borró
                finish();
                return;
            }
            book = b;
            showBook(b);
        });
        // Se observa el historial del libro
        viewModel.observeLogsOfBook(bookId).observe(this, this::showLogs);
    }

    /** Muestra los datos del libro en pantalla. */
    private void showBook(BookEntity b) {
        CoverLoader.load(ivCover, b.coverPath);
        tvTitle.setText(b.title);
        tvAuthor.setText(BookText.authorOrUnknown(this, b));

        if (b.genre == null || b.genre.trim().isEmpty()) {
            tvGenre.setVisibility(View.GONE);
        } else {
            tvGenre.setVisibility(View.VISIBLE);
            tvGenre.setText(b.genre);
        }

        // Barra y texto de avance
        progressBar.setProgressCompat(b.percent(), true);
        tvProgress.setText(BookText.progressText(this, b));

        updatingUi = true;
        statusToggle.check(BookEntity.STATUS_DONE.equals(b.status) ? R.id.btnStDone
                : BookEntity.STATUS_READING.equals(b.status) ? R.id.btnStReading : R.id.btnStWant);
        stars.setRating(b.rating);
        updatingUi = false;

        // Fechas (solo las que existen)
        StringBuilder dates = new StringBuilder();
        if (b.startDate > 0) {
            dates.append(getString(R.string.detail_started, DateUtils.formatDate(b.startDate)));
        }
        if (b.finishDate > 0) {
            if (dates.length() > 0) {
                dates.append("\n");
            }
            dates.append(getString(R.string.detail_finished, DateUtils.formatDate(b.finishDate)));
        }
        tvDates.setText(dates.toString());
        tvDates.setVisibility(dates.length() == 0 ? View.GONE : View.VISIBLE);

        // El historial necesita saber si el libro es por páginas o por porcentaje
        if (lastLogs != null) {
            showLogs(lastLogs);
        }
    }

    private List<ProgressLogEntity> lastLogs;

    private void showLogs(List<ProgressLogEntity> logs) {
        lastLogs = logs;
        boolean pages = book == null || book.isPages();
        logAdapter.submit(logs, pages);
        tvLogsEmpty.setVisibility(logs == null || logs.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // Pide confirmación antes de borrar, porque no se puede deshacer
    private void confirmDelete() {
        if (book == null) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_title)
                .setMessage(getString(R.string.delete_message, book.title))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (d, w) -> viewModel.deleteBook(book.id))
                .show();
    }
}
