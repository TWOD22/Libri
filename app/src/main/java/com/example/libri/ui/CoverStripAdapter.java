package com.example.libri.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.libri.R;
import com.example.libri.data.local.BookEntity;
import com.example.libri.util.CoverLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Fila horizontal de portadas (libros terminados en el año, en Estadísticas).
 */
public class CoverStripAdapter extends RecyclerView.Adapter<CoverStripAdapter.Holder> {

    // Quién atiende el toque sobre una portada (abre el detalle del libro)
    private final BookAdapter.Listener listener;
    // Libros que se muestran en la fila horizontal
    private List<BookEntity> items = new ArrayList<>();

    // Recibe el oyente que se avisa cuando el usuario toca una portada
    public CoverStripAdapter(BookAdapter.Listener listener) {
        this.listener = listener;
    }

    // Reemplaza la lista de libros y vuelve a dibujar la fila
    public void submit(List<BookEntity> books) {
        items = books == null ? new ArrayList<>() : new ArrayList<>(books);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    // Crea la vista de una portada a partir de item_cover_small.xml
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cover_small, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        // Libro que corresponde a esta posición de la fila
        BookEntity book = items.get(position);
        // Carga la imagen de portada (o la imagen por defecto si no tiene)
        CoverLoader.load(h.ivCover, book.coverPath);
        // El título sirve como descripción para lectores de pantalla
        h.ivCover.setContentDescription(book.title);
        // Al tocar la portada se abre el detalle del libro
        h.itemView.setOnClickListener(v -> listener.onOpen(book));
    }

    @Override
    // Cantidad de portadas que hay que dibujar
    public int getItemCount() {
        return items.size();
    }

    // Guarda la referencia a la imagen para no buscarla cada vez (patrón ViewHolder)
    static class Holder extends RecyclerView.ViewHolder {
        final ImageView ivCover;

        Holder(@NonNull View v) {
            super(v);
            ivCover = v.findViewById(R.id.ivCover);
        }
    }
}
