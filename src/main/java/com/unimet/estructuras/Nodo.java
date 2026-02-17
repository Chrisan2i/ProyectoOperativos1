package com.unimet.estructuras;

// Usamos <T> para que sea genérico (sirva para Procesos, enteros, etc.)
public class Nodo<T> {
    private T contenido;
    private Nodo<T> siguiente;

    // Constructor
    public Nodo(T contenido) {
        this.contenido = contenido;
        this.siguiente = null;
    }

    // Getters y Setters
    public T getContenido() {
        return contenido;
    }

    public void setContenido(T contenido) {
        this.contenido = contenido;
    }

    public Nodo<T> getSiguiente() {
        return siguiente;
    }

    public void setSiguiente(Nodo<T> siguiente) {
        this.siguiente = siguiente;
    }
}