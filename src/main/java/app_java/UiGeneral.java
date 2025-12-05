package app_java;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class UiGeneral extends JFrame {
    public CardLayout cardLayout;
    private JPanel mainPanel;

    // constantes para nombre de las vistas
    public static final String VIEW_INVENTARIO = "Inventario";
    public static final String VIEW_GENERAL = "General";
    public static final String VIEW_REPORTES = "Reportes";

    // ======== Modelo de dominio ========

    static class Producto { // producto es un objeto
        final int id;// id para identificacion una del producto
        String nombre;
        int cantidad;
        BigDecimal precio; // usar BigDecimal para dinero

        Producto(int id, String nombre, int cantidad, BigDecimal precio) { // se crea un contructor para el objeto
                                                                           // producto asi podemos instanciarlo
            this.id = id;
            this.nombre = nombre;
            this.cantidad = cantidad;
            this.precio = precio.setScale(2, RoundingMode.HALF_UP);
        }
    }

    // ======== TableModel ========
    static class InventarioModel extends AbstractTableModel { // le hereda toda la funcionalidad basica
        private final String[] cols = { "ID", "Nombre", "Cantidad", "Precio", "Subtotal" }; // se crean las columnas
        private final Class<?>[] types = { Integer.class, String.class, Integer.class, BigDecimal.class, // su tipo de
                                                                                                         // dato
                BigDecimal.class };
        private final List<Producto> data = shoppingCart(); // que los datos de Producto sean los obtenidos de la base
                                                            // de datos de sqlite

        private List<Producto> shoppingCart() { // todos los componentes de la lista deben de ser Producto
            List<Producto> lista = new ArrayList<>();

            return lista; // la funcion retorna la lista que va a mostar
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return cols.length;
        }

        @Override
        public String getColumnName(int c) {
            return cols[c];
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return types[c];
        }

        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }

        @Override
        public Object getValueAt(int row, int col) {
            var p = data.get(row);
            return switch (col) {
                case 0 -> p.id;
                case 1 -> p.nombre;
                case 2 -> p.cantidad;
                case 3 -> p.precio;
                case 4 -> p.precio.multiply(new BigDecimal(p.cantidad)).setScale(2, RoundingMode.HALF_UP);
                default -> "";
            };
        }

        public Producto get(int row) {
            return data.get(row); // retorna los datos obtenidos del (get) de la fila
        }

        public void setAll(List<Producto> list) { // metodo de importacion
            data.clear(); // limpia los datos actuales
            fireTableDataChanged(); // le avisa a la tabla anterior que sus datos han cambiado
        }

        public void add(Producto p) { // funcion para agregar un producto
            data.add(p); // los datos son pasados como parametros al objeto de Producto que su instancia
                         // es p
            int idx = data.size() - 1; // le dice en donde se agregara la nueva fila, 1 antes de la que a esta
            fireTableRowsInserted(idx, idx); // le avisa a la tabla vieja que fue insertado una nueva fila
        }

        public void update(int row, Producto p) { // esta funcion actuliza los datos ocupa los parametros de fila y
                                                  // producto
            data.set(row, p); // cambia los datos enviando de parametros la fila(row) y el producto
            fireTableRowsUpdated(row, row); // le avisa a la tabla vieja que fila fue actualizada
        }

        public void remove(int row) { // la funcion de borrar obtiene de parametro la fila
            var p = data.get(row); // se obtiene losd atos de la fila
            Database.DeleteProduct(p.id); // se borra el elemento de la base de datos basado en su id

            data.remove(row); // aqui se borra la fila del lado del frontend.
            fireTableRowsDeleted(row, row);
        }

        public List<Producto> all() { // aqui se retorna todda la lista
            return data;
        }
    }

    // ======== Estado/UI ========
    private final InventarioModel model = new InventarioModel(); // Crea el modelo de datos del inventario.

    private final JTable table = new JTable(model); // Crea una tabla grafica usando el model del inventario
    private TableRowSorter<TableModel> sorter; // filtrado de filas, ordenar el contenido de la tabla.
    private final JTextField txtFilter = new JTextField(18); // Input donde el usuario escribe para filtrar elementos.
    private final JLabel lblTotal = new JLabel("Items: 0 | Total: $0.00"); // Etiqueta que muestra el total de articulos
                                                                           // , moneasdas del inv.
    private final DecimalFormat moneyFmt = new DecimalFormat("#,##0.00"); // Formateador numerico, muestra precios con
                                                                          // formato de monedas(dos decimales, separador
                                                                          // de miles)
    private double uiScale = 1.0; // Almacena un factor de escalado de la interfaz (parra ampliar o reducir
                                  // elementos graficos).
    private Font lafBaseFont; // Guarda la fuente del Look and Feel, aplica cambios al tema o size de la UI.
    private JPanel toolbar; // Estos dos son paneles de la parte superior (Toolbar) y de la (barra de
                            // estado).
    private JPanel status; //
    private Dimension filterBaseSize; // Guarda el size original del campo de filtro, para recalcularlo al escalar la
                                      // UI.

    // límites de validación
    private static final int CANT_MIN = 0, CANT_MAX = 1_000_000; // Define los limites validos para la cantidad de un
                                                                 // articulo en inventario (0 a 1M)
    private static final BigDecimal PRECIO_MIN = new BigDecimal("0.00"); // Definen los limites validos para el precio:
                                                                         // 0.00 y 1,000,000,000
    private static final BigDecimal PRECIO_MAX = new BigDecimal("1000000000.00"); // 1e9

    // ======== THEME: enum + paletas ========
    private enum Theme {
        DARK, LIGHT
    }

    // Clase interna inmutable que agrupa todos los colores de un tema
    private static final class Palette {
        final Color bg, fg, toolbarBg, statusBg, labelFg,
                btnBg, btnFg, btnHoverBg, border,
                tableRowEven, tableRowOdd, tableHeaderBg, tableHeaderFg, fieldBg, fieldFg;

        // Constructor: recibe todos los colores del tema y los asigna
        // (Color es la clase estandar de Java (java.awt.Color)) RGB..
        Palette(Color bg, Color fg, Color toolbarBg, Color statusBg, Color labelFg,
                Color btnBg, Color btnFg, Color btnHoverBg, Color border,
                Color tableRowEven, Color tableRowOdd, Color tableHeaderBg, Color tableHeaderFg,
                Color fieldBg, Color fieldFg) {
            this.bg = bg; // Color de fondo general
            this.fg = fg; // Color de texto general
            this.toolbarBg = toolbarBg; // Fondo de la toolbar
            this.statusBg = statusBg; // Fondo de la barra de estado
            this.labelFg = labelFg; // Color de texto de etiquetas
            this.btnBg = btnBg; // Fondo de botones
            this.btnFg = btnFg; // Texto de botones
            this.btnHoverBg = btnHoverBg; // Fondo de botón al pasar el mouse
            this.border = border; // Color de bordes
            this.tableRowEven = tableRowEven; // Fondo filas pares de la tabla
            this.tableRowOdd = tableRowOdd; // Fondo filas impares de la tabla
            this.tableHeaderBg = tableHeaderBg; // Fondo del encabezado de la tabla
            this.tableHeaderFg = tableHeaderFg; // Texto del encabezado de la tabla
            this.fieldBg = fieldBg; // Fondo de campos de texto
            this.fieldFg = fieldFg; // Texto de campos de texto
        }
    }

    private Theme currentTheme = Theme.LIGHT; // Tema actual (inicia en modo claro)

    private final Palette PALETTE_DARK = new Palette(
            new Color(26, 27, 30), new Color(230, 235, 240), // bg, fg
            new Color(40, 42, 48), new Color(26, 27, 30), // toolbar bg, status bg
            new Color(210, 215, 220), // label fg
            new Color(44, 46, 52), new Color(230, 235, 240), // btn bg/fg
            new Color(56, 58, 66), new Color(70, 75, 85), // btn hover, border
            new Color(30, 32, 38), new Color(34, 36, 42), // filas pares/impares tabla
            new Color(40, 42, 48), new Color(196, 200, 208), // header tabla bg/fg
            new Color(55, 55, 60), new Color(230, 235, 240) // campos texto bg/fg
    );

    // Paleta de colores para el tema claro
    private final Palette PALETTE_LIGHT = new Palette(
            Color.WHITE, Color.BLACK, // bg, fg
            new Color(245, 245, 247), new Color(245, 245, 247), // toolbar bg, status bg
            new Color(30, 30, 30), // label fg
            new Color(230, 230, 230), Color.BLACK, // btn bg/fg
            new Color(210, 210, 210), new Color(200, 200, 200), // btn hover, border
            new Color(250, 250, 250), new Color(242, 242, 242), // filas tabla
            new Color(240, 240, 240), new Color(50, 50, 50), // header tabla bg/fg
            Color.WHITE, Color.BLACK // campos texto bg/fg
    );

    // Tamaño base de separadores de toolbar (para recalcular con zoom)
    private static final Dimension SEP_BASE_SIZE = new Dimension(12, 28);
    // Padding base de los botones (para recalcular con zoom)
    private static final Insets BUTTON_BASE_PADDING = new Insets(6, 10, 6, 10);

    public UiGeneral() {
        super("Carrito de Compra"); // Título de la ventana principal

        lafBaseFont = UIManager.getFont("Label.font"); // Fuente base del Look and Feel
        if (lafBaseFont == null)
            lafBaseFont = new JLabel().getFont(); // Fallback si no hay fuente en UIManager

        setDefaultCloseOperation(EXIT_ON_CLOSE); // Cierra la app al cerrar la ventana
        setLayout(new BorderLayout()); // Layout principal de la ventana

        // ======== Menú ========
        setJMenuBar(createMenuBar()); // Crea y asigna la barra de menú

        // ======== Toolbar ========

        var btnBorrar = makeButton("Borrar"); // Botón para borrar ítem
        var btnMostrar = makeButton("Mostrar"); // Botón para mostrar detalles
        var btnGuardar = makeButton("Guardar"); // Botón para guardar CSV
        var btnAbrir = makeButton("Abrir"); // Botón para abrir CSV
        var btnZoomIn = makeButton("Zoom +"); // Aumentar zoom UI
        var btnZoomOut = makeButton("Zoom −"); // Reducir zoom UI
        var btnZoomReset = makeButton("Reset"); // Resetear zoom
        var btnShortcuts = makeButton("Shortcuts"); // Mostrar atajos de teclado
        var btnTheme = makeButton("🌙 Oscuro"); // Botón para cambiar tema (ofrece modo oscuro)

        toolbar = new JPanel(new GridBagLayout()); // Toolbar con GridBagLayout
        toolbar.setBorder(new EmptyBorder(10, 12, 10, 12)); // Margen interno de la toolbar
        toolbar.setOpaque(true); // La toolbar pinta su fondo

        var gbc = new GridBagConstraints(); // Restricciones para GridBag: Reglas que describen como se debe colocar un
                                            // componente dentro de un GridBagLayout)
        gbc.insets = new Insets(0, 4, 0, 4); // Espaciado entre componentes
        gbc.gridy = 0; // Fila fija (solo una fila)

        int x = 0; // Columna actual
        for (JButton b : new JButton[] { btnBorrar, btnMostrar }) {
            gbc.gridx = x++; // Columna siguiente
            toolbar.add(b, gbc); // Añadir botón a la toolbar
        }
        gbc.gridx = x++;
        toolbar.add(makeSeparator(), gbc); // Separador visual en toolbar

        for (JButton b : new JButton[] { btnGuardar, btnAbrir }) {
            gbc.gridx = x++;
            toolbar.add(b, gbc); // Añadir botones Guardar/Abrir
        }
        gbc.gridx = x++;
        toolbar.add(makeSeparator(), gbc); // Otro separador

        var lblBuscar = styledLabel("Buscar:"); // Etiqueta "Buscar:"
        gbc.gridx = x++;
        toolbar.add(lblBuscar, gbc); // Añadir etiqueta buscar
        gbc.gridx = x++;
        toolbar.add(txtFilter, gbc); // Campo de texto filtro

        gbc.gridx = x++;
        toolbar.add(makeSeparator(), gbc); // Separador antes de zoom
        for (JButton b : new JButton[] { btnZoomOut, btnZoomIn, btnZoomReset }) {
            gbc.gridx = x++;
            toolbar.add(b, gbc); // Añadir botones de zoom
        }
        gbc.gridx = x++;
        toolbar.add(makeSeparator(), gbc); // Separador antes de shortcuts
        gbc.gridx = x++;
        toolbar.add(btnShortcuts, gbc); // Botón de atajos

        // THEME: botón de cambio de tema al final de la toolbar
        gbc.gridx = x++;
        toolbar.add(makeSeparator(), gbc); // Separador antes de tema
        gbc.gridx = x++;
        toolbar.add(btnTheme, gbc); // Botón tema

        add(toolbar, BorderLayout.NORTH); // Colocar toolbar arriba

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        mainPanel.add(new UiInventario(), VIEW_INVENTARIO);
        mainPanel.add(new UiGeneral(), VIEW_GENERAL);

        // ======== Acciones ========

        btnBorrar.addActionListener(e -> onBorrar()); // Acción borrar
        btnMostrar.addActionListener(e -> onMostrar()); // Acción mostrar detalles
        btnGuardar.addActionListener(e -> onGuardarCSV()); // Acción guardar CSV
        btnAbrir.addActionListener(e -> onAbrirCSV()); // Acción abrir CSV
        btnZoomIn.addActionListener(e -> zoomIn()); // Acción zoom +
        btnZoomOut.addActionListener(e -> zoomOut()); // Acción zoom −
        btnZoomReset.addActionListener(e -> zoomReset()); // Acción reset zoom
        btnShortcuts.addActionListener(e -> JOptionPane.showMessageDialog(
                this,
                """
                        Atajos y acciones
                        -----------------
                        Crear: botón "Crear" (o Ctrl+N).

                        Editar: selecciona una fila → "Editar" (o Ctrl+E).

                        Borrar: selecciona una fila → "Borrar" (o tecla Delete).

                        Mostrar: selecciona una fila → "Mostrar".

                        Guardar: "Guardar" (o Ctrl+S) → elige .csv.

                        Abrir: "Abrir" (o Ctrl+O) → selecciona un .csv previamente guardado.

                        Buscar: escribe en "Buscar".

                        Zoom: Ctrl++, Ctrl+-, Ctrl+0 (o botones "Zoom").
                        """,
                "Shortcuts",
                JOptionPane.INFORMATION_MESSAGE)); // Muestra ventana con ayuda de atajos

        // THEME: alternar entre claro y oscuro
        btnTheme.addActionListener(e -> {
            if (currentTheme == Theme.LIGHT) { // Si está en claro…
                applyTheme(Theme.DARK); // aplica oscuro
                btnTheme.setText("☀️ Claro"); // texto del botón pasa a "Claro"
            } else {
                applyTheme(Theme.LIGHT); // aplica claro
                btnTheme.setText("🌙 Oscuro"); // texto del botón pasa a "Oscuro"
            }
        });

        // Filtro: cada cambio en el texto aplica el filtro en la tabla
        txtFilter.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void update(DocumentEvent e) {
                applyFilter(); // Refiltra filas según texto
            }
        });

        // ======== Atajos ========
        installShortcuts(); // Configura atajos de teclado globales

        updateTotals(); // Calcula y muestra totales iniciales

        // THEME: aplicar el tema inicial (claro)
        applyTheme(Theme.LIGHT);

        // Guardar tamaño base del campo de búsqueda (se usa para zoom)
        SwingUtilities.invokeLater(() -> {
            if (filterBaseSize == null)
                filterBaseSize = txtFilter.getPreferredSize(); // Tamaño inicial del campo
        });

        setSize(940, 560); // Tamaño inicial de la ventana
        setLocationRelativeTo(null); // Centra la ventana en pantalla
    }

    // ======== Menú ========
    private JMenuBar createMenuBar() { // Crea la barra de menú
        var mb = new JMenuBar(); // instancia de JMenuBar
        mb.setBorder(new EmptyBorder(6, 10, 6, 10));
        JMenu mViews = new JMenu("Vistas"); // menu de vistas
        JMenuItem miGeneral = new JMenuItem("Carrito de Compra");
        JMenuItem miReportes = new JMenuItem("Reportes");
        JMenuItem miInventario = new JMenuItem("Inventario");
        mViews.add(miGeneral);
        mViews.add(miReportes);
        mViews.add(miInventario);
        miInventario.addActionListener(e -> {
            cardLayout.show(mainPanel, VIEW_INVENTARIO);
        });
        miGeneral.addActionListener(e -> {
            cardLayout.show(mainPanel, VIEW_GENERAL);
        });
        JMenu mFile = new JMenu("Archivo"); // menu de archivo
        JMenuItem miNuevo = new JMenuItem("Nuevo (limpiar)");
        JMenuItem miAbrir = new JMenuItem("Abrir CSV…");
        JMenuItem miGuardar = new JMenuItem("Guardar CSV…");
        JMenuItem miSalir = new JMenuItem("Salir");

        miNuevo.addActionListener(e -> {
            if (confirm("Esto limpiará el inventario actual. ¿Continuar?")) {
                // 1) Limpiar el modelo (tabla en memoria)
                model.setAll(new ArrayList<>());
                updateTotals();

                // 2) Limpiar también la base de datos SQLite
                Database.clearProducts();
            }
        });

        miAbrir.addActionListener(e -> onAbrirCSV()); // llama al metodo de abrir CSV
        miGuardar.addActionListener(e -> onGuardarCSV()); // llama al metodo de guardar CSV
        miSalir.addActionListener(e -> dispose()); // cierra la aplicacion

        mFile.add(miNuevo);
        mFile.addSeparator();
        mFile.add(miAbrir);
        mFile.add(miGuardar);
        mFile.addSeparator();
        mFile.add(miSalir);

        JMenu mEdit = new JMenu("Editar");
        JMenuItem miCrear = new JMenuItem("Crear");
        JMenuItem miEditar = new JMenuItem("Editar");
        JMenuItem miBorrar = new JMenuItem("Borrar");
        miCrear.addActionListener(e -> onCrear());
        miEditar.addActionListener(e -> onEditar());
        miBorrar.addActionListener(e -> onBorrar());
        mEdit.add(miCrear);
        mEdit.add(miEditar);
        mEdit.add(miBorrar);

        JMenu mView = new JMenu("Vista");
        JMenuItem miZoomIn = new JMenuItem("Zoom +");
        JMenuItem miZoomOut = new JMenuItem("Zoom −");
        JMenuItem miZoomReset = new JMenuItem("Reset zoom");
        miZoomIn.addActionListener(e -> zoomIn());
        miZoomOut.addActionListener(e -> zoomOut());
        miZoomReset.addActionListener(e -> zoomReset());
        mView.add(miZoomIn);
        mView.add(miZoomOut);
        mView.add(miZoomReset);

        JMenu mHelp = new JMenu("Ayuda");

        JMenuItem miAbout = new JMenuItem("Acerca de…");
        miAbout.addActionListener(e -> JOptionPane.showMessageDialog(
                this,
                "Gestión de Inventario\nUI con Zoom, Filtros y CSV.\n© 2025, DEVELOPERS\nAdán Pech\nIván Beltrán\nJesús Borbón",
                "Acerca de", JOptionPane.INFORMATION_MESSAGE));
        mHelp.add(miAbout);
        mb.add(mViews);

        mb.add(mFile);
        mb.add(mEdit);
        mb.add(mView);
        mb.add(mHelp);
        return mb;
    }

    // ======== CRUD ========
    private void onCrear() {
        var p = showProductoDialog(null);
        if (p != null) {
            // 1) Insertar en BD
            if (!Database.productExist(p.nombre)) {
                int idGenerado = Database.insertProduct(
                        p.nombre,
                        p.cantidad,
                        p.precio.doubleValue());

                if (idGenerado != -1) {
                    // 2) Producto con ID real
                    var conIdReal = new Producto(idGenerado, p.nombre, p.cantidad, p.precio);

                    // 3) Solo agregar al modelo
                    model.add(conIdReal);
                    selectLastRow();
                    updateTotals();
                } else {
                    error("No se pudo insertar el producto en la base de datos.");
                }
            } else {
                error("El produto ya existe, favor de editarlo o eliminarlo");
            }
        }

    }

    private void onEditar() {
        int row = table.getSelectedRow();
        if (row < 0) {
            warn("Selecciona un producto para editar.");
            return;
        }

        int modelRow = table.convertRowIndexToModel(row);
        var base = model.get(modelRow); // Producto actual (con ID)

        var p = showProductoDialog(base);
        if (p != null) {
            // 1) Actualizar en base de datos usando el ID existente
            Database.updateProduct(
                    base.id,
                    p.nombre,
                    p.cantidad,
                    p.precio.doubleValue());

            // 2) Mantener el mismo ID en el modelo
            var actualizado = new Producto(base.id, p.nombre, p.cantidad, p.precio);
            model.update(modelRow, actualizado);

            updateTotals();
        }
    }

    private void onBorrar() {
        int row = table.getSelectedRow();
        if (row < 0) {
            warn("Selecciona un producto para borrar.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        var p = model.get(modelRow);
        if (confirm("¿Borrar el producto ID " + p.id + " (" + p.nombre + ")?")) {
            model.remove(modelRow);
            updateTotals();
        }
    }

    // Metodo que se ejecuta cuando el usuario quiere mostrar el detalle de un
    // producto
    private void onMostrar() {
        // Obtiene la fila seleccionada en la tabla
        int row = table.getSelectedRow();

        // Si no hay ninguna fila seleccionada, muestra una advertencia y termina
        if (row < 0) {
            warn("Selecciona un producto para mostrar.");
            return;
        }

        // Convierte el indice de la fila visual al indice real del modelo
        int modelRow = table.convertRowIndexToModel(row);

        // Obtiene el producto correspondiente a esa fila en el modelo
        var p = model.get(modelRow);

        // Calcula el subtotal multiplicando precio por cantidad
        // Se ajusta a 2 decimales con redondeo HALF_UP
        BigDecimal subtotal = p.precio
                .multiply(new BigDecimal(p.cantidad))
                .setScale(2, RoundingMode.HALF_UP);

        // Muestra un mensaje informativo con los datos del producto
        info("""
                Detalle del producto
                --------------------
                ID: %d
                Nombre: %s
                Cantidad: %d
                Precio: $%s
                Subtotal: $%s
                """.formatted(
                p.id,
                p.nombre,
                p.cantidad,
                moneyFmt.format(p.precio),
                moneyFmt.format(subtotal)));
    }

    // ======== Filtro ========
    private void applyFilter() {
        String q = txtFilter.getText().trim();
        if (q.isEmpty()) {
            sorter.setRowFilter(null);
            return;
        }
        sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(q), 1)); // columna Nombre
    }

    // ======== Persistencia CSV ========
    private void onGuardarCSV() {
        var fc = createCSVChooser("Guardar inventario");
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = ensureCsvExtension(fc.getSelectedFile());
            try (var out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8))) {
                out.println("id,nombre,cantidad,precio");
                for (var p : model.all()) {
                    out.printf("%d,%s,%d,%s%n",
                            p.id,
                            escapeCsv(p.nombre),
                            p.cantidad,
                            p.precio);
                }
                info("Guardado en:\n" + f.getAbsolutePath());

                // Luego de guardar el CSV, sincronizamos SQLite con el modelo/CSV. ATT tu compi
                // el BOR
                syncModelToDatabase();

            } catch (IOException ex) {
                error("No se pudo guardar:\n" + ex.getMessage());
            }
        }
    }

    // Sincronizar todo el modelo con la base de datos
    private void syncModelToDatabase() {
        try {
            // 1) Vaciar la tabla en la base de datos
            Database.clearProducts();

            // 2) Insertar de nuevo todo lo que hay en el modelo
            for (var p : model.all()) {
                Database.insertProductWithId(
                        p.id,
                        p.nombre,
                        p.cantidad,
                        p.precio.doubleValue());
            }

            System.out.println("Sincronización modelo → SQLite completada.");
        } catch (Exception e) {
            e.printStackTrace();
            error("Error al sincronizar con la base de datos:\n" + e.getMessage());
        }
    }

    private void onAbrirCSV() {
        var fc = createCSVChooser("Abrir inventario");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            try (var br = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
                String line;
                List<Producto> list = new ArrayList<>();
                int maxId = 0;
                boolean headerSkipped = false;
                while ((line = br.readLine()) != null) {
                    if (!headerSkipped) {
                        headerSkipped = true;
                        continue;
                    }
                    String[] parts = parseCsvLine(line, 4);
                    int id = Integer.parseInt(parts[0]);
                    String nombre = parts[1];
                    int cantidad = Integer.parseInt(parts[2]);
                    BigDecimal precio = new BigDecimal(parts[3]).setScale(2, RoundingMode.HALF_UP);
                    list.add(new Producto(id, nombre, cantidad, precio));
                    maxId = Math.max(maxId, id);
                }

                model.setAll(list);
                updateTotals();
                info("Cargado desde:\n" + f.getAbsolutePath());

                // Despues de cargar desde CSV, hacemos que SQLite quede igual que el archivo
                syncModelToDatabase();

            } catch (Exception ex) {
                error("No se pudo abrir:\n" + ex.getMessage());
            }
        }
    }

    private static JFileChooser createCSVChooser(String title) {
        var fc = new JFileChooser();
        fc.setDialogTitle(title);
        fc.setFileFilter(new FileNameExtensionFilter("CSV (*.csv)", "csv"));
        return fc;
    }

    private static File ensureCsvExtension(File f) {
        return f.getName().toLowerCase().endsWith(".csv") ? f : new File(f.getParentFile(), f.getName() + ".csv");
    }

    private static String escapeCsv(String s) {
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            s = s.replace("\"", "\"\"");
            return "\"" + s + "\"";
        }
        return s;
    }

    private static String[] parseCsvLine(String line, int expected) {
        List<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQ = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQ) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        sb.append('"');
                        i++;
                    } else
                        inQ = false;
                } else
                    sb.append(c);
            } else {
                if (c == ',') {
                    out.add(sb.toString());
                    sb.setLength(0);
                } else if (c == '"')
                    inQ = true;
                else
                    sb.append(c);
            }
        }
        out.add(sb.toString());
        if (out.size() != expected)
            throw new IllegalArgumentException("CSV inválido");
        return out.toArray(String[]::new);
    }

    // ======== Dialog crear/editar ========
    private Producto showProductoDialog(Producto base) {
        // Campo de texto para nombre (22 columnas)
        var txtNombre = new JTextField(22);

        // Spinner numerico para cantidad con modelo limitado por CANT_MIN y CANT_MAX
        var spCantidad = new JSpinner(
                new SpinnerNumberModel((base == null ? 0 : base.cantidad), CANT_MIN, CANT_MAX, 1));

        // Campo de texto para precio
        var txtPrecio = new JTextField(10);

        // Si se esta editando un producto, cargar sus datos en los campos
        if (base != null) {
            txtNombre.setText(base.nombre);
            txtPrecio.setText(base.precio.toPlainString());
        }

        // Panel del formulario con GridBagLayout
        var form = new JPanel(new GridBagLayout());
        form.setOpaque(false); // Fondo transparente

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 8, 6, 8); // Espaciado entre componentes

        // Primera columna: etiquetas alineadas a la derecha
        g.gridx = 0;
        g.gridy = 0;
        g.anchor = GridBagConstraints.LINE_END;
        form.add(styledLabel("Nombre:"), g);
        g.gridy++;
        form.add(styledLabel("Cantidad:"), g);
        g.gridy++;
        form.add(styledLabel("Precio:"), g);

        // Segunda columna: campos alineados a la izquierda
        g.gridx = 1;
        g.gridy = 0;
        g.anchor = GridBagConstraints.LINE_START;
        form.add(txtNombre, g);
        g.gridy++;
        form.add(spCantidad, g);
        g.gridy++;
        form.add(txtPrecio, g);

        // Determinar titulo segun si es crear o editar
        String titulo = (base == null) ? "Crear producto" : ("Editar producto (ID " + base.id + ")");

        // Bucle para mostrar el dialogo hasta que los datos sean validos
        while (true) {
            int opt = JOptionPane.showConfirmDialog(this, form, titulo,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            // Si se cancela o cierra el dialogo, no se devuelve producto
            if (opt != JOptionPane.OK_OPTION)
                return null;

            // Obtener valores ingresados
            String nombre = txtNombre.getText().trim();
            int cantidad = (Integer) spCantidad.getValue();
            String sPrecio = txtPrecio.getText().trim();

            // Validacion del nombre
            if (nombre.isBlank()) {
                error("El nombre no puede estar vacio.");
                continue;
            }
            if (nombre.length() > 120) {
                error("Nombre demasiado largo (max. 120).");
                continue;
            }

            // Validacion del precio con BigDecimal
            BigDecimal precio;
            try {
                // Comprobar formato numerico permitido
                if (!sPrecio.matches("\\d{1,12}([.,]\\d{1,2})?"))
                    throw new NumberFormatException();

                // Convertir coma a punto si se uso coma decimal
                sPrecio = sPrecio.replace(',', '.');

                // Convertir a BigDecimal
                precio = new BigDecimal(sPrecio);

                // Validar rango permitido
                if (precio.compareTo(PRECIO_MIN) < 0 || precio.compareTo(PRECIO_MAX) > 0)
                    throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                error("Precio invalido. Ejemplos validos: 0, 9.99, 12345.50 (max: " + PRECIO_MAX + ")");
                continue;
            }

            // Crear producto nuevo o actualizar existente
            if (base == null)
                return new Producto(0, nombre, cantidad, precio);
            else
                return new Producto(base.id, nombre, cantidad, precio);
        }
    }

    // ======== Totales ========
    private void updateTotals() {
        int items = model.getRowCount();
        BigDecimal total = new BigDecimal("0.00");
        for (var p : model.all())
            total = total.add(p.precio.multiply(new BigDecimal(p.cantidad)));
        lblTotal.setText("Items: " + items + "  |  Total: $" + moneyFmt.format(total));
    }

    // ======== Zoom ========
    private void zoomIn() {
        setScale(uiScale * 1.10);
    }

    private void zoomOut() {
        setScale(uiScale / 1.10);
    }

    private void zoomReset() {
        setScale(1.0);
    }

    private void setScale(double newScale) {
        setScale(newScale, false);
    }

    private void setScale(double newScale, boolean force) {
        newScale = Math.max(0.7, Math.min(2.0, newScale));
        if (!force && Math.abs(newScale - uiScale) < 0.01)
            return;

        uiScale = newScale;

        // Escala fuentes del L&F a partir de lafBaseFont
        Font base = (lafBaseFont != null) ? lafBaseFont : UIManager.getFont("Label.font");
        if (base == null)
            base = new JLabel().getFont();
        Font scaled = base.deriveFont((float) (base.getSize2D() * uiScale));
        for (Object key : UIManager.getLookAndFeelDefaults().keySet()) {
            if (key.toString().endsWith(".font"))
                UIManager.put(key, scaled);
        }
        SwingUtilities.updateComponentTreeUI(this);

        Font f = scaledFont();

        // Aplica fuente a toda la ventana (contenido, toolbar, status, etc.)
        applyFontRecursively(getContentPane(), f);
        applyFontRecursively(toolbar, f);
        applyFontToMenuBar(getJMenuBar(), f);

        // Header de la tabla
        table.getTableHeader().setFont(f.deriveFont(Font.BOLD));

        // Ajustes de tabla
        table.setRowHeight((int) Math.round(28 * uiScale));
        table.getTableHeader().setPreferredSize(new Dimension(
                table.getTableHeader().getPreferredSize().width,
                (int) Math.round(36 * uiScale)));

        // Escalar toolbar (padding/breaks)
        scaleToolbarUI();

        revalidate();
        repaint();
    }

    private void scaleToolbarUI() {
        if (toolbar == null)
            return;

        for (Component c : toolbar.getComponents()) {
            if (c instanceof JButton b) {
                Insets base = (Insets) b.getClientProperty("basePadding");
                if (base == null)
                    base = BUTTON_BASE_PADDING;
                Insets scaled = new Insets(
                        (int) Math.round(base.top * uiScale),
                        (int) Math.round(base.left * uiScale),
                        (int) Math.round(base.bottom * uiScale),
                        (int) Math.round(base.right * uiScale));
                // reconstruye el CompoundBorder con inner padding escalado
                // color de borde segun tema actual
                Color border = (currentTheme == Theme.DARK) ? PALETTE_DARK.border : PALETTE_LIGHT.border;
                b.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(border),
                        new EmptyBorder(scaled)));
                b.setPreferredSize(null);
            } else if (c instanceof JSeparator sep) {
                Dimension base = (Dimension) sep.getClientProperty("baseSize");
                if (base == null)
                    base = SEP_BASE_SIZE;
                sep.setPreferredSize(new Dimension(
                        (int) Math.round(base.width * uiScale),
                        (int) Math.round(base.height * uiScale)));
            }
        }

        // Reescala el campo de busqueda
        if (filterBaseSize != null) {
            txtFilter.setPreferredSize(new Dimension(
                    (int) Math.round(filterBaseSize.width * uiScale),
                    (int) Math.round(filterBaseSize.height * uiScale)));
        } else {
            txtFilter.setPreferredSize(null);
        }

        toolbar.revalidate();
        toolbar.repaint();
    }

    private void applyFontRecursively(Component c, Font f) {
        if (c == null)
            return;
        if (f != null)
            c.setFont(f);
        if (c instanceof Container cont) {
            for (Component child : cont.getComponents())
                applyFontRecursively(child, f);
        }
    }

    private void applyFontToMenuBar(JMenuBar mb, Font f) {
        if (mb == null)
            return;
        for (MenuElement me : mb.getSubElements()) {
            Component c = me.getComponent();
            if (f != null)
                c.setFont(f);
            if (c instanceof JMenu jmenu) {
                for (Component mc : jmenu.getMenuComponents())
                    mc.setFont(f);
            }
        }
    }

    private Font scaledFont() {
        Font base = (lafBaseFont != null) ? lafBaseFont : new JLabel().getFont();
        return base.deriveFont((float) (base.getSize2D() * uiScale));
    }

    // ======== THEME: Look&Feel base + overrides ========
    private static void installNimbusBase() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static void installNimbusDarkish() {
        installNimbusBase();
        UIManager.put("control", new Color(36, 38, 45));
        UIManager.put("info", new Color(36, 38, 45));
        UIManager.put("nimbusBase", new Color(18, 20, 25));
        UIManager.put("nimbusBlueGrey", new Color(50, 54, 60));
        UIManager.put("nimbusLightBackground", new Color(28, 30, 35));
        UIManager.put("text", new Color(230, 235, 240));
        UIManager.put("menuText", new Color(230, 235, 240));
        UIManager.put("controlText", new Color(230, 235, 240));
        UIManager.put("Table.alternateRowColor", new Color(32, 34, 40));
        UIManager.put("Table.showGrid", Boolean.FALSE);
    }

    private static void installNimbusLightish() {
        installNimbusBase();
        UIManager.put("control", new Color(250, 250, 250));
        UIManager.put("info", new Color(250, 250, 250));
        UIManager.put("nimbusBase", new Color(180, 180, 190));
        UIManager.put("nimbusBlueGrey", new Color(210, 210, 215));
        UIManager.put("nimbusLightBackground", Color.WHITE);
        UIManager.put("text", Color.BLACK);
        UIManager.put("menuText", Color.BLACK);
        UIManager.put("controlText", Color.BLACK);
        UIManager.put("Table.alternateRowColor", new Color(245, 245, 245));
        UIManager.put("Table.showGrid", Boolean.FALSE);
    }

    // ======== RENDERERS: dual para celdas y header ========
    class DarkCellRenderer extends DefaultTableCellRenderer {
        @Override
        protected void setValue(Object value) {
            if (value instanceof BigDecimal bd)
                super.setValue("$" + new DecimalFormat("#,##0.00").format(bd));
            else
                super.setValue(value);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                c.setBackground((row % 2 == 0) ? PALETTE_DARK.tableRowEven : PALETTE_DARK.tableRowOdd);
                setForeground(new Color(225, 230, 235));
            }
            if (c instanceof JComponent jc)
                jc.setBorder(new EmptyBorder(6, 8, 6, 8));
            return c;
        }
    }

    class LightCellRenderer extends DefaultTableCellRenderer {
        @Override
        protected void setValue(Object value) {
            if (value instanceof BigDecimal bd)
                super.setValue("$" + new DecimalFormat("#,##0.00").format(bd));
            else
                super.setValue(value);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                c.setBackground((row % 2 == 0) ? PALETTE_LIGHT.tableRowEven : PALETTE_LIGHT.tableRowOdd);
                setForeground(PALETTE_LIGHT.fg);
            }
            if (c instanceof JComponent jc)
                jc.setBorder(new EmptyBorder(6, 8, 6, 8));
            return c;
        }
    }

    class DarkHeaderRenderer implements TableCellRenderer {
        private final TableCellRenderer delegate;

        DarkHeaderRenderer(JTable table) {
            this.delegate = table.getTableHeader().getDefaultRenderer();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            Component c = delegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setBackground(PALETTE_DARK.tableHeaderBg);
            c.setForeground(PALETTE_DARK.tableHeaderFg);
            c.setFont(c.getFont().deriveFont(Font.BOLD));
            if (c instanceof JComponent jc)
                jc.setBorder(new EmptyBorder(6, 8, 6, 8));
            return c;
        }
    }

    class LightHeaderRenderer implements TableCellRenderer {
        private final TableCellRenderer delegate;

        LightHeaderRenderer(JTable table) {
            this.delegate = table.getTableHeader().getDefaultRenderer();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            Component c = delegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setBackground(PALETTE_LIGHT.tableHeaderBg);
            c.setForeground(PALETTE_LIGHT.tableHeaderFg);
            c.setFont(c.getFont().deriveFont(Font.BOLD));
            if (c instanceof JComponent jc)
                jc.setBorder(new EmptyBorder(6, 8, 6, 8));
            return c;
        }
    }

    // ======== THEME: aplicar a toda la UI ========
    private void applyTheme(Theme theme) {
        this.currentTheme = theme;

        // 1) Look&Feel base
        if (theme == Theme.DARK)
            installNimbusDarkish();
        else
            installNimbusLightish();

        // 2) Refrescar UI para que Nimbus aplique sus defaults
        SwingUtilities.updateComponentTreeUI(this);

        // 3) Ahora aplicamos NUESTRA paleta por encima del LAF
        var pal = (theme == Theme.DARK) ? PALETTE_DARK : PALETTE_LIGHT;

        // Contenedor principal
        getContentPane().setBackground(pal.bg);

        // Toolbar
        if (toolbar != null)
            toolbar.setBackground(pal.toolbarBg);

        // Status
        lblTotal.setForeground(pal.fg);
        if (status != null)
            status.setBackground(pal.statusBg);

        // MENU BAR (nav superior) – aquí es donde arreglamos el problema
        JMenuBar menuBar = getJMenuBar();
        if (menuBar != null) {
            menuBar.setOpaque(true);

            // FULL blanco en modo claro, y toolbarBg en oscuro
            Color navBg = (theme == Theme.LIGHT) ? Color.WHITE : pal.toolbarBg;

            menuBar.setBackground(navBg);
            menuBar.setForeground(pal.fg);

            for (MenuElement me : menuBar.getSubElements()) {
                if (me instanceof JMenu m) {
                    m.setOpaque(true);
                    m.setBackground(navBg);
                    m.setForeground(pal.fg);
                }
            }
        }

        // Tabla: renderers y header según tema
        TableCellRenderer cellR = (theme == Theme.DARK)
                ? new DarkCellRenderer()
                : new LightCellRenderer();

        table.setDefaultRenderer(Object.class, cellR);
        table.setDefaultRenderer(String.class, cellR);
        table.setDefaultRenderer(Number.class, cellR);
        table.setDefaultRenderer(Integer.class, cellR);
        table.setDefaultRenderer(BigDecimal.class, cellR);

        table.setBackground(pal.bg);
        table.setForeground(pal.fg);
        table.setGridColor(pal.border);
        table.setSelectionBackground(theme == Theme.DARK ? new Color(70, 75, 85) : new Color(200, 220, 255));
        table.setSelectionForeground(theme == Theme.DARK ? pal.fg : Color.BLACK);

        // HEADER: renderer que usa SIEMPRE la paleta actual
        JTableHeader header = table.getTableHeader();

        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus,
                    int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(CENTER);
                setOpaque(true);
                setBackground(pal.tableHeaderBg);
                setForeground(pal.tableHeaderFg);
                setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, pal.border));
                return c;
            }
        });

        header.setOpaque(true);
        header.setBackground(pal.tableHeaderBg);
        header.setForeground(pal.tableHeaderFg);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 36));
        header.repaint();

        // Viewport del JScrollPane
        Container p = table.getParent();
        if (p != null && p.getParent() instanceof JScrollPane sp) {
            sp.getViewport().setBackground(pal.bg);
            sp.setBackground(pal.bg);
        }

        // Aplicar paleta a todo el árbol de componentes (excepto menu bar)
        applyPaletteToTree(getContentPane(), pal);

        // Reajustar toolbar (padding/bordes) según escala y tema
        scaleToolbarUI();

        revalidate();
        repaint();
    }

    private void applyPaletteToTree(Component root, Palette pal) {
        Deque<Component> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            Component c = stack.pop();

            // basicos
            if (c instanceof JScrollPane || c instanceof JPanel || c instanceof JLabel)
                c.setBackground(pal.bg);
            c.setForeground(pal.fg);

            // fields
            if (c instanceof JTextComponent tc) {
                tc.setBackground(pal.fieldBg);
                tc.setForeground(pal.fieldFg);
                tc.setCaretColor(pal.fieldFg);
                if (tc instanceof JComponent jc)
                    jc.setBorder(BorderFactory.createLineBorder(pal.border));
            }

            // botones / toggles
            if (c instanceof JButton b) {
                b.setBackground(pal.btnBg);
                b.setForeground(pal.btnFg);
                if (b instanceof JComponent jc)
                    jc.setBorder(BorderFactory.createLineBorder(pal.border));
            } else if (c instanceof AbstractButton ab) {
                ab.setBackground(pal.bg);
                ab.setForeground(pal.fg);
            }

            // combo
            if (c instanceof JComboBox<?> combo) {
                combo.setBackground(pal.btnBg);
                combo.setForeground(pal.btnFg);
                combo.setRenderer(new DefaultListCellRenderer() {
                    @Override
                    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                            boolean isSelected, boolean cellHasFocus) {
                        Component r = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                        r.setBackground(isSelected ? pal.border : pal.btnBg);
                        r.setForeground(pal.btnFg);
                        list.setBackground(pal.btnBg);
                        list.setForeground(pal.btnFg);
                        return r;
                    }
                });
            }

            if (c instanceof Container cont) {
                for (Component child : cont.getComponents())
                    stack.push(child);
            }
        }
    }

    // ======== Util ========
    private JLabel styledLabel(String text) {
        var l = new JLabel(text);
        l.setForeground(currentTheme == Theme.DARK ? PALETTE_DARK.labelFg : PALETTE_LIGHT.labelFg);
        return l;
    }

    private JSeparator makeSeparator() {
        var sep = new JSeparator(SwingConstants.VERTICAL);
        sep.putClientProperty("baseSize", SEP_BASE_SIZE);
        sep.setPreferredSize(SEP_BASE_SIZE);

        Color border = (currentTheme == Theme.DARK) ? PALETTE_DARK.border : PALETTE_LIGHT.border;
        sep.setForeground(border);
        sep.setBackground(border);
        return sep;
    }

    private JButton makeButton(String text) {
        var b = new JButton(text);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.putClientProperty("basePadding", BUTTON_BASE_PADDING);

        Color baseBg = (currentTheme == Theme.DARK) ? PALETTE_DARK.btnBg : PALETTE_LIGHT.btnBg;
        Color baseFg = (currentTheme == Theme.DARK) ? PALETTE_DARK.btnFg : PALETTE_LIGHT.btnFg;
        Color border = (currentTheme == Theme.DARK) ? PALETTE_DARK.border : PALETTE_LIGHT.border;

        var inner = new EmptyBorder(BUTTON_BASE_PADDING);
        b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(border), inner));
        b.setBackground(baseBg);
        b.setForeground(baseFg);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        return b;
    }

    private boolean confirm(String msg) {
        return JOptionPane.showConfirmDialog(this, msg, "Confirmar", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION;
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Aviso", JOptionPane.WARNING_MESSAGE);
    }

    private void info(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Información", JOptionPane.INFORMATION_MESSAGE);
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void selectLastRow() {
        int last = model.getRowCount() - 1;
        if (last >= 0) {
            table.getSelectionModel().setSelectionInterval(last, last);
            table.scrollRectToVisible(table.getCellRect(last, 0, true));
        }
    }

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }

    private void installShortcuts() {
        JRootPane rp = getRootPane();
        InputMap im = rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = rp.getActionMap();

        bind(im, am, "new", KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), e -> onCrear());
        bind(im, am, "edit", KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK), e -> onEditar());
        bind(im, am, "save", KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), e -> onGuardarCSV());
        bind(im, am, "open", KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK), e -> onAbrirCSV());
        bind(im, am, "focusFilter", KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK),
                e -> txtFilter.requestFocusInWindow());
        bind(im, am, "delete", KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), e -> onBorrar());

        // Zoom combos
        bind(im, am, "zoomIn_eq_shift",
                KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
                e -> zoomIn());
        bind(im, am, "zoomIn_eq_plain", KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK),
                e -> zoomIn());
        bind(im, am, "zoomIn_numpad", KeyStroke.getKeyStroke(KeyEvent.VK_ADD, InputEvent.CTRL_DOWN_MASK),
                e -> zoomIn());
        try { // VK_PLUS opcional
            bind(im, am, "zoomIn_plus",
                    KeyStroke.getKeyStroke(KeyEvent.class.getField("VK_PLUS").getInt(null), InputEvent.CTRL_DOWN_MASK),
                    e -> zoomIn());
        } catch (Exception ignore) {
        }
        bind(im, am, "zoomOut", KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK), e -> zoomOut());
        bind(im, am, "zoomReset", KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK), e -> zoomReset());
    }

    private void bind(InputMap im, ActionMap am, String name, KeyStroke ks, ActionListener al) {
        im.put(ks, name);
        am.put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                al.actionPerformed(e);
            }
        });
    }

    // ======== SimpleDocumentListener (para el filtro) ========
    interface SimpleDocumentListener extends javax.swing.event.DocumentListener {
        void update(DocumentEvent e);

        @Override
        default void insertUpdate(DocumentEvent e) {
            update(e);
        }

        @Override
        default void removeUpdate(DocumentEvent e) {
            update(e);
        }

        @Override
        default void changedUpdate(DocumentEvent e) {
            update(e);
        }
    }

}
