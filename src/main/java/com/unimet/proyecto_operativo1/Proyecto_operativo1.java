
package com.unimet.proyecto_operativo1;
import com.unimet.interfaz.VentanaSimulador;
import javax.swing.SwingUtilities;

public class Proyecto_operativo1 {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new VentanaSimulador().setVisible(true);
        });
    }
}
