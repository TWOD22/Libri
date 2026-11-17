package com.example.libri.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.libri.R;
import com.example.libri.data.local.BookEntity;
import com.example.libri.util.BookText;
import com.example.libri.util.CoverLoader;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador de la lista de libros. Sirve para dos diseños:
 * - tarjeta "leyendo ahora" (Inicio), con botón para sumar avance;
 * - fila de la Biblioteca, con el estado y la calificación.
 */
public class BookAdapter extends RecyclerView.Adapter<BookAdapter.Holder> {

    /** Acciones del usuario sobre un libro. */
    public interface Listener {
        void onOpen(BookEntity book);

        void onAddProgress(BookEntity book);
    }

    // true = tarjeta de Inicio, false = fila de la Biblioteca
    private final boolean currentStyle;
    // Quién atiende los toques del usuario
    private final Listener listener;
    // Libros que se muestran
    private List<BookEntity> items = new ArrayList<>();

    public BookAdapter(boolean currentStyle, Listener listener) {
        this.currentStyle = currentStyle;
        this.listener = listener;
    }

    /** Reemplaza la lista y refresca (las listas son cortas, no hace falta DiffUtil). */
    public void submit(List<BookEntity> books) {
        items = books == null ? new ArrayList<>() : new ArrayList<>(books);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Se elige el diseño según el estilo del adaptador
        int layout = currentStyle ? R.layout.item_book_current : R.layout.item_book_library;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        BookEntity book = items.get(position);
        Context context = h.itemView.getContext();

        // Se llenan las vistas con los datos del libro
        CoverLoader.load(h.ivCover, book.coverPath);
        h.tvTitle.setText(book.title);
        h.tvAuthor.setText(BookText.authorOrUnknown(context, book));
        h.progress.setProgressCompat(book.percent(), false);
        h.tvProgress.setText(BookText.progressText(context, book));

        // Estos elementos solo existen en el diseño de la Biblioteca
        if (h.tvStatus != null) {
            h.tvStatus.setText(BookText.statusLabel(context, book.status));
        }
        if (h.tvRating != null) {
            if (book.rating > 0) {
                h.tvRating.setVisibility(View.VISIBLE);
                h.tvRating.setText(context.getString(R.string.rating_short, book.rating));
            } else {
                h.tvRating.setVisibility(View.GONE);
            }
        }
        // El botón "Sumar avance" solo existe en la tarjeta de Inicio
        if (h.btnAdd != null) {
            h.btnAdd.setOnClickListener(v -> listener.onAddProgress(book));
        }
        // Tocar la tarjeta completa abre el detalle
        h.itemView.setOnClickListener(v -> listener.onOpen(book));
    }

    @Override
    // Cantidad de libros en la lista
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView ivCover;
        final TextView tvTitle;
        final TextView tvAuthor;
        final TextView tvProgress;
        final TextView tvStatus;   // solo en la fila de la Biblioteca
        final TextView tvRating;   // solo en la fila de la Biblioteca
        final Button btnAdd;       // solo en la tarjeta de Inicio
        final LinearProgressIndicator progress;

        Holder(@NonNull View v) {
            super(v);
            ivCover = v.findViewById(R.id.ivCover);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvAuthor = v.findViewById(R.id.tvAuthor);
            tvProgress = v.findViewById(R.id.tvProgress);
            tvStatus = v.findViewById(R.id.tvStatus);
            tvRating = v.findViewById(R.id.tvRating);
            btnAdd = v.findViewById(R.id.btnAddProgress);
            progress = v.findViewById(R.id.progressBar);
        }
    }
}
