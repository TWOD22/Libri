package com.example.libri.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.libri.R;
import com.example.libri.util.DateUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Calendario del mes: una cuadrícula de 7 columnas (lunes a domingo).
 * Los días con lectura se marcan en dorado y el día de hoy lleva un borde.
 */
public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.Holder> {

    // Mes que se está dibujando
    private int year;
    private int month0;
    // Celdas vacías antes del día 1 (para que caiga en su día de la semana)
    private int offset;        // celdas vacías antes del día 1
    private int daysInMonth;
    // Días del mes en los que se registró lectura
    private Set<Integer> daysRead = new HashSet<>();

    public void submit(int year, int month0, Set<Integer> daysRead) {
        this.year = year;
        this.month0 = month0;
        this.offset = DateUtils.firstDayOffsetMonday(year, month0);
        this.daysInMonth = DateUtils.daysInMonth(year, month0);
        this.daysRead = daysRead == null ? new HashSet<>() : daysRead;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_day, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        // Número de día que corresponde a esta celda (menor a 1 = celda vacía)
        int day = position - offset + 1;
        if (day < 1 || day > daysInMonth) {
            h.tvDay.setText("");
            h.tvDay.setBackgroundResource(0);
            return;
        }
        h.tvDay.setText(String.valueOf(day));
        // Se decide el fondo según si ese día se leyó y si es hoy
        boolean read = daysRead.contains(day);
        boolean today = DateUtils.dayIndexOf(year, month0, day) == DateUtils.today();

        if (read && today) {
            h.tvDay.setBackgroundResource(R.drawable.bg_day_read_today);
        } else if (read) {
            h.tvDay.setBackgroundResource(R.drawable.bg_day_read);
        } else if (today) {
            h.tvDay.setBackgroundResource(R.drawable.bg_day_today);
        } else {
            h.tvDay.setBackgroundResource(R.drawable.bg_day_empty);
        }
        // Texto oscuro sobre el dorado para que se lea bien en ambos temas
        h.tvDay.setTextColor(h.itemView.getContext().getColor(
                read ? R.color.libri_background : R.color.libri_text_primary));
    }

    @Override
    // Celdas vacías del inicio más los días del mes
    public int getItemCount() {
        return offset + daysInMonth;
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView tvDay;

        Holder(@NonNull View v) {
            super(v);
            tvDay = v.findViewById(R.id.tvDay);
        }
    }
}
