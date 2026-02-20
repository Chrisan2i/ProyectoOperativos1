package com.unimet.estructuras;
import com.unimet.clases.PCB;
import java.util.concurrent.Semaphore;


public class Cola<T> {
    private Nodo<T> pFirst; // Cabeza de la cola
    private Nodo<T> pLast;  // Final de la cola
    private int size;       // Tamaño
    private Semaphore mutex = new Semaphore(1); // Semáforo Binario (Exclusión Mutua)
    public static final int CRITERIO_PRIORIDAD = 0; // Menor número = Mayor prioridad
    public static final int CRITERIO_SRT = 1;       // Shortest Remaining Time
    public static final int CRITERIO_EDF = 2;       // Earliest Deadline First


    public Cola() {
        this.pFirst = null;
        this.pLast = null;
        this.size = 0;
    }

    // Método protegido con Semáforo
    public void encolar(T dato) {
        try {
            mutex.acquire(); // Bloqueamos acceso
            Nodo<T> nuevo = new Nodo<>(dato);
            if (isEmpty()) {
                pFirst = nuevo;
                pLast = nuevo;
            } else {
                pLast.setSiguiente(nuevo);
                pLast = nuevo;
            }
            size++;
            mutex.release(); // Liberamos acceso
        } catch (InterruptedException e) { e.printStackTrace(); }
    }
    

    public void insertarOrdenado(T objeto, int criterio) {
        try {
            mutex.acquire(); // Bloqueamos
            Nodo<T> nuevo = new Nodo<>(objeto);
            PCB pNuevo = (PCB) objeto;

            if (isEmpty()) {
                pFirst = nuevo;
                pLast = nuevo;
                size++;
                mutex.release();
                return;
            }

            // Revisar cabeza
            PCB pCabeza = (PCB) pFirst.getContenido();
            if (esMejor(pNuevo, pCabeza, criterio)) {
                nuevo.setSiguiente(pFirst);
                pFirst = nuevo;
                size++;
                mutex.release();
                return;
            }

            // Revisar cuerpo
            Nodo<T> actual = pFirst;
            while (actual.getSiguiente() != null) {
                PCB pSiguiente = (PCB) actual.getSiguiente().getContenido();
                if (esMejor(pNuevo, pSiguiente, criterio)) {
                    break;
                }
                actual = actual.getSiguiente();
            }

            nuevo.setSiguiente(actual.getSiguiente());
            actual.setSiguiente(nuevo);

            if (nuevo.getSiguiente() == null) {
                pLast = nuevo;
            }
            size++;
            mutex.release(); // Liberamos
        } catch (InterruptedException e) { e.printStackTrace(); }
    }

    // Método auxiliar para comparar
    private boolean esMejor(PCB nuevo, PCB existente, int criterio) {
        switch (criterio) {
            case CRITERIO_PRIORIDAD: return nuevo.getPrioridad() < existente.getPrioridad();
            case CRITERIO_SRT:
                int restanteNuevo = nuevo.getInstruccionesTotales() - nuevo.getInstruccionesEjecutadas();
                int restanteExistente = existente.getInstruccionesTotales() - existente.getInstruccionesEjecutadas();
                return restanteNuevo < restanteExistente;
            case CRITERIO_EDF: return nuevo.getDeadline() < existente.getDeadline();
            default: return false;
        }
    }

    // Método para sacar del inicio (Desencolar)
    public T desencolar() {
        try {
            mutex.acquire(); // Bloqueamos
            if (isEmpty()) {
                mutex.release();
                return null;
            }
            T dato = pFirst.getContenido();
            pFirst = pFirst.getSiguiente();
            if (pFirst == null) {
                pLast = null;
            }
            size--;
            mutex.release(); // Liberamos
            return dato;
        } catch (InterruptedException e) { 
            e.printStackTrace(); 
            return null;
        }
    }
    
    // Ver quién es el primero sin sacarlo
    public T peek() {
        try {
            mutex.acquire();
            if (isEmpty()) {
                mutex.release();
                return null;
            }
            T dato = pFirst.getContenido();
            mutex.release();
            return dato;
        } catch (InterruptedException e) { return null; }
    }

    public boolean isEmpty() {
        return pFirst == null;
    }

    public int getSize() {
        return size;
    }
    // Método para imprimir todo el contenido de la cola
    @Override
    public String toString() {
        // Opcional: podrías usar semáforo aquí también si el toString falla
        String resultado = "";
        Nodo<T> aux = pFirst;
        while (aux != null) {
            resultado += aux.getContenido().toString() + "\n";
            aux = aux.getSiguiente();
        }
        return resultado;
    }
    // Método para extraer los datos y enviarlos a la interfaz gráfica de forma segura
    public Object[] toArray() {
        try {
            mutex.acquire(); // Bloqueamos para leer con seguridad
            Object[] arreglo = new Object[size];
            Nodo<T> actual = pFirst;
            int i = 0;
            while (actual != null && i < size) {
                arreglo[i] = actual.getContenido(); // Usamos getContenido() de tu clase Nodo
                actual = actual.getSiguiente();
                i++;
            }
            mutex.release(); // Liberamos
            return arreglo;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return new Object[0];
        }
    }
}