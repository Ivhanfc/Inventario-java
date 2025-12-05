package app_java;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import javax.swing.SwingUtilities;

@SpringBootApplication

public class App {
    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            UiGeneral ui = new UiGeneral();
            ui.setVisible(true);

        });

        // aqui se inicia el servidor
        SpringApplication.run(App.class, args);

    }
}
