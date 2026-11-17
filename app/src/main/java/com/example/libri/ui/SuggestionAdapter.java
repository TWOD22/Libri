package com.example.libri.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.libri.R;
import com.example.libri.data.BookSuggestion;
import com.example.libri.util.CoverLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultados de la búsqueda en Open Library. Al tocar uno, se usa para llenar el formulario.
 */
public class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.Holder> {

    /** Se avisa cuando el usuario elige un resultado. */
    public interface OnPick {
        void onPick(BookSuggestion suggestion);
    }

    // Quién se entera cuando el usuario elige un resultado
    private final OnPick listener;
    // Resultados de la búsqueda en internet
    private List<BookSuggestion> items = new ArrayList<>();

    public SuggestionAdapter(OnPick listener) {
        this.listener = listener;
    }

    public void submit(List<BookSuggestion> suggestions) {
        items = suggestions == null ? new ArrayList<>() : new ArrayList<>(suggestions);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_suggestion, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        BookSuggestion s = items.get(position);
        Context c = h.itemView.getContext();
        // Portada descargada de internet (o imagen por defecto)
        CoverLoader.load(h.ivCover, s.coverUrl);
        h.tvTitle.setText(s.title);

        // Autor y año (solo lo que exista)
        StringBuilder sb = new StringBuilder();
        sb.append(s.author == null || s.author.isEmpty() ? c.getString(R.string.author_unknown) : s.author);
        if (s.year > 0) {
            sb.append(" · ").append(s.year);
        }
        h.tvAuthor.setText(sb.toString());
        h.tvPages.setText(s.pages > 0 ? c.getString(R.string.suggestion_pages, s.pages) : "");
        // Al tocar el resultado se devuelve al formulario
        h.itemView.setOnClickListener(v -> listener.onPick(s));
    }

    @Override
    // Cantidad de resultados
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView ivCover;
        final TextView tvTitle;
        final TextView tvAuthor;
        final TextView tvPages;

        Holder(@NonNull View v) {
            super(v);
            ivCover = v.findViewById(R.id.ivCover);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvAuthor = v.findViewById(R.id.tvAuthor);
            tvPages = v.findViewById(R.id.tvPages);
        }
    }
}
