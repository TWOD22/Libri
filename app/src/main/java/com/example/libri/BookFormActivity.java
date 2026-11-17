package com.example.libri;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.libri.data.LibraryRepository;
import com.example.libri.data.local.BookEntity;
import com.example.libri.util.CoverLoader;
import com.example.libri.util.DateUtils;
import com.example.libri.util.ImageStorage;
import com.example.libri.util.UiUtils;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Formulario para agregar o editar un libro: título, autor, género, portada (galería o internet),
 * tipo de avance (páginas o porcentaje), estado y fecha de inicio.
 * Si recibe EXTRA_BOOK_ID, edita ese libro; si no, crea uno nuevo.
 */
public class BookFormActivity extends AppCompatActivity {

    public static final String EXTRA_BOOK_ID = "book_id";

    private LibraryRepository repository;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    // Libro que se edita (null si es nuevo)
    private BookEntity editing;

    // Datos que se guardan fuera de los campos de texto
    private String coverPath;
    private long startDate;

    private ImageView ivCover;
    private TextInputLayout layoutTitle;
    private TextInputLayout layoutTotal;
    private TextInputLayout layoutCurrent;
    private TextInputEditText etTitle;
    private TextInputEditText etAuthor;
    private TextInputEditText etGenre;
    private TextInputEditText etTotal;
    private TextInputEditText etCurrent;
    private MaterialButtonToggleGroup typeToggle;
    private MaterialButtonToggleGroup statusToggle;
    private Button btnStartDate;

    // Elegir una imagen de la galería
    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::onImagePicked);

    // Resultado de la búsqueda en internet
    private final ActivityResultLauncher<Intent> searchLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    fillFromSearch(result.getData());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_form);
        UiUtils.applyWindowInsets(findViewById(R.id.formRoot), true);

        repository = LibraryRepository.getInstance(this);

        ivCover = findViewById(R.id.ivCover);
        layoutTitle = findViewById(R.id.layoutTitle);
        layoutTotal = findViewById(R.id.layoutTotal);
        layoutCurrent = findViewById(R.id.layoutCurrent);
        etTitle = findViewById(R.id.etTitle);
        etAuthor = findViewById(R.id.etAuthor);
        etGenre = findViewById(R.id.etGenre);
        etTotal = findViewById(R.id.etTotal);
        etCurrent = findViewById(R.id.etCurrent);
        typeToggle = findViewById(R.id.typeToggle);
        statusToggle = findViewById(R.id.statusToggle);
        btnStartDate = findViewById(R.id.btnStartDate);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnPickGallery).setOnClickListener(v -> pickImage.launch("image/*"));
        findViewById(R.id.btnSearchOnline).setOnClickListener(v ->
                searchLauncher.launch(new Intent(this, BookSearchActivity.class)));
        findViewById(R.id.btnSave).setOnClickListener(v -> save());
        btnStartDate.setOnClickListener(v -> pickStartDate());

        // Al cambiar el tipo de avance cambian los textos de los campos
        typeToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                updateTypeHints();
            }
        });
        typeToggle.check(R.id.btnTypePages);
        statusToggle.check(R.id.btnFormWant);
        updateTypeHints();
        updateStartDateText();
        CoverLoader.load(ivCover, null);

        long id = getIntent().getLongExtra(EXTRA_BOOK_ID, 0);
        TextView tvFormTitle = findViewById(R.id.tvFormTitle);
        if (id != 0) {
            tvFormTitle.setText(R.string.form_title_edit);
            // Se carga el libro desde la base de datos y se llenan los campos
            repository.getBook(id, book -> {
                if (book != null && !isFinishing()) {
                    editing = book;
                    fillFromBook(book);
                }
            });
        } else {
            tvFormTitle.setText(R.string.form_title_new);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdown();
    }

    // ---------------------------------------------------------------
    // Llenar los campos
    // ---------------------------------------------------------------

    private void fillFromBook(BookEntity b) {
        etTitle.setText(b.title);
        etAuthor.setText(b.author);
        etGenre.setText(b.genre);
        coverPath = b.coverPath;
        CoverLoader.load(ivCover, coverPath);
        startDate = b.startDate;
        updateStartDateText();

        typeToggle.check(b.isPages() ? R.id.btnTypePages : R.id.btnTypePercent);
        etTotal.setText(b.totalPages > 0 ? String.valueOf(b.totalPages) : "");
        etCurrent.setText(b.current > 0 ? String.valueOf(b.current) : "");
        statusToggle.check(BookEntity.STATUS_DONE.equals(b.status) ? R.id.btnFormDone
                : BookEntity.STATUS_READING.equals(b.status) ? R.id.btnFormReading : R.id.btnFormWant);
        updateTypeHints();
    }

    /** Llena el título, autor, género, portada y páginas con el libro encontrado en internet. */
    private void fillFromSearch(Intent data) {
        etTitle.setText(data.getStringExtra(BookSearchActivity.RESULT_TITLE));
        etAuthor.setText(data.getStringExtra(BookSearchActivity.RESULT_AUTHOR));
        etGenre.setText(data.getStringExtra(BookSearchActivity.RESULT_GENRE));
        coverPath = data.getStringExtra(BookSearchActivity.RESULT_COVER_URL);
        CoverLoader.load(ivCover, coverPath);
        int pages = data.getIntExtra(BookSearchActivity.RESULT_PAGES, 0);
        if (pages > 0) {
            etTotal.setText(String.valueOf(pages));
        }
    }

    // ---------------------------------------------------------------
    // Portada desde la galería
    // ---------------------------------------------------------------

    /** Copia la imagen elegida a la carpeta de la app (en segundo plano) y la muestra. */
    private void onImagePicked(Uri uri) {
        if (uri == null) {
            return; // El usuario cerró la galería sin elegir
        }
        io.execute(() -> {
            try {
                String path = ImageStorage.saveCover(getApplicationContext(), uri);
                runOnUiThread(() -> {
                    coverPath = path;
                    CoverLoader.load(ivCover, coverPath);
                });
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, R.string.error_image, Toast.LENGTH_SHORT).show());
            }
        });
    }

    // ---------------------------------------------------------------
    // Tipo de avance y fecha
    // ---------------------------------------------------------------

    private boolean isPagesType() {
        return typeToggle.getCheckedButtonId() != R.id.btnTypePercent;
    }

    private void updateTypeHints() {
        if (isPagesType()) {
            layoutTotal.setHint(getString(R.string.form_total_pages));
            layoutCurrent.setHint(getString(R.string.form_current_pages));
        } else {
            layoutTotal.setHint(getString(R.string.form_total_pages_optional));
            layoutCurrent.setHint(getString(R.string.form_current_percent));
        }
        layoutTotal.setError(null);
        layoutCurrent.setError(null);
    }

    private void updateStartDateText() {
        btnStartDate.setText(startDate > 0
                ? getString(R.string.form_start_date_value, DateUtils.formatDate(startDate))
                : getString(R.string.form_start_date_none));
    }

    /** Selector de fecha de Material. */
    private void pickStartDate() {
        MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.form_start_date_title);
        if (startDate > 0) {
            // El selector trabaja en hora UTC: se compensa la zona horaria local
            builder.setSelection(startDate + TimeZone.getDefault().getOffset(startDate));
        }
        MaterialDatePicker<Long> picker = builder.build();
        picker.addOnPositiveButtonClickListener(selection -> {
            // Se convierte de UTC a la hora local para que no se corra un día
            startDate = selection - TimeZone.getDefault().getOffset(selection);
            updateStartDateText();
        });
        picker.show(getSupportFragmentManager(), "start_date");
    }

    // ---------------------------------------------------------------
    // Guardar
    // ---------------------------------------------------------------

    /** Lee un número de un campo; devuelve -1 si está vacío y -2 si no es válido. */
    private int readNumber(TextInputEditText field) {
        String text = field.getText() == null ? "" : field.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            return -1;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return -2;
        }
    }

    private void save() {
        layoutTitle.setError(null);
        layoutTotal.setError(null);
        layoutCurrent.setError(null);

        String title = etTitle.getText() == null ? "" : etTitle.getText().toString().trim();
        if (title.isEmpty()) {
            layoutTitle.setError(getString(R.string.error_title_required));
            return;
        }

        boolean pages = isPagesType();
        int total = readNumber(etTotal);
        int current = readNumber(etCurrent);

        // Total de páginas: obligatorio si el avance es por páginas
        if (total == -2 || (pages && total <= 0)) {
            layoutTotal.setError(getString(R.string.error_total_required));
            return;
        }
        if (total < 0) {
            total = 0;
        }
        // Avance actual (opcional)
        if (current == -2) {
            layoutCurrent.setError(getString(R.string.error_number_invalid));
            return;
        }
        if (current < 0) {
            current = 0;
        }
        int max = pages ? total : 100;
        if (max > 0 && current > max) {
            layoutCurrent.setError(getString(
                    pages ? R.string.error_over_pages : R.string.error_over_percent, max));
            return;
        }

        String status = statusToggle.getCheckedButtonId() == R.id.btnFormDone ? BookEntity.STATUS_DONE
                : statusToggle.getCheckedButtonId() == R.id.btnFormReading ? BookEntity.STATUS_READING
                : BookEntity.STATUS_WANT;
        // Si ya hay avance, el libro se está leyendo
        if (current > 0 && BookEntity.STATUS_WANT.equals(status)) {
            status = BookEntity.STATUS_READING;
        }

        BookEntity book = editing != null ? editing : new BookEntity();
        book.title = title;
        book.author = etAuthor.getText() == null ? "" : etAuthor.getText().toString().trim();
        book.genre = etGenre.getText() == null ? "" : etGenre.getText().toString().trim();
        book.coverPath = coverPath;
        book.progressType = pages ? BookEntity.TYPE_PAGES : BookEntity.TYPE_PERCENT;
        book.totalPages = total;
        book.current = current;
        book.status = status;
        book.startDate = startDate;

        repository.saveBook(book, id -> {
            Toast.makeText(this, R.string.toast_book_saved, Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
