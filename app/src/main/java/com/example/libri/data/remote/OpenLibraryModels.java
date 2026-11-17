package com.example.libri.data.remote;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Clases que representan la respuesta JSON de Open Library.
 * Gson convierte el texto JSON en estos objetos automáticamente.
 * Algunos datos pueden venir vacíos (null), porque la información la aporta la comunidad.
 */
public final class OpenLibraryModels {

    private OpenLibraryModels() {
        // Solo agrupa las clases internas
    }

    /** Respuesta de la búsqueda: una lista de libros. */
    public static class SearchResponse {
        public List<Doc> docs;
    }

    /** Un libro encontrado. */
    public static class Doc {
        public String key;
        public String title;

        @SerializedName("author_name")
        public List<String> authorName;

        @SerializedName("cover_i")
        public Integer coverId;

        @SerializedName("number_of_pages_median")
        public Integer pages;

        @SerializedName("first_publish_year")
        public Integer year;

        public List<String> subject;
    }
}
