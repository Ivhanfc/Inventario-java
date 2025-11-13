package com.ivhanfc.scannerjs.app_java;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import javax.swing.SwingUtilities;
import com.ivhanfc.scannerjs.app_java.UI;
import com.ivhanfc.scannerjs.app_java.Database;



@SpringBootApplication

public class App {
    public static void main(String[]args) {
          //aqui inicializamos la inferfaz grafica (javaswing)
    SwingUtilities.invokeLater(() -> {
           UI.installNimbusDarkish();
            UI ui = new UI();
            ui.setVisible(true);
        });
       
        //aqui se inicia el servidor
               SpringApplication.run(App.class, args);
    
 
    }}
