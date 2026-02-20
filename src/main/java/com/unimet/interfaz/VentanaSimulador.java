package com.unimet.interfaz;

import com.unimet.clases.GestorGraficas;
import com.unimet.clases.PCB;
import com.unimet.estructuras.Cola;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.concurrent.Semaphore;

public class VentanaSimulador extends JFrame {

    // --- 1. COMPONENTES VISUALES ---
    private DefaultTableModel modeloListos, modeloBloqueados, modeloListosSusp, modeloBloqSusp;
    private JTable tablaListos, tablaBloqueados, tablaListosSusp, tablaBloqSusp;

    private JLabel lblProcesoActual, lblRelojGlobal, lblMemoriaRAM;
    private JProgressBar barraProgresoCPU;
    private JComboBox<String> cmbAlgoritmos;
    private JButton btnIniciar, btnCrearProceso, btnCrear20, btnCargarArchivo;
    private JSpinner spnQuantum, spnVelocidad;
    private JTextArea txtLogEventos;

    // --- 2. VARIABLES DE LÓGICA DEL SIMULADOR ---
    private Cola<PCB> colaListos = new Cola<>();
    private Cola<PCB> colaBloqueados = new Cola<>();
    private Cola<PCB> colaListosSuspendidos = new Cola<>();
    private Cola<PCB> colaBloqueadosSuspendidos = new Cola<>();
    
    public static final int MAX_MEMORIA = 5; 
    private GestorGraficas misGraficas; 
    
    private PCB procesoEnCpu = null;
    private boolean corriendo = false;
    private int contadorQuantum = 0; 
    private int relojGlobal = 0;
    
    // Semáforo global para pausar el CPU si hay una interrupción crítica
    private final Semaphore semaforoSistema = new Semaphore(1);

    // Paleta de Colores
    Color colorFondo = Color.decode("#0F172A");
    Color colorPanel = Color.decode("#1E293B");
    Color colorBorde = Color.decode("#38BDF8");
    Color colorTexto = Color.decode("#E2E8F0");
    Color colorVerde = Color.decode("#22C55E");
    Color colorRojo = Color.decode("#EF4444");

    public VentanaSimulador() {
        setTitle("UNIMET-Sat RTOS | Mission Control Center");
        setSize(1280, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(colorFondo);

        misGraficas = new GestorGraficas(); // Inicia la ventana de gráficos
        inicializarComponentes();
        
        generarProcesosAleatorios(5); // Poblar colas inicialmente (Requisito PDF)
        actualizarTablas();
    }

    private void inicializarComponentes() {
        String[] columnasPCB = {"ID", "Nombre", "Status", "PC", "MAR", "Prio", "Deadline"};

        // --- ZONA OESTE: RAM ---
        JPanel panelRAM = new JPanel(new GridLayout(2, 1, 5, 5));
        panelRAM.setOpaque(false);
        modeloListos = new DefaultTableModel(columnasPCB, 0);
        tablaListos = new JTable(modeloListos);
        panelRAM.add(crearPanelTabla("Ready Queue (RAM)", tablaListos, colorVerde));

        modeloBloqueados = new DefaultTableModel(columnasPCB, 0);
        tablaBloqueados = new JTable(modeloBloqueados);
        panelRAM.add(crearPanelTabla("Blocked Queue (RAM - I/O)", tablaBloqueados, colorRojo));

        // --- ZONA ESTE: SWAP (DISCO) ---
        JPanel panelDisco = new JPanel(new GridLayout(2, 1, 5, 5));
        panelDisco.setOpaque(false);
        modeloListosSusp = new DefaultTableModel(columnasPCB, 0);
        tablaListosSusp = new JTable(modeloListosSusp);
        panelDisco.add(crearPanelTabla("Swap Space: Ready-Suspended", tablaListosSusp, Color.GRAY));

        modeloBloqSusp = new DefaultTableModel(columnasPCB, 0);
        tablaBloqSusp = new JTable(modeloBloqSusp);
        panelDisco.add(crearPanelTabla("Swap Space: Blocked-Suspended", tablaBloqSusp, Color.GRAY));

        // --- ZONA CENTRAL: CPU Y CONTROLES ---
        JPanel panelCentral = new JPanel(new BorderLayout());
        panelCentral.setOpaque(false);

        JPanel panelCPU = new JPanel(new GridLayout(4, 1, 5, 5));
        panelCPU.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(colorBorde), "RUNNING PROCESS (CPU)", 0, 0, null, colorBorde));
        panelCPU.setBackground(colorPanel);
        
        lblRelojGlobal = new JLabel("MISSION CLOCK: Cycle 0", SwingConstants.CENTER);
        lblRelojGlobal.setForeground(colorBorde);
        lblRelojGlobal.setFont(new Font("Impact", Font.PLAIN, 26));
        
        lblMemoriaRAM = new JLabel("Memory Usage: 0/5", SwingConstants.CENTER);
        lblMemoriaRAM.setForeground(colorVerde);
        lblMemoriaRAM.setFont(new Font("Consolas", Font.BOLD, 14));

        lblProcesoActual = new JLabel("IDLE - Esperando Procesos...", SwingConstants.CENTER);
        lblProcesoActual.setForeground(Color.WHITE);
        lblProcesoActual.setFont(new Font("Consolas", Font.BOLD, 16));
        
        barraProgresoCPU = new JProgressBar(0, 100);
        barraProgresoCPU.setStringPainted(true);
        barraProgresoCPU.setForeground(colorVerde);
        barraProgresoCPU.setBackground(Color.BLACK);

        panelCPU.add(lblRelojGlobal);
        panelCPU.add(lblMemoriaRAM);
        panelCPU.add(lblProcesoActual);
        panelCPU.add(barraProgresoCPU);
        
        JPanel panelControles = new JPanel(new FlowLayout());
        panelControles.setBackground(colorPanel);
        panelControles.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        cmbAlgoritmos = new JComboBox<>(new String[]{"FCFS", "RR", "Prioridad", "SRT", "EDF"});
        btnIniciar = new JButton("Iniciar Simulación");
        btnCrearProceso = new JButton("Crear 1 Proceso");
        btnCrear20 = new JButton("Generar 20 Aleatorios");
        btnCargarArchivo = new JButton("Cargar CSV");
        
        spnQuantum = new JSpinner(new SpinnerNumberModel(5, 1, 20, 1));
        spnVelocidad = new JSpinner(new SpinnerNumberModel(1000, 100, 5000, 100));

        estilizarBoton(btnIniciar, colorVerde);
        estilizarBoton(btnCrearProceso, colorBorde);
        estilizarBoton(btnCrear20, colorBorde);
        estilizarBoton(btnCargarArchivo, Color.LIGHT_GRAY);

        btnIniciar.addActionListener(e -> iniciarSimulacion());
        btnCrearProceso.addActionListener(e -> generarProcesosAleatorios(1));
        btnCrear20.addActionListener(e -> generarProcesosAleatorios(20));
        btnCargarArchivo.addActionListener(e -> cargarDesdeArchivo());

        panelControles.add(new JLabel("<html><font color='white'>Algoritmo:</font></html>"));
        panelControles.add(cmbAlgoritmos);
        panelControles.add(new JLabel("<html><font color='white'>Quantum:</font></html>"));
        panelControles.add(spnQuantum);
        panelControles.add(new JLabel("<html><font color='white'>Velocidad(ms):</font></html>"));
        panelControles.add(spnVelocidad);
        panelControles.add(btnIniciar);
        
        JPanel panelExtraBotones = new JPanel();
        panelExtraBotones.setOpaque(false);
        panelExtraBotones.add(btnCrearProceso);
        panelExtraBotones.add(btnCrear20);
        panelExtraBotones.add(btnCargarArchivo);

        JPanel surCentral = new JPanel(new BorderLayout());
        surCentral.add(panelControles, BorderLayout.NORTH);
        surCentral.add(panelExtraBotones, BorderLayout.SOUTH);

        panelCentral.add(panelCPU, BorderLayout.CENTER);
        panelCentral.add(surCentral, BorderLayout.SOUTH);

        // --- ZONA SUR: LOG DE EVENTOS ---
        txtLogEventos = new JTextArea(6, 50);
        txtLogEventos.setEditable(false);
        txtLogEventos.setBackground(Color.BLACK);
        txtLogEventos.setForeground(colorVerde);
        txtLogEventos.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollLog = new JScrollPane(txtLogEventos);
        scrollLog.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), "System Log", 0, 0, null, colorTexto));

        // ENSAMBLAR
        add(panelRAM, BorderLayout.WEST);
        add(panelDisco, BorderLayout.EAST);
        add(panelCentral, BorderLayout.CENTER);
        add(scrollLog, BorderLayout.SOUTH);
    }

    // --- 3. LÓGICA DE LA SIMULACIÓN ---
    private void iniciarSimulacion() {
        if (corriendo) return;
        corriendo = true;
        escribirLog("Sistema Operativo Iniciado.");

        // HILO DEL PLANIFICADOR
        Thread hiloSimulacion = new Thread(() -> {
            while (corriendo) {
                try {
                    semaforoSistema.acquire(); // Bloquear para asegurar integridad
                    
                    int quantumActual = (Integer) spnQuantum.getValue();
                    int velocidad = (Integer) spnVelocidad.getValue();
                    relojGlobal++;
                    
                    SwingUtilities.invokeLater(() -> lblRelojGlobal.setText("MISSION CLOCK: Cycle " + relojGlobal));

                    gestionarBloqueados();
                    revisarSuspendidos();

                    // PLANIFICADOR
                    if (procesoEnCpu == null) {
                        if (!colaListos.isEmpty()) {
                            procesoEnCpu = colaListos.desencolar();
                            procesoEnCpu.setEstado("Ejecucion");
                            contadorQuantum = 0;
                            
                            PCB pRef = procesoEnCpu;
                            SwingUtilities.invokeLater(() -> lblProcesoActual.setText(pRef.getNombre() + " [ID:" + pRef.getId() + "]"));
                        } else {
                            SwingUtilities.invokeLater(() -> {
                                lblProcesoActual.setText("IDLE - CPU Inactiva");
                                barraProgresoCPU.setValue(0);
                                barraProgresoCPU.setString("0 / 0");
                            });
                        }
                    }

                    // EJECUCIÓN
                    if (procesoEnCpu != null) {
                        if (relojGlobal > procesoEnCpu.getDeadline()) {
                            escribirLog("FALLO DEADLINE: " + procesoEnCpu.getNombre() + " abortado.");
                            misGraficas.registrarFallo();
                            procesoEnCpu.setEstado("Terminado");
                            procesoEnCpu = null;
                        } 
                        else if (procesoEnCpu.getCicloParaBloqueo() != -1 && procesoEnCpu.getInstruccionesEjecutadas() == procesoEnCpu.getCicloParaBloqueo()) {
                            escribirLog("INTERRUPCIÓN E/S: " + procesoEnCpu.getNombre() + " se bloquea.");
                            procesoEnCpu.setEstado("Bloqueado");
                            
                            if (getOcupacionMemoria() < MAX_MEMORIA) {
                                colaBloqueados.encolar(procesoEnCpu);
                            } else {
                                procesoEnCpu.setEstado("Bloqueado-Suspendido");
                                colaBloqueadosSuspendidos.encolar(procesoEnCpu);
                            }
                            procesoEnCpu = null;
                        } 
                        else {
                            procesoEnCpu.avanzarInstruccion();
                            contadorQuantum++;

                            int total = procesoEnCpu.getInstruccionesTotales();
                            int actual = procesoEnCpu.getInstruccionesEjecutadas();
                            int porcentaje = (actual * 100) / total;
                            
                            SwingUtilities.invokeLater(() -> {
                                barraProgresoCPU.setValue(porcentaje);
                                barraProgresoCPU.setString(actual + " / " + total + " Instr | Deadline en: " + (procesoEnCpu.getDeadline() - relojGlobal));
                                if(porcentaje > 80) barraProgresoCPU.setForeground(Color.ORANGE);
                                else barraProgresoCPU.setForeground(colorVerde);
                            });

                            if (actual >= total) {
                                escribirLog("ÉXITO: " + procesoEnCpu.getNombre() + " finalizado correctamente.");
                                misGraficas.registrarExito();
                                procesoEnCpu.setEstado("Terminado");
                                procesoEnCpu = null;
                                revisarSuspendidos();
                            } 
                            else if (cmbAlgoritmos.getSelectedItem().equals("RR") && contadorQuantum >= quantumActual) {
                                escribirLog("TIMEOUT: " + procesoEnCpu.getNombre() + " agota su Quantum.");
                                procesoEnCpu.setEstado("Listo");
                                agregarAColaListos(procesoEnCpu);
                                procesoEnCpu = null;
                                contadorQuantum = 0;
                            }
                        }
                    }

                    actualizarTablas();
                    actualizarMemoriaVisual();
                    semaforoSistema.release(); // Liberar paso
                    
                    Thread.sleep(velocidad);

                } catch (InterruptedException e) { e.printStackTrace(); }
            }
        });
        hiloSimulacion.start();
        
        // HILO DE INTERRUPCIONES ASÍNCRONAS
        Thread hiloInterrupciones = new Thread(() -> {
            while (corriendo) {
                try {
                    int tiempoEspera = (int)(Math.random() * 15000) + 15000; 
                    Thread.sleep(tiempoEspera);

                    semaforoSistema.acquire(); // Interrumpimos el sistema
                    PCB interrupcion = new PCB("¡ALERTA MICRO-METEORITO!", 0, 15, relojGlobal + 30);
                    interrupcion.setEstado("Listo");
                    colaListos.insertarOrdenado(interrupcion, Cola.CRITERIO_PRIORIDAD); 
                    
                    escribirLog("¡¡¡ EMERGENCIA !!! Interrupción de hardware detectada.");
                    actualizarTablas();
                    semaforoSistema.release();

                } catch (InterruptedException e) { e.printStackTrace(); }
            }
        });
        hiloInterrupciones.start();
    }

    // --- 4. GESTIÓN DE COLAS Y MÉTODOS AUXILIARES ---
    private void gestionarBloqueados() {
        int sizeBloq = colaBloqueados.getSize();
        for(int i=0; i<sizeBloq; i++) {
            PCB b = colaBloqueados.desencolar();
            if(b != null) {
                b.setLongitudBloqueo(b.getLongitudBloqueo() - 1);
                if (b.getLongitudBloqueo() <= 0) {
                    b.setEstado("Listo");
                    b.setCicloParaBloqueo(-1);
                    agregarAColaListos(b); 
                } else { colaBloqueados.encolar(b); }
            }
        }
        
        int sizeBloqSusp = colaBloqueadosSuspendidos.getSize();
        for(int i=0; i<sizeBloqSusp; i++) {
            PCB bSusp = colaBloqueadosSuspendidos.desencolar();
            if(bSusp != null) {
                bSusp.setLongitudBloqueo(bSusp.getLongitudBloqueo() - 1);
                if (bSusp.getLongitudBloqueo() <= 0) {
                    bSusp.setEstado("Listo-Suspendido");
                    bSusp.setCicloParaBloqueo(-1);
                    colaListosSuspendidos.encolar(bSusp);
                } else { colaBloqueadosSuspendidos.encolar(bSusp); }
            }
        }
    }

    private void revisarSuspendidos() {
        while (getOcupacionMemoria() < MAX_MEMORIA && !colaListosSuspendidos.isEmpty()) {
            PCB rescatado = colaListosSuspendidos.desencolar();
            if (rescatado != null) {
                rescatado.setEstado("Listo");
                agregarAColaListos(rescatado);
                escribirLog("SWAP IN: " + rescatado.getNombre() + " subió a RAM.");
            }
        }
    }

    public void agregarAColaListos(PCB proceso) {
        String algoritmo = (String) cmbAlgoritmos.getSelectedItem();

        switch (algoritmo) {
            case "FCFS":
            case "RR":
                colaListos.encolar(proceso); 
                break;
            case "Prioridad":
                colaListos.insertarOrdenado(proceso, Cola.CRITERIO_PRIORIDAD); 
                break;
            case "SRT":
                colaListos.insertarOrdenado(proceso, Cola.CRITERIO_SRT); 
                break;
            case "EDF":
                colaListos.insertarOrdenado(proceso, Cola.CRITERIO_EDF); 
                break;
        }

        // PREEMPTION
        if (procesoEnCpu != null) {
            boolean expropiar = false;
            if (algoritmo.equals("Prioridad") && proceso.getPrioridad() < procesoEnCpu.getPrioridad()) expropiar = true;
            else if (algoritmo.equals("SRT") && (proceso.getInstruccionesTotales() - proceso.getInstruccionesEjecutadas()) < (procesoEnCpu.getInstruccionesTotales() - procesoEnCpu.getInstruccionesEjecutadas())) expropiar = true;
            else if (algoritmo.equals("EDF") && proceso.getDeadline() < procesoEnCpu.getDeadline()) expropiar = true;

            if (expropiar) {
                escribirLog("PREEMPTION: " + proceso.getNombre() + " expropia la CPU a " + procesoEnCpu.getNombre());
                PCB saliente = procesoEnCpu;
                saliente.setEstado("Listo");
                
                switch (algoritmo) {
                    case "Prioridad": colaListos.insertarOrdenado(saliente, Cola.CRITERIO_PRIORIDAD); break;
                    case "SRT":       colaListos.insertarOrdenado(saliente, Cola.CRITERIO_SRT); break;
                    case "EDF":       colaListos.insertarOrdenado(saliente, Cola.CRITERIO_EDF); break;
                }
                procesoEnCpu = null; 
                contadorQuantum = 0;
            }
        }
    }

    private void generarProcesosAleatorios(int cantidad) {
        for (int i = 0; i < cantidad; i++) {
            int inst = (int) (Math.random() * 80) + 20;
            int prio = (int) (Math.random() * 3) + 1; 
            int dead = relojGlobal + (int) (Math.random() * 200) + 50; 
            
            PCB nuevo = new PCB("Proc-" + (int)(Math.random() * 1000), prio, inst, dead);
            
            if (getOcupacionMemoria() < MAX_MEMORIA) {
                nuevo.setEstado("Listo");
                agregarAColaListos(nuevo);
            } else {
                nuevo.setEstado("Listo-Suspendido");
                colaListosSuspendidos.encolar(nuevo);
                escribirLog("SWAP OUT: Memoria llena, " + nuevo.getNombre() + " a Disco.");
            }
        }
        revisarSuspendidos();
        actualizarTablas();
        actualizarMemoriaVisual();
    }

    private void cargarDesdeArchivo() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File archivo = fileChooser.getSelectedFile();
            try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(archivo))) {
                String linea;
                while ((linea = br.readLine()) != null) {
                    String[] datos = linea.split(",");
                    if (datos.length >= 4) {
                        PCB nuevo = new PCB(datos[0].trim(), Integer.parseInt(datos[1].trim()), Integer.parseInt(datos[2].trim()), Integer.parseInt(datos[3].trim()));
                        if (getOcupacionMemoria() < MAX_MEMORIA) {
                            nuevo.setEstado("Listo");
                            agregarAColaListos(nuevo);
                        } else {
                            nuevo.setEstado("Listo-Suspendido");
                            colaListosSuspendidos.encolar(nuevo);
                        }
                    }
                }
                escribirLog("Archivo CSV cargado correctamente.");
                actualizarTablas();
                actualizarMemoriaVisual();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error al leer archivo.");
            }
        }
    }

    // --- 5. ACTUALIZACIÓN VISUAL ---
    private void actualizarTablas() {
        SwingUtilities.invokeLater(() -> {
            llenarModeloTabla(modeloListos, colaListos);
            llenarModeloTabla(modeloBloqueados, colaBloqueados);
            llenarModeloTabla(modeloListosSusp, colaListosSuspendidos);
            llenarModeloTabla(modeloBloqSusp, colaBloqueadosSuspendidos);
        });
    }

    private void llenarModeloTabla(DefaultTableModel modelo, Cola<PCB> cola) {
        modelo.setRowCount(0); 
        Object[] procesos = cola.toArray(); 
        if (procesos != null) {
            for (Object obj : procesos) {
                PCB p = (PCB) obj;
                // Columnas: "ID", "Nombre", "Status", "PC", "MAR", "Prio", "Deadline"
                modelo.addRow(new Object[]{
                    p.getId(), p.getNombre(), p.getEstado(), 
                    p.getProgramCounter(), p.getInstruccionesEjecutadas(), 
                    p.getPrioridad(), (p.getDeadline() - relojGlobal)
                });
            }
        }
    }

    private int getOcupacionMemoria() {
        int ocupados = colaListos.getSize() + colaBloqueados.getSize();
        if (procesoEnCpu != null) ocupados++;
        return ocupados;
    }

    private void actualizarMemoriaVisual() {
        int oc = getOcupacionMemoria();
        SwingUtilities.invokeLater(() -> {
            lblMemoriaRAM.setText("Memory Usage: " + oc + "/" + MAX_MEMORIA);
            if (oc >= MAX_MEMORIA) lblMemoriaRAM.setForeground(colorRojo);
            else lblMemoriaRAM.setForeground(colorVerde);
        });
    }

    private void escribirLog(String mensaje) {
        SwingUtilities.invokeLater(() -> {
            txtLogEventos.append("[" + relojGlobal + "] " + mensaje + "\n");
            txtLogEventos.setCaretPosition(txtLogEventos.getDocument().getLength());
        });
    }

    // --- 6. MÉTODOS DE ESTILO ---
    private JScrollPane crearPanelTabla(String titulo, JTable tabla, Color color) {
        tabla.setBackground(colorPanel);
        tabla.setForeground(Color.WHITE);
        tabla.setFont(new Font("Consolas", Font.PLAIN, 12));
        tabla.getTableHeader().setBackground(Color.DARK_GRAY);
        tabla.getTableHeader().setForeground(color);
        
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setPreferredSize(new Dimension(300, 0));
        scroll.getViewport().setBackground(colorPanel);
        scroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), titulo, 0, 0, null, colorTexto));
        return scroll;
    }

    private void estilizarBoton(JButton btn, Color color) {
        btn.setBackground(color);
        btn.setForeground(Color.BLACK);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }
}