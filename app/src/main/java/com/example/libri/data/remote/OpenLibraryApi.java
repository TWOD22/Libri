package com.example.libri.data.remote;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Servicio web de Open Library (gratuito y sin clave).
 * Retrofit convierte esta interfaz en llamadas HTTP reales.
 * Ejemplo: https://openlibrary.org/search.json?q=dune&limit=20
 */
public interface OpenLibraryApi {

    @GET("search.json")
    Call<OpenLibraryModels.SearchResponse> search(
            @Query("q") String query,
            @Query("limit") int limit,
            @Query("fields") String fields);
}
