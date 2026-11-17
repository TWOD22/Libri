package com.example.libri.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.libri.R;
import com.example.libri.data.local.ProgressLogEntity;
import com.example.libri.util.DateUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Historial de avances de un libro: fecha, cuánto se leyó y hasta dónde se llegó.
 */
public class LogAdapter extends RecyclerView.Adapter<LogAdapter.Holder> {

    // Registros de avance del libro, del más reciente al más antiguo
    private List<ProgressLogEntity> items = new ArrayList<>();
    // Indica si el libro se mide por páginas o por porcentaje
    private boolean pages = true;

    /** pages = true si el libro se mide por páginas; false si es por porcentaje. */
    public void submit(List<ProgressLogEntity> logs, boolean pages) {
        this.items = logs == null ? new ArrayList<>() : new ArrayList<>(logs);
        this.pages = pages;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    // Crea la vista de una fila del historial
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_log, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        ProgressLogEntity log = items.get(position);
        Context c = h.itemView.getContext();
        // Fecha del registro
        h.tvDate.setText(DateUtils.formatDate(log.date));
        // El texto cambia según el tipo de avance del libro
        if (pages) {
            h.tvDelta.setText(c.getString(R.string.log_delta_pages, log.delta));
            h.tvAfter.setText(c.getString(R.string.log_after_pages, log.valueAfter));
        } else {
            h.tvDelta.setText(c.getString(R.string.log_delta_percent, log.delta));
            h.tvAfter.setText(c.getString(R.string.log_after_percent, log.valueAfter));
        }
    }

    @Override
    // Cantidad de registros
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView tvDate;
        final TextView tvDelta;
        final TextView tvAfter;

        Holder(@NonNull View v) {
            super(v);
            tvDate = v.findViewById(R.id.tvLogDate);
            tvDelta = v.findViewById(R.id.tvLogDelta);
            tvAfter = v.findViewById(R.id.tvLogAfter);
        }
    }
}
