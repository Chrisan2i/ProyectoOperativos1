package com.unimet.clases;

public class PCB {
    private static int contadorIds = 1; // Para autogenerar IDs únicos (1, 2, 3...)
    
    // Atributos requeridos por el PDF
    private int id;
    private String nombre;
    private String estado; // "Nuevo", "Listo", "Ejecucion", "Bloqueado", "Terminado"
    private int programCounter; // PC
    private int mar; // MAR
    private int prioridad; // Prioridad del proceso
    private int instruccionesTotales; // Duración total
    private int instruccionesEjecutadas; // Progreso
    private int deadline; // Tiempo límite (ciclo reloj absoluto)
    private int cicloParaBloqueo; // En qué instrucción ocurre el evento (ej: ciclo 10)
    private int longitudBloqueo;  // Cuánto tiempo debe esperar (ej: 5 ciclos)
    private int tiempoEsperado; //métrica de tiempo de espera
    
    // Constructor
    public PCB(String nombre, int prioridad, int instrucciones, int deadline) {
        this.id = contadorIds++;
        this.nombre = nombre;
        this.prioridad = prioridad;
        this.instruccionesTotales = instrucciones;
        this.deadline = deadline; // Ciclo reloj en el que DEBE terminar
        
        // Valores iniciales por defecto
        this.estado = "Nuevo";
        this.programCounter = 0;
        this.mar = 0;
        this.instruccionesEjecutadas = 0;
        // Si el proceso es largo, le damos probabilidad de bloquearse
    if (instrucciones > 20) {
        this.cicloParaBloqueo = (int) (Math.random() * (instrucciones - 10)) + 5;
        this.longitudBloqueo = (int) (Math.random() * 5) + 3; // Espera entre 3 y 7 ciclos
    } else {
        this.cicloParaBloqueo = -1; // No se bloquea
        this.longitudBloqueo = 0;
}
    }
    
    // --- Getters y Setters ---

    public int getId() { return id; }
    
    public int getMar() { 
    return mar; 
}
    
    public String getNombre() { return nombre; }
    
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    
    public int getPrioridad() { return prioridad; }
    
    public int getInstruccionesTotales() { return instruccionesTotales; }
    
    public int getInstruccionesEjecutadas() { return instruccionesEjecutadas; }

    public int getProgramCounter() { return programCounter; }
    
    public int getDeadline() { return deadline; }

    // Método para simular ejecución de un ciclo
    public void avanzarInstruccion() {
        this.programCounter++;
        this.mar++;
        this.instruccionesEjecutadas++;
    }
    public int getTiempoEsperado() { return tiempoEsperado; }
    public void incrementarTiempoEspera() { this.tiempoEsperado++; }
    
    @Override
    public String toString() {
    return String.format("[%d] %s | Est: %s | Prio: %d | Dead: %d | PC: %d", 
            id, nombre, estado, prioridad, deadline, programCounter);
}

    public int getCicloParaBloqueo() { return cicloParaBloqueo; }
    public void setCicloParaBloqueo(int c) { this.cicloParaBloqueo = c; }
    public int getLongitudBloqueo() { return longitudBloqueo; }
    public void setLongitudBloqueo(int l) { this.longitudBloqueo = l; }
}