package com.example.libri.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.libri.BookDetailActivity;
import com.example.libri.BookFormActivity;
import com.example.libri.R;
import com.example.libri.data.local.BookEntity;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Biblioteca: todos los libros del usuario con buscador y filtro por estado.
 */
public class LibraryFragment extends Fragment implements BookAdapter.Listener {

    // ViewModel compartido: guarda los libros y el estado del buscador y del filtro
    private LibraryViewModel viewModel;
    // Adaptador que dibuja la lista de libros
    private BookAdapter adapter;
    // Mensaje que aparece cuando no hay libros que mostrar
    private TextView tvEmpty;
    // Todos los libros del usuario, sin filtrar
    private List<BookEntity> allBooks = new ArrayList<>();

    @Nullable
    @Override
    // Crea la pantalla a partir de fragment_library.xml
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_library, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Se usa el ViewModel de la Activity para compartir datos entre pestañas
        viewModel = new ViewModelProvider(requireActivity()).get(LibraryViewModel.class);

        // Referencias a las vistas de la pantalla
        TextInputEditText etSearch = view.findViewById(R.id.etSearch);
        ChipGroup chips = view.findViewById(R.id.chipGroup);
        RecyclerView rv = view.findViewById(R.id.rvBooks);
        FloatingActionButton fab = view.findViewById(R.id.fabAdd);
        tvEmpty = view.findViewById(R.id.tvEmpty);

        // false = diseño de fila de la Biblioteca (con estado y calificación)
        adapter = new BookAdapter(false, this);
        // 1 columna en celular y 2 en tablet u horizontal (ver integers.xml)
        rv.setLayoutManager(new GridLayoutManager(requireContext(),
                getResources().getInteger(R.integer.library_columns)));
        rv.setAdapter(adapter);

        // Se restaura el buscador y el filtro guardados en el ViewModel
        etSearch.setText(viewModel.getLibraryQuery());
        chips.check(chipIdForStatus(viewModel.getLibraryStatus()));

        // Cada vez que el usuario escribe, se guarda el texto y se vuelve a filtrar
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setLibraryQuery(s.toString());
                applyFilter();
            }
        });

        // Al elegir un chip (Todos, Quiero leer, Leyendo, Terminado) se cambia el filtro
        chips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            int id = checkedIds.isEmpty() ? R.id.chipAll : checkedIds.get(0);
            viewModel.setLibraryStatus(statusForChipId(id));
            applyFilter();
        });

        // El botón + abre el formulario para agregar un libro nuevo
        fab.setOnClickListener(v -> startActivity(new Intent(requireContext(), BookFormActivity.class)));

        // Cuando cambian los libros en la base de datos, la lista se actualiza sola
        viewModel.getBooks().observe(getViewLifecycleOwner(), list -> {
            allBooks = list == null ? new ArrayList<>() : list;
            applyFilter();
        });
    }

    /** Filtra por texto (título o autor) y por estado, y actualiza la lista. */
    private void applyFilter() {
        String query = viewModel.getLibraryQuery().trim().toLowerCase(Locale.getDefault());
        String status = viewModel.getLibraryStatus();
        // Aquí se van guardando los libros que cumplen el filtro
        List<BookEntity> result = new ArrayList<>();
        for (BookEntity b : allBooks) {
            // Si hay un estado elegido, se descartan los libros de otro estado
            if (status != null && !status.equals(b.status)) {
                continue;
            }
            // Si hay texto de búsqueda, el título o el autor deben contenerlo
            if (!query.isEmpty()) {
                String title = b.title.toLowerCase(Locale.getDefault());
                String author = b.author == null ? "" : b.author.toLowerCase(Locale.getDefault());
                if (!title.contains(query) && !author.contains(query)) {
                    continue;
                }
            }
            result.add(b);
        }
        // Se muestra el resultado y, si está vacío, el mensaje de "no hay libros"
        adapter.submit(result);
        tvEmpty.setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // Convierte un estado (WANT, READING, DONE) en el chip que le corresponde
    private int chipIdForStatus(String status) {
        if (BookEntity.STATUS_WANT.equals(status)) {
            return R.id.chipWant;
        }
        if (BookEntity.STATUS_READING.equals(status)) {
            return R.id.chipReading;
        }
        if (BookEntity.STATUS_DONE.equals(status)) {
            return R.id.chipDone;
        }
        return R.id.chipAll;
    }

    // Convierte el chip elegido en el estado; null significa "todos"
    private String statusForChipId(int id) {
        if (id == R.id.chipWant) {
            return BookEntity.STATUS_WANT;
        }
        if (id == R.id.chipReading) {
            return BookEntity.STATUS_READING;
        }
        if (id == R.id.chipDone) {
            return BookEntity.STATUS_DONE;
        }
        return null;
    }

    @Override
    // Abre la pantalla de detalle del libro tocado
    public void onOpen(BookEntity book) {
        Intent intent = new Intent(requireContext(), BookDetailActivity.class);
        intent.putExtra(BookDetailActivity.EXTRA_BOOK_ID, book.id);
        startActivity(intent);
    }

    @Override
    // Abre la ventana para sumar avance
    public void onAddProgress(BookEntity book) {
        ProgressDialogs.showAddProgress(requireContext(), book, viewModel);
    }
}
