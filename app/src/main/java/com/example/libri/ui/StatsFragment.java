package com.example.libri.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.libri.BookDetailActivity;
import com.example.libri.R;
import com.example.libri.data.local.BookEntity;
import com.example.libri.data.local.ProgressLogEntity;
import com.example.libri.util.DateUtils;
import com.example.libri.util.StatsCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Estadísticas: calendario del mes con los días leídos, resumen del mes, racha
 * y las portadas de los libros terminados en el año.
 */
public class StatsFragment extends Fragment implements BookAdapter.Listener {

    // ViewModel compartido; también recuerda qué mes se está viendo
    private LibraryViewModel viewModel;
    // Dibuja la cuadrícula del calendario
    private CalendarAdapter calendarAdapter;
    // Dibuja la fila de portadas de libros terminados en el año
    private CoverStripAdapter yearAdapter;

    // Vistas del título del mes y de las fichas de resumen
    private TextView tvMonth;
    private TextView tvStatBooks;
    private TextView tvStatPages;
    private TextView tvStatPerDay;
    private TextView tvStatStreak;
    private TextView tvYearTitle;
    private TextView tvYearEmpty;

    // Últimos datos recibidos de la base de datos
    private List<BookEntity> books = new ArrayList<>();
    private List<ProgressLogEntity> logs = new ArrayList<>();

    @Nullable
    @Override
    // Crea la pantalla a partir de fragment_stats.xml
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(LibraryViewModel.class);

        tvMonth = view.findViewById(R.id.tvMonth);
        tvStatBooks = view.findViewById(R.id.tvStatBooks);
        tvStatPages = view.findViewById(R.id.tvStatPages);
        tvStatPerDay = view.findViewById(R.id.tvStatPerDay);
        tvStatStreak = view.findViewById(R.id.tvStatStreak);
        tvYearTitle = view.findViewById(R.id.tvYearTitle);
        tvYearEmpty = view.findViewById(R.id.tvYearEmpty);
        RecyclerView rvCalendar = view.findViewById(R.id.rvCalendar);
        RecyclerView rvYear = view.findViewById(R.id.rvYear);

        // El calendario es una cuadrícula de 7 columnas (lunes a domingo)
        calendarAdapter = new CalendarAdapter();
        rvCalendar.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        rvCalendar.setNestedScrollingEnabled(false);
        rvCalendar.setAdapter(calendarAdapter);

        // Fila horizontal que se desplaza de lado
        yearAdapter = new CoverStripAdapter(this);
        rvYear.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvYear.setAdapter(yearAdapter);

        // Botones para ir al mes anterior y al siguiente
        view.findViewById(R.id.btnPrevMonth).setOnClickListener(v -> {
            viewModel.moveStatsMonth(-1);
            refresh();
        });
        view.findViewById(R.id.btnNextMonth).setOnClickListener(v -> {
            viewModel.moveStatsMonth(1);
            refresh();
        });

        // Si cambian los libros o los registros, se recalculan las estadísticas
        viewModel.getBooks().observe(getViewLifecycleOwner(), list -> {
            books = list == null ? new ArrayList<>() : list;
            refresh();
        });
        viewModel.getLogs().observe(getViewLifecycleOwner(), list -> {
            logs = list == null ? new ArrayList<>() : list;
            refresh();
        });
    }

    /** Recalcula todo lo que se ve según el mes elegido. */
    private void refresh() {
        int year = viewModel.getStatsYear();
        int month = viewModel.getStatsMonth();

        // Título del mes y cálculo de sus estadísticas
        tvMonth.setText(DateUtils.monthTitle(year, month));
        StatsCalculator.MonthStats stats = StatsCalculator.monthStats(year, month, books, logs);
        calendarAdapter.submit(year, month, stats.daysRead);

        // Fichas de resumen: libros terminados, páginas y páginas por día de lectura
        tvStatBooks.setText(String.valueOf(stats.booksFinished));
        tvStatPages.setText(String.valueOf(stats.pages));
        tvStatPerDay.setText(String.valueOf(stats.pagesPerDay));

        // La racha siempre es la actual, sin importar el mes que se vea
        Set<Long> days = StatsCalculator.readingDays(logs);
        tvStatStreak.setText(String.valueOf(StatsCalculator.currentStreak(days)));

        // Portadas de los libros terminados en el año del mes mostrado
        List<BookEntity> finished = new ArrayList<>();
        for (BookEntity b : books) {
            if (BookEntity.STATUS_DONE.equals(b.status) && b.finishDate > 0
                    && DateUtils.yearOf(b.finishDate) == year) {
                finished.add(b);
            }
        }
        tvYearTitle.setText(getString(R.string.stats_year_title, year, finished.size()));
        yearAdapter.submit(finished);
        tvYearEmpty.setVisibility(finished.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    // Al tocar una portada del año se abre el detalle del libro
    public void onOpen(BookEntity book) {
        Intent intent = new Intent(requireContext(), BookDetailActivity.class);
        intent.putExtra(BookDetailActivity.EXTRA_BOOK_ID, book.id);
        startActivity(intent);
    }

    @Override
    public void onAddProgress(BookEntity book) {
        // No se usa en Estadísticas
    }
}
