// Guia rapida JavaFX para el equipo:
// 1) Contenedores de layout: StackPane (apila), VBox/HBox (vertical/horizontal), GridPane (celdas), BorderPane (top/left/center/right/bottom).
// 2) Controles basicos: Label, Button, TextField, PasswordField, TextArea, CheckBox, RadioButton, ComboBox, DatePicker.
// 3) Controles avanzados: TableView y ListView (listas/tablas con celdas personalizables), TreeView, Pagination.
// 4) Controles de menu: MenuBar y MenuItem, ToolBar con Button/ToggleButton, ContextMenu para clic derecho.
// 5) Media y grafico: ImageView (mostrar imagen), Canvas (dibujar), Chart (BarChart, PieChart, LineChart), WebView (render HTML).
// 6) Dialogos/modales: Alert (info, warning, error, confirm), Dialog personalizado, FileChooser/DirectoryChooser.
// 7) Layout decorativo: Separator, TitledPane, Accordion, TabPane para secciones.
// 8) Eventos tipicos: setOnAction en botones/menus; setOnKeyPressed / setOnMouseClicked en nodos; listeners a propiedades (textProperty(), selectedProperty()).
// 9) Estilos: CSS externo con getStylesheets().add(...); clases con getStyleClass().add("nombre"); ids con setId("nombre").
// 10) Escena y ventana: Scene(root, ancho, alto) colocada en Stage con setScene; puedes abrir mas Stages para nuevas ventanas.
// 11) Layout responsive: usa VBox/HBox con setSpacing, setPadding, setAlignment; HGrow/VGrow en contenedores; GridPane.setHgrow(node, Priority.ALWAYS).
// 12) Iconos: setGraphic en Buttons/Labels con ImageView; carga imagenes desde resources con getResource("ruta").
// 13) Animaciones: clases de javafx.animation (FadeTransition, TranslateTransition, Timeline) sobre nodos.
// 14) Data binding: usa property().bind(...) o bindBidirectional(...) entre controles y modelos para sincronizar valores.
// 15) Validaciones: textProperty().addListener para verificar input; pseudo-classes para estilos de error.
// 16) Atajos: escena.setOnKeyPressed para manejar ESC, ENTER, etc.; tambien puedes usar MnemonicParsing en botones.
// 17) Drag and Drop: setOnDragDetected, setOnDragOver, setOnDragDropped en nodos para mover datos/archivos.
// 18) Accesibilidad: setAccessibleText en nodos, y orden de focus via setFocusTraversable.
// 19) Internacionalizacion: usa ResourceBundle al cargar vistas (FXML) o para textos centralizados.
// 20) FXML (opcional): define vistas en .fxml y controladores anotados con @FXML; carga con FXMLLoader si quieres separar UI de logica.

import java.util.Objects;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) {
        Label title = new Label("Inventory Management");
        title.getStyleClass().add("title");

        Label subtitle = new Label("Inicia sesion para continuar");
        subtitle.getStyleClass().add("subtitle");

        TextField username = new TextField();
        username.setPromptText("Correo o usuario");

        PasswordField password = new PasswordField();
        password.setPromptText("Contrasena");

        Button loginButton = new Button("Acceder");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.getStyleClass().add("login-btn");
        loginButton.setOnAction(event -> {
            if (isValidLogin(username.getText(), password.getText())) {
                openDashboard(stage, username.getText());
            } else {
                showInvalidLogin(stage);
            }
        });

        VBox card = new VBox(12, title, subtitle, username, password, loginButton);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(24));
        card.setMaxWidth(360);
        card.getStyleClass().add("card");

        StackPane root = new StackPane(card);
        root.setPadding(new Insets(24));

        Scene scene = new Scene(root, 480, 360);
        scene.getStylesheets().add(loadStylesheet());

        stage.setTitle("JavaFX Login");
        stage.setScene(scene);
        stage.show();
    }

    private String loadStylesheet() {
        return Objects.requireNonNull(
                App.class.getResource("styles.css"),
                "styles.css no encontrado")
                .toExternalForm();
    }

    private boolean isValidLogin(String username, String password) {
        // usuario Admin y contrasena zorra.
        return "Admin".equals(username) && "zorra".equals(password);
    }

    private void openDashboard(Stage loginStage, String username) {
        Label welcome = new Label("Carrito de compras");
        welcome.getStyleClass().add("title");

        Label userInfo = new Label("Sesion iniciada como: " + username);
        userInfo.getStyleClass().add("subtitle");

        Label placeholder = new Label("Aqui ira el listado de productos y el resumen del carrito.");
        placeholder.getStyleClass().add("subtitle");

        VBox content = new VBox(12, welcome, userInfo, placeholder);
        content.setAlignment(Pos.TOP_LEFT);
        content.setPadding(new Insets(24));

        Scene scene = new Scene(content, 540, 320);
        scene.getStylesheets().add(loadStylesheet());

        Stage dashboard = new Stage();
        dashboard.setTitle("Carrito de compras");
        dashboard.setScene(scene);
        dashboard.show();

        // Cierra la ventana de login para dejar solo el carrito.
        loginStage.close();
    }

    private void showInvalidLogin(Stage owner) {
        Alert alert = new Alert(AlertType.WARNING);
        alert.initOwner(owner);
        alert.setTitle("Acceso denegado");
        alert.setHeaderText("Credenciales invalidas");
        alert.setContentText("Revisa usuario y contrasena.");
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
