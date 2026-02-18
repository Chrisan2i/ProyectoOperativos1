package com.unimet.estructuras;

public class Cola<T> {
    private Nodo<T> pFirst; 
    private Nodo<T> pLast;  
    private int size;       

    public Cola() {
        this.pFirst = null;
        this.pLast = null;
        this.size = 0;
    }

    public void encolar(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        if (isEmpty()) {
            pFirst = nuevo;
            pLast = nuevo;
        } else {
            pLast.setSiguiente(nuevo);
            pLast = nuevo;
        }
        size++;
    }

    public T desencolar() {
        if (isEmpty()) return null;
        
        T dato = pFirst.getContenido();
        pFirst = pFirst.getSiguiente();
        if (pFirst == null) {
            pLast = null;
        }
        size--;
        return dato;
    }
    
    public boolean isEmpty() { return pFirst == null; }
    public int getSize() { return size; }

    @Override
    public String toString() {
        String resultado = "";
        Nodo<T> aux = pFirst;
        while (aux != null) {
            resultado += aux.getContenido().toString() + "\n";
            aux = aux.getSiguiente();
        }
        return resultado;
    }
}