package com.unimet.clases;

public class PCB {
    private static int contadorIds = 1; // Autogenerar IDs
    
    // Atributos base del PDF
    private int id;
    private String nombre;
    private String estado; // Nuevo, Listo, Ejecucion, etc.
    private int prioridad; 
    private int instruccionesTotales;
    private int instruccionesEjecutadas;
    
    public PCB(String nombre, int prioridad, int instrucciones) {
        this.id = contadorIds++;
        this.nombre = nombre;
        this.prioridad = prioridad;
        this.instruccionesTotales = instrucciones;
        this.estado = "Nuevo";
        this.instruccionesEjecutadas = 0;
    }
    
    // Getters basicos
    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public int getPrioridad() { return prioridad; }
    public int getInstruccionesTotales() { return instruccionesTotales; }
    public int getInstruccionesEjecutadas() { return instruccionesEjecutadas; }
    
    @Override
    public String toString() {
        return String.format("[%d] %s | Est: %s | Prio: %d", id, nombre, estado, prioridad);
    }
}