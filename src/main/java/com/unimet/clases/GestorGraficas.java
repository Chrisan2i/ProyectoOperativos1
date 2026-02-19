
package com.unimet.clases;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import javax.swing.JFrame;
import java.awt.BorderLayout;

/**
 *
 * @author chris
 */
public class GestorGraficas extends JFrame {
    
    private DefaultPieDataset dataset;
    private int exitos = 0;
    private int fallos = 0;

    public GestorGraficas() {
        super("Métricas de Rendimiento - Tasa de Éxito");
        
        // 1. Crear el set de datos inicial
        dataset = new DefaultPieDataset();
        dataset.setValue("Éxitos (Deadline Cumplido)", 0);
        dataset.setValue("Fallos (Deadline Vencido)", 0);

        // 2. Crear la gráfica
        JFreeChart chart = ChartFactory.createPieChart(
                "Tasa de Éxito de la Misión", // Título
                dataset,                      // Datos
                true,                         // Leyenda
                true,
                false
        );

        // 3. Ponerla en un Panel
        ChartPanel panel = new ChartPanel(chart);
        this.setLayout(new BorderLayout());
        this.add(panel, BorderLayout.CENTER);
        
        // 4. Configurar ventana
        this.setSize(400, 400);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // No cerrar, solo esconder
        this.setVisible(true);
    }

    public void registrarExito() {
        exitos++;
        actualizar();
    }

    public void registrarFallo() {
        fallos++;
        actualizar();
    }

    private void actualizar() {
        // JFreeChart maneja esto bastante bien, pero por seguridad de hilos:
        javax.swing.SwingUtilities.invokeLater(() -> {
            dataset.setValue("Éxitos (Deadline Cumplido)", exitos);
            dataset.setValue("Fallos (Deadline Vencido)", fallos);
        });
    }
}
