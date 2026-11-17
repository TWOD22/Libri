package com.example.libri;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.libri.data.BookSuggestion;
import com.example.libri.data.LibraryRepository;
import com.example.libri.ui.SuggestionAdapter;
import com.example.libri.util.UiUtils;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Busca libros en Open Library (internet). Al elegir uno, devuelve sus datos al formulario
 * para que el usuario no tenga que escribirlos.
 */
public class BookSearchActivity extends AppCompatActivity {

    // Claves de los datos que se devuelven al formulario
    public static final String RESULT_TITLE = "title";
    public static final String RESULT_AUTHOR = "author";
    public static final String RESULT_GENRE = "genre";
    public static final String RESULT_COVER_URL = "cover_url";
    public static final String RESULT_PAGES = "pages";

    // Vistas de la pantalla
    private TextInputEditText etQuery;
    private TextView tvMessage;
    private LinearProgressIndicator loading;
    private SuggestionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_search);
        UiUtils.applyWindowInsets(findViewById(R.id.searchRoot), true);

        etQuery = findViewById(R.id.etQuery);
        tvMessage = findViewById(R.id.tvMessage);
        loading = findViewById(R.id.loading);
        RecyclerView rv = findViewById(R.id.rvResults);

        // Al elegir un resultado se llama a pick()
        adapter = new SuggestionAdapter(this::pick);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSearch).setOnClickListener(v -> search());
        // La tecla de buscar del teclado hace la misma búsqueda que la lupa
        etQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search();
                return true;
            }
            return false;
        });

        tvMessage.setText(R.string.search_idle);
        tvMessage.setVisibility(View.VISIBLE);
    }

    /** Lanza la búsqueda en internet con lo que escribió el usuario. */
    private void search() {
        String query = etQuery.getText() == null ? "" : etQuery.getText().toString().trim();
        // Se pide un mínimo de letras para no consultar con texto vacío
        if (query.length() < 2) {
            tvMessage.setText(R.string.search_too_short);
            tvMessage.setVisibility(View.VISIBLE);
            return;
        }
        // Cierra el teclado
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etQuery.getWindowToken(), 0);
        }

        // Se muestra la barra de carga mientras llega la respuesta de internet
        loading.setVisibility(View.VISIBLE);
        tvMessage.setVisibility(View.GONE);
        adapter.submit(null);

        // Si la pantalla ya se cerró, se ignora la respuesta para no fallar
        LibraryRepository.getInstance(this).searchOnline(query,
                results -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    loading.setVisibility(View.GONE);
                    adapter.submit(results);
                    if (results.isEmpty()) {
                        tvMessage.setText(R.string.search_empty);
                        tvMessage.setVisibility(View.VISIBLE);
                    }
                },
                error -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    loading.setVisibility(View.GONE);
                    tvMessage.setText(error);
                    tvMessage.setVisibility(View.VISIBLE);
                });
    }

    /** Devuelve el libro elegido a la pantalla anterior. */
    private void pick(BookSuggestion s) {
        Intent data = new Intent();
        data.putExtra(RESULT_TITLE, s.title);
        data.putExtra(RESULT_AUTHOR, s.author);
        data.putExtra(RESULT_GENRE, s.genre);
        data.putExtra(RESULT_COVER_URL, s.coverUrl);
        data.putExtra(RESULT_PAGES, s.pages);
        setResult(RESULT_OK, data);
        finish();
    }
}
