package com.example.libri.data;

/**
 * Resultado de registrar un avance: sirve para decidir qué mensaje mostrar.
 */
public class ProgressResult {
    /** true si se guardó un avance nuevo. */
    public boolean changed;
    /** true si con este avance el libro quedó terminado. */
    public boolean finished;
    /** true si el valor escrito era menor al anterior (se tomó como corrección). */
    public boolean corrected;
    /** Título del libro. */
    public String title;
}
