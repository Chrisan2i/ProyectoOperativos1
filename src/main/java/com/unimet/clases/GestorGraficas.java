package com.unimet.clases;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import javax.swing.JFrame;
import java.awt.GridLayout;

public class GestorGraficas extends JFrame {
    
    private DefaultPieDataset datasetPie;
    private XYSeries serieUsoCPU; // Nueva serie para la línea de CPU
    private int exitos = 0;
    private int fallos = 0;

    public GestorGraficas() {
        super("Métricas de Rendimiento del RTOS");
        
        // Dividimos la ventana en 2 filas (Arriba: Pastel, Abajo: Líneas)
        this.setLayout(new GridLayout(2, 1));
        
        // 1. GRÁFICA DE PASTEL (Éxitos vs Fallos)
        datasetPie = new DefaultPieDataset();
        datasetPie.setValue("Éxitos (Deadline Cumplido)", 0);
        datasetPie.setValue("Fallos (Deadline Vencido)", 0);
        JFreeChart pieChart = ChartFactory.createPieChart("Tasa de Éxito de la Misión", datasetPie, true, true, false);
        this.add(new ChartPanel(pieChart));

        // 2. GRÁFICA DE LÍNEAS (Uso de CPU en el tiempo)
        serieUsoCPU = new XYSeries("Uso de CPU (%)");
        XYSeriesCollection datasetXY = new XYSeriesCollection(serieUsoCPU);
        JFreeChart xyChart = ChartFactory.createXYLineChart(
                "Utilización del Procesador", 
                "Ciclo de Reloj", 
                "Uso del CPU (%)", 
                datasetXY
        );
        this.add(new ChartPanel(xyChart));
        
        // Configuración ventana
        this.setSize(500, 800); // Más alta para que quepan ambas
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setVisible(true);
    }

    public void registrarExito() { exitos++; actualizarPie(); }
    public void registrarFallo() { fallos++; actualizarPie(); }

    private void actualizarPie() {
        javax.swing.SwingUtilities.invokeLater(() -> {
            datasetPie.setValue("Éxitos (Deadline Cumplido)", exitos);
            datasetPie.setValue("Fallos (Deadline Vencido)", fallos);
        });
    }

    // Nuevo método para que el simulador le mande el uso de CPU cada ciclo
    public void agregarDatoCPU(int cicloDelReloj, double usoPorcentual) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            serieUsoCPU.add(cicloDelReloj, usoPorcentual);
        });
    }
}