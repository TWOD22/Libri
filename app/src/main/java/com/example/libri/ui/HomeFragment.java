package com.example.libri.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.libri.BookDetailActivity;
import com.example.libri.R;
import com.example.libri.data.local.BookEntity;
import com.example.libri.data.local.ProgressLogEntity;
import com.example.libri.util.DateUtils;
import com.example.libri.util.SessionManager;
import com.example.libri.util.StatsCalculator;
import com.example.libri.util.UiUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

/**
 * Inicio: saludo, meta diaria con racha, últimos 7 días y los libros que se están leyendo.
 */
public class HomeFragment extends Fragment implements BookAdapter.Listener {

    // ViewModel compartido con las demás pestañas
    private LibraryViewModel viewModel;
    // Adaptador de las tarjetas "Leyendo ahora"
    private BookAdapter adapter;

    // Vistas de la tarjeta de meta diaria y racha
    private TextView tvStreak;
    private TextView tvGoal;
    // Contenedor de los 7 puntos de la semana
    private LinearLayout weekDots;
    // Mensaje cuando no hay libros en lectura
    private TextView tvEmpty;

    // Últimos datos recibidos (se recalcula cuando llega cualquiera de los dos)
    private List<BookEntity> books = new ArrayList<>();
    private List<ProgressLogEntity> logs = new ArrayList<>();

    @Nullable
    @Override
    // Crea la pantalla a partir de fragment_home.xml
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Se usa el ViewModel de la Activity para compartir datos entre pestañas
        viewModel = new ViewModelProvider(requireActivity()).get(LibraryViewModel.class);

        TextView tvGreeting = view.findViewById(R.id.tvGreeting);
        tvStreak = view.findViewById(R.id.tvStreak);
        tvGoal = view.findViewById(R.id.tvGoal);
        weekDots = view.findViewById(R.id.weekDots);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        RecyclerView rv = view.findViewById(R.id.rvCurrent);

        // Saludo con el nombre del usuario que inició sesión
        tvGreeting.setText(getString(R.string.home_greeting, SessionManager.getDisplayName(requireContext())));

        // Columnas según la pantalla (1 en celular, 2 en tablet u horizontal)
        adapter = new BookAdapter(true, this);
        rv.setLayoutManager(new GridLayoutManager(requireContext(),
                getResources().getInteger(R.integer.library_columns)));
        rv.setNestedScrollingEnabled(false);
        rv.setAdapter(adapter);

        // Cada vez que cambian los libros se recalcula la pantalla
        viewModel.getBooks().observe(getViewLifecycleOwner(), list -> {
            books = list == null ? new ArrayList<>() : list;
            refresh();
        });
        // Cada vez que cambian los registros de avance se recalcula la racha
        viewModel.getLogs().observe(getViewLifecycleOwner(), list -> {
            logs = list == null ? new ArrayList<>() : list;
            refresh();
        });
    }

    /** Recalcula la racha, los puntos de la semana y la lista de libros en lectura. */
    private void refresh() {
        // Días en los que el usuario leyó, y de ahí la racha y la meta de hoy
        Set<Long> days = StatsCalculator.readingDays(logs);
        int streak = StatsCalculator.currentStreak(days);
        boolean readToday = StatsCalculator.readToday(days);

        // Texto de la racha (con singular o plural) y estado de la meta diaria
        tvStreak.setText(getResources().getQuantityString(R.plurals.streak_days, streak, streak));
        tvGoal.setText(readToday ? R.string.goal_done : R.string.goal_pending);
        buildWeekDots(days);

        // Solo se muestran los libros que están en estado "Leyendo"
        List<BookEntity> reading = new ArrayList<>();
        for (BookEntity b : books) {
            if (BookEntity.STATUS_READING.equals(b.status)) {
                reading.add(b);
            }
        }
        adapter.submit(reading);
        tvEmpty.setVisibility(reading.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** Dibuja 7 puntos (los últimos 7 días): encendido si ese día se leyó. */
    private void buildWeekDots(Set<Long> days) {
        weekDots.removeAllViews();
        // Letras de los días de la semana, empezando por domingo
        String[] letters = getResources().getStringArray(R.array.week_letters);
        long today = DateUtils.today();
        for (int i = 6; i >= 0; i--) {
            // Se retrocede i días desde hoy para saber qué letra le toca a cada punto
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_YEAR, -i);
            String letter = letters[c.get(Calendar.DAY_OF_WEEK) - 1];

            LinearLayout cell = new LinearLayout(requireContext());
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            cell.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            View dot = new View(requireContext());
            int size = UiUtils.dp(requireContext(), 22);
            dot.setLayoutParams(new LinearLayout.LayoutParams(size, size));
            // Punto encendido si ese día hubo lectura, apagado si no
            dot.setBackgroundResource(days.contains(today - i) ? R.drawable.bg_dot_on : R.drawable.bg_dot_off);

            TextView label = new TextView(requireContext());
            label.setText(letter);
            label.setTextSize(12);
            label.setTextColor(requireContext().getColor(R.color.libri_text_secondary));

            cell.addView(dot);
            cell.addView(label);
            weekDots.addView(cell);
        }
    }

    @Override
    // Abre el detalle del libro tocado
    public void onOpen(BookEntity book) {
        Intent intent = new Intent(requireContext(), BookDetailActivity.class);
        intent.putExtra(BookDetailActivity.EXTRA_BOOK_ID, book.id);
        startActivity(intent);
    }

    @Override
    // Abre la ventana para escribir en qué página (o porcentaje) va el lector
    public void onAddProgress(BookEntity book) {
        ProgressDialogs.showAddProgress(requireContext(), book, viewModel);
    }
}
