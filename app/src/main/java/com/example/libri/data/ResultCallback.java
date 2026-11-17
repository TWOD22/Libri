package com.example.libri.data;

/**
 * Función que se ejecuta en el hilo principal cuando termina una tarea hecha en segundo plano.
 */
public interface ResultCallback<T> {
    void onResult(T value);
}
