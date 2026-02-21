package com.unimet.interfaz;

import com.unimet.clases.GestorGraficas;
import com.unimet.clases.PCB;
import com.unimet.estructuras.Cola;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.concurrent.Semaphore;

public class VentanaSimulador extends JFrame {

    // --- 1. COMPONENTES VISUALES ---
    private DefaultTableModel modeloListos, modeloBloqueados, modeloListosSusp, modeloBloqSusp;
    private JTable tablaListos, tablaBloqueados, tablaListosSusp, tablaBloqSusp;

    private JLabel lblProcesoActual, lblRelojGlobal, lblMemoriaRAM, lblModoSistema, lblMetricas;
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
    
    // Variables para las métricas del 20 (Sin usar Collections de Java)
    private int totalProcesosTerminados = 0;
    private double sumaTiempoEspera = 0;
    private int[] historialCPU = new int[10]; // Guardará 1 si se usó, 0 si estuvo libre
    private int indiceHistorial = 0;
    
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
        setSize(1450, 850); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(15, 15));
        getContentPane().setBackground(colorFondo);
        ((JComponent)getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        misGraficas = new GestorGraficas(); 
        inicializarComponentes();
        
        generarProcesosAleatorios(5); 
        actualizarTablas();
    }

    private void inicializarComponentes() {
        String[] columnasPCB = {"ID", "Nombre", "Status", "PC", "MAR", "Prio", "Deadline"};

        // --- ZONA OESTE: RAM ---
        JPanel panelRAM = new JPanel(new GridLayout(2, 1, 10, 10));
        panelRAM.setOpaque(false);
        modeloListos = new DefaultTableModel(columnasPCB, 0);
        tablaListos = new JTable(modeloListos);
        panelRAM.add(crearPanelTabla("Ready Queue (RAM)", tablaListos, colorVerde));

        modeloBloqueados = new DefaultTableModel(columnasPCB, 0);
        tablaBloqueados = new JTable(modeloBloqueados);
        panelRAM.add(crearPanelTabla("Blocked Queue (RAM - I/O)", tablaBloqueados, colorRojo));

        // --- ZONA ESTE: SWAP (DISCO) ---
        JPanel panelDisco = new JPanel(new GridLayout(2, 1, 10, 10));
        panelDisco.setOpaque(false);
        modeloListosSusp = new DefaultTableModel(columnasPCB, 0);
        tablaListosSusp = new JTable(modeloListosSusp);
        panelDisco.add(crearPanelTabla("Swap Space: Ready-Suspended", tablaListosSusp, Color.GRAY));

        modeloBloqSusp = new DefaultTableModel(columnasPCB, 0);
        tablaBloqSusp = new JTable(modeloBloqSusp);
        panelDisco.add(crearPanelTabla("Swap Space: Blocked-Suspended", tablaBloqSusp, Color.GRAY));

        // --- ZONA CENTRAL: CPU Y CONTROLES ---
        JPanel panelCentral = new JPanel(new BorderLayout(0, 15));
        panelCentral.setOpaque(false);
        panelCentral.setBorder(new EmptyBorder(0, 10, 0, 10)); 

        JPanel panelCPU = new JPanel(new GridLayout(5, 1, 10, 10));
        panelCPU.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(colorBorde, 2), " RUNNING PROCESS (CPU) ", TitledBorder.LEFT, TitledBorder.TOP, new Font("Consolas", Font.BOLD, 14), colorBorde),
                new EmptyBorder(20, 20, 20, 20)
        ));
        panelCPU.setBackground(colorPanel);
        
        lblRelojGlobal = new JLabel("MISSION CLOCK: Cycle 0", SwingConstants.CENTER);
        lblRelojGlobal.setForeground(colorBorde);
        lblRelojGlobal.setFont(new Font("Impact", Font.PLAIN, 32));
        
        lblModoSistema = new JLabel("MODO: SISTEMA OPERATIVO", SwingConstants.CENTER);
        lblModoSistema.setForeground(Color.YELLOW);
        lblModoSistema.setFont(new Font("Consolas", Font.BOLD, 18));
        
        lblMemoriaRAM = new JLabel("Memory Usage: 0/5", SwingConstants.CENTER);
        lblMemoriaRAM.setForeground(colorVerde);
        lblMemoriaRAM.setFont(new Font("Consolas", Font.BOLD, 16));

        lblProcesoActual = new JLabel("IDLE - Esperando Procesos...", SwingConstants.CENTER);
        lblProcesoActual.setForeground(Color.WHITE);
        lblProcesoActual.setFont(new Font("Consolas", Font.BOLD, 18));
        
        barraProgresoCPU = new JProgressBar(0, 100);
        barraProgresoCPU.setStringPainted(true);
        barraProgresoCPU.setForeground(colorVerde);
        barraProgresoCPU.setBackground(Color.decode("#0F172A"));
        barraProgresoCPU.setFont(new Font("Consolas", Font.BOLD, 14));
        barraProgresoCPU.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        panelCPU.add(lblRelojGlobal);
        panelCPU.add(lblModoSistema);
        panelCPU.add(lblMemoriaRAM);
        panelCPU.add(lblProcesoActual);
        panelCPU.add(barraProgresoCPU);
        
        // --- AQUÍ ESTÁ LA SOLUCIÓN DEL BOTÓN OCULTO ---
        // Usamos un GridLayout de 2 filas para asegurar que los botones no se oculten
        JPanel panelControles = new JPanel(new GridLayout(2, 1, 5, 5));
        panelControles.setBackground(colorPanel);
        panelControles.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                new EmptyBorder(10, 5, 10, 5)
        ));

        cmbAlgoritmos = new JComboBox<>(new String[]{"FCFS", "RR", "Prioridad", "SRT", "EDF"});
        // Escuchador que detecta cuando el usuario cambia el algoritmo en la interfaz
        cmbAlgoritmos.addActionListener(e -> {
            reorganizarPorCambioAlgoritmo();
        });
        cmbAlgoritmos.setFont(new Font("Consolas", Font.BOLD, 14));
        cmbAlgoritmos.addActionListener(e -> {
            String algoritmo = (String) cmbAlgoritmos.getSelectedItem();
            int criterio = -1;
            contadorQuantum = 0;
            
            if (algoritmo.equals("Prioridad")) criterio = Cola.CRITERIO_PRIORIDAD;
            else if (algoritmo.equals("SRT")) criterio = Cola.CRITERIO_SRT;
            else if (algoritmo.equals("EDF")) criterio = Cola.CRITERIO_EDF;

            if (criterio != -1) {
                colaListos.ordenar(criterio);
                colaListosSuspendidos.ordenar(criterio);
            }
            
            actualizarTablas(); 
            escribirLog("Algoritmo modificado a " + algoritmo + ". Colas reordenadas dinámicamente.");
        });
        
        btnIniciar = new JButton("Iniciar Simulación");
        btnCrearProceso = new JButton("Crear 1 Proceso");
        btnCrear20 = new JButton("Generar 20 Aleatorios");
        btnCargarArchivo = new JButton("Cargar CSV");
        
        spnQuantum = new JSpinner(new SpinnerNumberModel(5, 1, 20, 1));
        spnVelocidad = new JSpinner(new SpinnerNumberModel(1000, 100, 5000, 100));
        
        JComponent editorQuantum = spnQuantum.getEditor();
        JComponent editorVelocidad = spnVelocidad.getEditor();
        if (editorQuantum instanceof JSpinner.DefaultEditor) {
            ((JSpinner.DefaultEditor)editorQuantum).getTextField().setFont(new Font("Consolas", Font.BOLD, 14));
            ((JSpinner.DefaultEditor)editorVelocidad).getTextField().setFont(new Font("Consolas", Font.BOLD, 14));
        }

        estilizarBoton(btnIniciar, colorVerde);
        estilizarBoton(btnCrearProceso, colorBorde);
        estilizarBoton(btnCrear20, colorBorde);
        estilizarBoton(btnCargarArchivo, Color.LIGHT_GRAY);

        btnIniciar.addActionListener(e -> iniciarSimulacion());
        btnCrearProceso.addActionListener(e -> generarProcesosAleatorios(1));
        btnCrear20.addActionListener(e -> generarProcesosAleatorios(20));
        btnCargarArchivo.addActionListener(e -> cargarDesdeArchivo());

        // Fila 1: Opciones
        JPanel filaOpciones = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        filaOpciones.setOpaque(false);
        filaOpciones.add(new JLabel("<html><font color='white' size='4'>Algoritmo:</font></html>"));
        filaOpciones.add(cmbAlgoritmos);
        filaOpciones.add(new JLabel("<html><font color='white' size='4'>Quantum:</font></html>"));
        filaOpciones.add(spnQuantum);
        filaOpciones.add(new JLabel("<html><font color='white' size='4'>Velocidad(ms):</font></html>"));
        filaOpciones.add(spnVelocidad);

        // Fila 2: Botones (Todos agrupados para que ninguno se pierda)
        JPanel filaBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        filaBotones.setOpaque(false);
        filaBotones.add(btnIniciar);
        filaBotones.add(btnCrearProceso);
        filaBotones.add(btnCrear20);
        filaBotones.add(btnCargarArchivo);

        panelControles.add(filaOpciones);
        panelControles.add(filaBotones);
        
        lblMetricas = new JLabel("Throughput: 0.00 proc/ciclo | Espera Promedio: 0.0 ciclos", SwingConstants.CENTER);
        lblMetricas.setForeground(Color.CYAN);
        lblMetricas.setFont(new Font("Consolas", Font.BOLD, 15));
        lblMetricas.setBorder(new EmptyBorder(10, 0, 0, 0));

        JPanel surCentral = new JPanel(new BorderLayout(0, 5));
        surCentral.setOpaque(false);
        surCentral.add(panelControles, BorderLayout.CENTER);
        surCentral.add(lblMetricas, BorderLayout.SOUTH);

        panelCentral.add(panelCPU, BorderLayout.CENTER);
        panelCentral.add(surCentral, BorderLayout.SOUTH);

        // --- ZONA SUR: LOG DE EVENTOS ---
        txtLogEventos = new JTextArea(6, 50);
        txtLogEventos.setEditable(false);
        txtLogEventos.setBackground(Color.BLACK);
        txtLogEventos.setForeground(colorVerde);
        txtLogEventos.setFont(new Font("Monospaced", Font.PLAIN, 13));
        
        txtLogEventos.setBorder(new EmptyBorder(5, 10, 5, 10)); 
        
        JScrollPane scrollLog = new JScrollPane(txtLogEventos);
        scrollLog.setBackground(colorFondo); 
        scrollLog.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), " System Log ", TitledBorder.LEFT, TitledBorder.TOP, new Font("Consolas", Font.BOLD, 14), Color.WHITE),
            new EmptyBorder(5, 5, 5, 5)
        ));

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

        Thread hiloSimulacion = new Thread(() -> {
            while (corriendo) {
                try {
                    semaforoSistema.acquire(); 
                    
                    int quantumActual = (Integer) spnQuantum.getValue();
                    int velocidad = (Integer) spnVelocidad.getValue();
                    relojGlobal++;
                    
                    SwingUtilities.invokeLater(() -> {
                        lblRelojGlobal.setText("MISSION CLOCK: Cycle " + relojGlobal);
                        lblModoSistema.setText("MODO: SISTEMA OPERATIVO");
                        lblModoSistema.setForeground(Color.YELLOW);
                    });

                    aumentarTiempoEspera(colaListos);
                    aumentarTiempoEspera(colaListosSuspendidos);

                    gestionarBloqueados();
                    revisarSuspendidos();

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

                    registrarUsoCPU();

                    if (procesoEnCpu != null) {
                        SwingUtilities.invokeLater(() -> {
                            lblModoSistema.setText("MODO: USUARIO (" + procesoEnCpu.getNombre() + ")");
                            lblModoSistema.setForeground(Color.CYAN);
                        });

                        if (relojGlobal > procesoEnCpu.getDeadline()) {
                            escribirLog("FALLO DEADLINE: " + procesoEnCpu.getNombre() + " abortado.");
                            misGraficas.registrarFallo();
                            registrarMetricasFinProceso(procesoEnCpu);
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
                                registrarMetricasFinProceso(procesoEnCpu);
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
                    actualizarMetricasGUI(); 
                    semaforoSistema.release(); 
                    
                    Thread.sleep(velocidad);

                } catch (InterruptedException e) { e.printStackTrace(); }
            }
        });
        hiloSimulacion.start();
        
        Thread hiloInterrupciones = new Thread(() -> {
            while (corriendo) {
                try {
                    int tiempoEspera = (int)(Math.random() * 15000) + 15000; 
                    Thread.sleep(tiempoEspera);

                    semaforoSistema.acquire(); 
                    PCB interrupcion = new PCB("¡ALERTA MICRO-METEORITO!", 0, 15, relojGlobal + 30);

                    escribirLog("¡¡¡ EMERGENCIA !!! Interrupción de hardware detectada.");

                    if (procesoEnCpu != null) {
                        escribirLog("SUSPENDIENDO " + procesoEnCpu.getNombre() + " para atender emergencia.");
                        procesoEnCpu.setEstado("Listo");
                        agregarAColaListos(procesoEnCpu); 
                    }

                    procesoEnCpu = interrupcion;
                    procesoEnCpu.setEstado("Ejecucion");
                    contadorQuantum = 0; 

                    actualizarTablas();
                    semaforoSistema.release();

                } catch (InterruptedException e) { e.printStackTrace(); }
            }
        });
        hiloInterrupciones.start();
    }

    // --- NUEVOS MÉTODOS MATEMÁTICOS PARA EL 20 PUNTOS ---
    
    private void aumentarTiempoEspera(Cola<PCB> cola) {
        Object[] procesosArray = cola.toArray();
        if (procesosArray != null) {
            for (Object obj : procesosArray) {
                ((PCB) obj).incrementarTiempoEspera();
            }
        }
    }

    private void registrarMetricasFinProceso(PCB p) {
        totalProcesosTerminados++;
        sumaTiempoEspera += p.getTiempoEsperado();
    }

    private void actualizarMetricasGUI() {
        double throughput = (relojGlobal > 0) ? (double) totalProcesosTerminados / relojGlobal : 0;
        double esperaPromedio = (totalProcesosTerminados > 0) ? sumaTiempoEspera / totalProcesosTerminados : 0;
        
        SwingUtilities.invokeLater(() -> {
            lblMetricas.setText(String.format("Throughput: %.3f proc/ciclo | Espera Promedio: %.1f ciclos", throughput, esperaPromedio));
        });
    }

    private void registrarUsoCPU() {
        historialCPU[indiceHistorial % 10] = (procesoEnCpu != null) ? 1 : 0;
        indiceHistorial++;
        
        int limite = Math.min(indiceHistorial, 10);
        int sumaUso = 0;
        for (int i = 0; i < limite; i++) sumaUso += historialCPU[i];
        
        double porcentajeUso = ((double) sumaUso / limite) * 100.0;
        misGraficas.agregarDatoCPU(relojGlobal, porcentajeUso);
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
            
            String algoritmoActual = (String) cmbAlgoritmos.getSelectedItem();
            int criterio = -1;
            
            if (algoritmoActual.equals("Prioridad")) criterio = Cola.CRITERIO_PRIORIDAD;
            else if (algoritmoActual.equals("SRT")) criterio = Cola.CRITERIO_SRT;
            else if (algoritmoActual.equals("EDF")) criterio = Cola.CRITERIO_EDF;
            
            if (criterio != -1) {
                colaListosSuspendidos.ordenar(criterio); 
            }
            
            PCB rescatado = colaListosSuspendidos.desencolar();
            if (rescatado != null) {
                rescatado.setEstado("Listo");
                agregarAColaListos(rescatado);
                escribirLog("SWAP IN: " + rescatado.getNombre() + " subió a RAM bajo política " + algoritmoActual + ".");
            }
        }
    }

    public void agregarAColaListos(PCB proceso) {
        String algoritmo = cmbAlgoritmos.getSelectedItem().toString();

        // 1. Insertar el proceso en la posición correcta según el algoritmo
        if (algoritmo.equals("EDF")) {
            colaListos.insertarOrdenado(proceso, Cola.CRITERIO_EDF);
        } 
        else if (algoritmo.equals("SRT")) {
            colaListos.insertarOrdenado(proceso, Cola.CRITERIO_SRT);
        } 
        else if (algoritmo.equals("Prioridad")) {
            colaListos.insertarOrdenado(proceso, Cola.CRITERIO_PRIORIDAD);
        } 
        else {
            // Si es FCFS o Round Robin, simplemente se encola al final (FIFO)
            colaListos.encolar(proceso);
        }
        actualizarTablasGUI();

        if (procesoEnCpu != null) {
            boolean expropiar = false;
            if (procesoEnCpu.getPrioridad() != 0) {
                if (algoritmo.equals("Prioridad") && proceso.getPrioridad() < procesoEnCpu.getPrioridad()) expropiar = true;
                else if (algoritmo.equals("SRT") && (proceso.getInstruccionesTotales() - proceso.getInstruccionesEjecutadas()) < (procesoEnCpu.getInstruccionesTotales() - procesoEnCpu.getInstruccionesEjecutadas())) expropiar = true;
                else if (algoritmo.equals("EDF") && proceso.getDeadline() < procesoEnCpu.getDeadline()) expropiar = true;
            }
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
    // Método para refrescar la interfaz gráfica con el estado real de las memorias
    public void actualizarTablasGUI() {
        // 1. Limpiamos visualmente todas las tablas dejándolas en 0 filas
        modeloListos.setRowCount(0);
        modeloListosSusp.setRowCount(0);
        modeloBloqueados.setRowCount(0);
        // modeloBloqSusp.setRowCount(0); // Descomenta si también tienes esta tabla

        // 2. Llenamos la tabla de RAM (Cola de Listos)
        Object[] procesosRAM = colaListos.toArray();
        if (procesosRAM != null) {
            for (Object obj : procesosRAM) {
                PCB p = (PCB) obj;
                // Ajusta las columnas según lo que muestre tu JTable
                modeloListos.addRow(new Object[]{
                    p.getId(), p.getNombre(), p.getPrioridad(), p.getInstruccionesTotales(), p.getDeadline()
                });
            }
        }

        // 3. Llenamos la tabla de Disco (Listos-Suspendidos)
        Object[] procesosDisco = colaListosSuspendidos.toArray();
        if (procesosDisco != null) {
            for (Object obj : procesosDisco) {
                PCB p = (PCB) obj;
                modeloListosSusp.addRow(new Object[]{
                    p.getId(), p.getNombre(), p.getPrioridad(), p.getInstruccionesTotales(), p.getDeadline()
                });
            }
        }

        // 4. Llenamos la tabla de Bloqueados (E/S)
        Object[] procesosBloqueados = colaBloqueados.toArray();
        if (procesosBloqueados != null) {
            for (Object obj : procesosBloqueados) {
                PCB p = (PCB) obj;
                modeloBloqueados.addRow(new Object[]{
                    p.getId(), p.getNombre(), p.getPrioridad(), p.getInstruccionesTotales(), p.getDeadline()
                });
            }
        }
    }

    private void generarProcesosAleatorios(int cantidad) {
        for (int i = 0; i < cantidad; i++) {
            int inst = (int) (Math.random() * 80) + 20;
            int prio = (int) (Math.random() * 3) + 1; 
            int dead = relojGlobal + inst + (int) (Math.random() * 100) + 20;
            
            PCB nuevo = new PCB("Proc-" + (int)(Math.random() * 1000), prio, inst, dead);
            
            gestionarIngresoMemoria(nuevo);
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
            int lineasCargadas = 0;
            reiniciarSistema();
            while ((linea = br.readLine()) != null) {
                if (linea.trim().isEmpty()) continue; // Ignora líneas vacías

                String[] datos = linea.split(",");
                if (datos.length >= 4) {
                    try {
                        // Intentamos extraer los datos
                        String nombre = datos[0].trim();
                        int prio = Integer.parseInt(datos[1].trim());
                        int inst = Integer.parseInt(datos[2].trim());
                        int deadRelativo = Integer.parseInt(datos[3].trim());

                        // Creamos el proceso sumando el relojGlobal para que el deadline sea coherente
                        PCB nuevo = new PCB(nombre, prio, inst, relojGlobal + deadRelativo);
                        
                        // Usamos la lógica de memoria inteligente que creamos antes
                        gestionarIngresoMemoria(nuevo); 
                        lineasCargadas++;
                        
                    } catch (NumberFormatException nfe) {
                        // Si falla el número, escribimos en el log pero NO paramos el programa
                        escribirLog("Línea ignorada (formato inválido): " + linea);
                    }
                }
            }
            escribirLog("Carga finalizada. Se procesaron " + lineasCargadas + " procesos.");
            actualizarTablas();
            actualizarMemoriaVisual();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error crítico: No se pudo acceder al archivo.");
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
                modelo.addRow(new Object[]{
                    p.getId(), p.getNombre(), p.getEstado(), 
                    p.getProgramCounter(), p.getMar(),
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

    // --- 6. MÉTODOS DE ESTILO Y FIX DE LA TABLA ---
    private JScrollPane crearPanelTabla(String titulo, JTable tabla, Color color) {
        tabla.setBackground(colorPanel);
        tabla.setForeground(Color.WHITE);
        tabla.setFont(new Font("Consolas", Font.PLAIN, 13));
        
        tabla.setRowHeight(28); 
        tabla.setSelectionBackground(Color.decode("#38BDF8"));
        tabla.setSelectionForeground(Color.BLACK);
        tabla.setShowGrid(true);
        tabla.setGridColor(Color.decode("#334155"));
        
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setBackground(Color.decode("#0F172A")); 
        headerRenderer.setForeground(color); 
        headerRenderer.setFont(new Font("Consolas", Font.BOLD, 13));
        headerRenderer.setHorizontalAlignment(JLabel.CENTER);
        headerRenderer.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, color)); 

        for (int i = 0; i < tabla.getModel().getColumnCount(); i++) {
            tabla.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }
        
        int[] anchos = {35, 80, 100, 35, 35, 45, 85};
        for (int i = 0; i < tabla.getColumnCount(); i++) {
            if (i < anchos.length) {
                tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
            }
        }
        
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
        cellRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < tabla.getModel().getColumnCount(); i++) {
            tabla.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }
        
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setPreferredSize(new Dimension(380, 0)); 
        scroll.setBackground(colorFondo); 
        scroll.getViewport().setBackground(colorPanel);
        scroll.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), " " + titulo + " ", TitledBorder.LEFT, TitledBorder.TOP, new Font("Consolas", Font.BOLD, 14), Color.WHITE),
            new EmptyBorder(5, 5, 5, 5) 
        ));
        return scroll;
    }

    private void estilizarBoton(JButton btn, Color color) {
        btn.setBackground(color);
        btn.setForeground(Color.BLACK);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setMargin(new Insets(6, 15, 6, 15));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
    }

    public void gestionarIngresoMemoria(PCB nuevo) {
        if (getOcupacionMemoria() < MAX_MEMORIA) {
            // Si hay espacio en la RAM, entra directo
            nuevo.setEstado("Listo");
            agregarAColaListos(nuevo);
        } else {
            // La RAM está llena (5 procesos). 
            String algoritmo = cmbAlgoritmos.getSelectedItem().toString();
            
            // ¡AQUÍ SE CREA LA VARIABLE! Toma la colaListos y la convierte en arreglo
            Object[] procesosEnRAM = colaListos.toArray(); 
            
            PCB peorProceso = null;
            boolean expropiarMemoria = false;

            if (procesosEnRAM != null && procesosEnRAM.length > 0) {
                // 1. Lógica para EDF
                if (algoritmo.equals("EDF")) {
                    int mayorDeadline = -1;
                    for (Object obj : procesosEnRAM) {
                        PCB p = (PCB) obj;
                        if (p.getDeadline() > mayorDeadline) {
                            mayorDeadline = p.getDeadline();
                            peorProceso = p;
                        }
                    }
                    if (peorProceso != null && nuevo.getDeadline() < peorProceso.getDeadline()) expropiarMemoria = true;
                } 
                // 2. Lógica para PRIORIDAD
                else if (algoritmo.equals("Prioridad")) {
                    int peorPrioridad = -1; // Número mayor = peor prioridad
                    for (Object obj : procesosEnRAM) {
                        PCB p = (PCB) obj;
                        if (p.getPrioridad() > peorPrioridad) {
                            peorPrioridad = p.getPrioridad();
                            peorProceso = p;
                        }
                    }
                    if (peorProceso != null && nuevo.getPrioridad() < peorProceso.getPrioridad()) expropiarMemoria = true;
                } 
                // 3. Lógica para SRT
                else if (algoritmo.equals("SRT")) {
                    int mayorTiempoRestante = -1;
                    for (Object obj : procesosEnRAM) {
                        PCB p = (PCB) obj;
                        int restante = p.getInstruccionesTotales() - p.getInstruccionesEjecutadas();
                        if (restante > mayorTiempoRestante) {
                            mayorTiempoRestante = restante;
                            peorProceso = p;
                        }
                    }
                    int nuevoRestante = nuevo.getInstruccionesTotales();
                    if (peorProceso != null && nuevoRestante < mayorTiempoRestante) expropiarMemoria = true;
                }
                // FCFS y RR no expropian memoria (expropiarMemoria sigue false)
            }

            // --- EJECUCIÓN DEL SWAP ---
            if (expropiarMemoria && peorProceso != null) {
                colaListos.remover(peorProceso); 
                peorProceso.setEstado("Listo-Suspendido");
                colaListosSuspendidos.encolar(peorProceso);
                escribirLog("SWAP OUT: " + peorProceso.getNombre() + " a Disco (Haciendo espacio bajo " + algoritmo + ").");
                
                nuevo.setEstado("Listo");
                agregarAColaListos(nuevo);
                escribirLog("SWAP IN: " + nuevo.getNombre() + " a RAM (Cumple criterio de " + algoritmo + ").");
            } else {
                // Si es FCFS, RR, o si el nuevo proceso NO es mejor que los que ya están
                nuevo.setEstado("Listo-Suspendido");
                colaListosSuspendidos.encolar(nuevo);
                escribirLog("SWAP: RAM llena. " + nuevo.getNombre() + " a Disco (Esperando su turno).");
            }
        }
    }
    
    
    // Método maestro para reevaluar RAM y Disco cuando el usuario cambia de algoritmo en vivo
    public void reorganizarPorCambioAlgoritmo() {
        String algoritmo = cmbAlgoritmos.getSelectedItem().toString();
        escribirLog(">>> REEVALUANDO MEMORIA: Cambio de algoritmo a " + algoritmo + " <<<");

        // 1. Obtener los procesos de RAM y Disco
        Object[] enRam = colaListos.toArray();
        Object[] enDisco = colaListosSuspendidos.toArray();
        
        int sizeRam = (enRam != null) ? enRam.length : 0;
        int sizeDisco = (enDisco != null) ? enDisco.length : 0;
        int totalProcesos = sizeRam + sizeDisco;
        
        // Si no hay procesos en el sistema, no hacemos nada para evitar errores
        if (totalProcesos == 0) return; 

        // 2. Unir TODOS los procesos en un solo arreglo (Respetando la restricción de no usar java.util.*)
        PCB[] todos = new PCB[totalProcesos];
        int index = 0;
        if (enRam != null) {
            for (Object obj : enRam) {
                todos[index++] = (PCB) obj;
            }
        }
        if (enDisco != null) {
            for (Object obj : enDisco) {
                todos[index++] = (PCB) obj;
            }
        }

        // 3. ORDENAR el arreglo completo según las reglas del algoritmo seleccionado (Bubble Sort)
        for (int i = 0; i < todos.length - 1; i++) {
            for (int j = 0; j < todos.length - i - 1; j++) {
                boolean intercambiar = false;
                PCB p1 = todos[j];
                PCB p2 = todos[j+1];

                // FCFS y RR: Orden estricto por orden de llegada (ID)
                if (algoritmo.equals("FCFS") || algoritmo.equals("RR")) {
                    if (p1.getId() > p2.getId()) {
                        intercambiar = true;
                    }
                } 
                // EDF: Orden por Deadline más corto. (Desempate: ID)
                else if (algoritmo.equals("EDF")) {
                    if (p1.getDeadline() > p2.getDeadline()) {
                        intercambiar = true;
                    } else if (p1.getDeadline() == p2.getDeadline() && p1.getId() > p2.getId()) {
                        intercambiar = true; 
                    }
                } 
                // PRIORIDAD: Orden por prioridad menor. (Desempate: ID)
                else if (algoritmo.equals("Prioridad")) {
                    if (p1.getPrioridad() > p2.getPrioridad()) {
                        intercambiar = true;
                    } else if (p1.getPrioridad() == p2.getPrioridad() && p1.getId() > p2.getId()) {
                        intercambiar = true; 
                    }
                } 
                // SRT: Orden por tiempo restante menor. (Desempate: ID)
                else if (algoritmo.equals("SRT")) {
                    int restante1 = p1.getInstruccionesTotales() - p1.getInstruccionesEjecutadas();
                    int restante2 = p2.getInstruccionesTotales() - p2.getInstruccionesEjecutadas();
                    
                    if (restante1 > restante2) {
                        intercambiar = true;
                    } else if (restante1 == restante2 && p1.getId() > p2.getId()) {
                        intercambiar = true; 
                    }
                }

                // Ejecutamos el intercambio si alguna condición se cumplió
                if (intercambiar) {
                    PCB temp = todos[j];
                    todos[j] = todos[j+1];
                    todos[j+1] = temp;
                }
            }
        }

        // 4. Vaciar las colas actuales para reconstruirlas impecablemente
        colaListos = new Cola<>();
        colaListosSuspendidos = new Cola<>();

        // 5. Repartir el arreglo ya ordenado: los primeros a RAM, el resto a Disco
        for (int i = 0; i < todos.length; i++) {
            PCB p = todos[i];
            
            if (i < MAX_MEMORIA) { 
                // Hay espacio en la RAM (Los 5 mejores procesos)
                p.setEstado("Listo");
                colaListos.encolar(p);
            } else {
                // La RAM ya se llenó, los siguientes van al disco (Swap)
                p.setEstado("Listo-Suspendido");
                colaListosSuspendidos.encolar(p);
            }
        }

        // 6. Actualizar las tablas visuales para reflejar la nueva distribución
        actualizarTablasGUI();
    }
    // Método para limpiar el sistema antes de cargar un nuevo escenario (CSV)
    public void reiniciarSistema() {
        escribirLog(">>> INICIANDO REINICIO DEL SISTEMA <<<");

        // 1. Vaciamos TODAS las memorias creando colas nuevas (sin usar java.util)
        colaListos = new Cola<>();
        colaListosSuspendidos = new Cola<>();
        // 3. Actualizamos las tablas para que la interfaz quede en blanco
        actualizarTablasGUI();
        
        escribirLog("--- SISTEMA LIMPIO: Listo para cargar nueva misión ---");
    }
}