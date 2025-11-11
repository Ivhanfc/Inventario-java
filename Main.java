import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class Main extends JFrame {

    // ======== Modelo de dominio ========
    static class Producto {
        final int id;
        String nombre;
        int cantidad;
        BigDecimal precio; // usar BigDecimal para dinero

        Producto(int id, String nombre, int cantidad, BigDecimal precio) {
            this.id = id;
            this.nombre = nombre;
            this.cantidad = cantidad;
            this.precio = precio.setScale(2, RoundingMode.HALF_UP);
        }
    }

    // ======== TableModel ========
    static class InventarioModel extends AbstractTableModel {
        private final String[] cols = { "ID", "Nombre", "Cantidad", "Precio", "Subtotal" };
        private final Class<?>[] types = { Integer.class, String.class, Integer.class, BigDecimal.class,
                BigDecimal.class };
        private final List<Producto> data = new ArrayList<>();

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
            return data.get(row);
        }

        public void setAll(List<Producto> list) {
            data.clear();
            data.addAll(list);
            fireTableDataChanged();
        }

        public void add(Producto p) {
            data.add(p);
            int idx = data.size() - 1;
            fireTableRowsInserted(idx, idx);
        }

        public void update(int row, Producto p) {
            data.set(row, p);
            fireTableRowsUpdated(row, row);
        }

        public void remove(int row) {
            data.remove(row);
            fireTableRowsDeleted(row, row);
        }

        public List<Producto> all() {
            return data;
        }
    }

    // ======== Estado/UI ========
    private final InventarioModel model = new InventarioModel();
    private final JTable table = new JTable(model);
    private TableRowSorter<TableModel> sorter;
    private final JTextField txtFilter = new JTextField(18);
    private final JLabel lblTotal = new JLabel("Items: 0 | Total: $0.00");
    private final DecimalFormat moneyFmt = new DecimalFormat("#,##0.00");
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);
    private double uiScale = 1.0;
    private Font lafBaseFont;
    private JPanel toolbar;

    // límites de validación
    private static final int CANT_MIN = 0, CANT_MAX = 1_000_000;
    private static final BigDecimal PRECIO_MIN = new BigDecimal("0.00");
    private static final BigDecimal PRECIO_MAX = new BigDecimal("1000000000.00"); // 1e9
    private Dimension filterBaseSize;

    public Main() {
        super("Gestión de Inventario • Neo Swing");

        lafBaseFont = UIManager.getFont("Label.font");
        if (lafBaseFont == null)
            lafBaseFont = new JLabel().getFont();

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        installNimbusDarkish();
        setLayout(new BorderLayout());

        // ======== Menú ========
        setJMenuBar(createMenuBar());

        // ======== Toolbar ========
        var btnCrear = makeButton("Crear");
        var btnEditar = makeButton("Editar");
        var btnBorrar = makeButton("Borrar");
        var btnMostrar = makeButton("Mostrar");
        var btnGuardar = makeButton("Guardar");
        var btnAbrir = makeButton("Abrir");
        var btnZoomIn = makeButton("Zoom +");
        var btnZoomOut = makeButton("Zoom −");
        var btnZoomReset = makeButton("Reset");
        var btnShortcuts = makeButton("Shortcuts");

        toolbar = new JPanel(new GridBagLayout());
        toolbar.setBorder(new EmptyBorder(10, 12, 10, 12));
        toolbar.setOpaque(true);
        toolbar.setBackground(new Color(26, 27, 30));

        var gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 4, 0, 4);
        gbc.gridy = 0;

        int x = 0;
        for (JButton b : new JButton[] { btnCrear, btnEditar, btnBorrar, btnMostrar }) {
            gbc.gridx = x++;
            toolbar.add(b, gbc);
        }
        gbc.gridx = x++;
        toolbar.add(makeSeparator(), gbc);
        for (JButton b : new JButton[] { btnGuardar, btnAbrir }) {
            gbc.gridx = x++;
            toolbar.add(b, gbc);
        }
        gbc.gridx = x++;
        toolbar.add(makeSeparator(), gbc);

        var lblBuscar = styledLabel("Buscar:");
        gbc.gridx = x++;
        toolbar.add(lblBuscar, gbc);
        gbc.gridx = x++;
        toolbar.add(txtFilter, gbc);

        gbc.gridx = x++;
        toolbar.add(makeSeparator(), gbc);
        for (JButton b : new JButton[] { btnZoomOut, btnZoomIn, btnZoomReset }) {
            gbc.gridx = x++;
            toolbar.add(b, gbc);
        }

        for (JButton b : new JButton[] { btnZoomOut, btnZoomIn, btnZoomReset }) {
            gbc.gridx = x++;
            toolbar.add(b, gbc);
        }

        gbc.gridx = x++;
        toolbar.add(makeSeparator(), gbc);
        gbc.gridx = x++;
        toolbar.add(btnShortcuts, gbc);

        add(toolbar, BorderLayout.NORTH);

        // ======== Tabla ========
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(28);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setAutoCreateRowSorter(true);
        table.setDefaultRenderer(Object.class, new FuturisticCellRenderer());
        // header custom
        JTableHeader header = table.getTableHeader();
        header.setDefaultRenderer(new HeaderRenderer(table));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 36));
        sorter = (TableRowSorter<TableModel>) table.getRowSorter();
        add(new JScrollPane(table), BorderLayout.CENTER);

        // ======== Status bar ========
        var status = new JPanel(new BorderLayout());
        status.setBorder(new EmptyBorder(8, 12, 8, 12));
        status.setBackground(new Color(26, 27, 30));
        lblTotal.setForeground(new Color(200, 205, 210));
        status.add(lblTotal, BorderLayout.WEST);
        add(status, BorderLayout.SOUTH);

        // ======== Acciones ========
        btnCrear.addActionListener(e -> onCrear());
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
                JOptionPane.INFORMATION_MESSAGE));

        btnEditar.addActionListener(e -> onEditar());
        btnBorrar.addActionListener(e -> onBorrar());
        btnMostrar.addActionListener(e -> onMostrar());
        btnGuardar.addActionListener(e -> onGuardarCSV());
        btnAbrir.addActionListener(e -> onAbrirCSV());
        btnZoomIn.addActionListener(e -> zoomIn());
        btnZoomOut.addActionListener(e -> zoomOut());
        btnZoomReset.addActionListener(e -> zoomReset());

        txtFilter.getDocument().addDocumentListener((SimpleDocumentListener) e -> applyFilter());

        // ======== Atajos ========
        installShortcuts();

        // ======== Datos de muestra ========
        model.add(new Producto(NEXT_ID.getAndIncrement(), "Teclado MK.II", 10, bd("19.99")));
        model.add(new Producto(NEXT_ID.getAndIncrement(), "Mouse Photon", 25, bd("12.50")));
        model.add(new Producto(NEXT_ID.getAndIncrement(), "Monitor Quantum 27\"", 5, bd("299.00")));
        updateTotals();

        getContentPane().setBackground(new Color(26, 27, 30));

        JScrollPane sp = (JScrollPane) table.getParent().getParent();
        sp.getViewport().setBackground(new Color(28, 30, 35));

        SwingUtilities.invokeLater(() -> {
            if (filterBaseSize == null) {
                // Toma el tamaño preferido actual como base
                filterBaseSize = txtFilter.getPreferredSize();
            }
        });

        setSize(940, 560);
        setLocationRelativeTo(null);
    }

    // ======== Menú ========
    private JMenuBar createMenuBar() {
        var mb = new JMenuBar();
        mb.setBorder(new EmptyBorder(6, 10, 6, 10));

        JMenu mFile = new JMenu("Archivo");
        JMenuItem miNuevo = new JMenuItem("Nuevo (limpiar)");
        JMenuItem miAbrir = new JMenuItem("Abrir CSV…");
        JMenuItem miGuardar = new JMenuItem("Guardar CSV…");
        JMenuItem miSalir = new JMenuItem("Salir");

        miNuevo.addActionListener(e -> {
            if (confirm("Esto limpiará el inventario actual. ¿Continuar?")) {
                model.setAll(new ArrayList<>());
                NEXT_ID.set(1);
                updateTotals();
            }
        });
        miAbrir.addActionListener(e -> onAbrirCSV());
        miGuardar.addActionListener(e -> onGuardarCSV());
        miSalir.addActionListener(e -> dispose());

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
                "Gestión de Inventario • Neo Swing\nUI futurista con Zoom, Filtros y CSV.\n© 2025",
                "Acerca de", JOptionPane.INFORMATION_MESSAGE));
        mHelp.add(miAbout);

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
            model.add(p);
            selectLastRow();
            updateTotals();
        }
    }

    private void onEditar() {
        int row = table.getSelectedRow();
        if (row < 0) {
            warn("Selecciona un producto para editar.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        var base = model.get(modelRow);
        var p = showProductoDialog(base);
        if (p != null) {
            model.update(modelRow, p);
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

    private void onMostrar() {
        int row = table.getSelectedRow();
        if (row < 0) {
            warn("Selecciona un producto para mostrar.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        var p = model.get(modelRow);
        BigDecimal subtotal = p.precio.multiply(new BigDecimal(p.cantidad)).setScale(2, RoundingMode.HALF_UP);
        info("""
                Detalle del producto
                --------------------
                ID: %d
                Nombre: %s
                Cantidad: %d
                Precio: $%s
                Subtotal: $%s
                """.formatted(p.id, p.nombre, p.cantidad, moneyFmt.format(p.precio), moneyFmt.format(subtotal)));
    }

    // ======== Filtro ========
    private void applyFilter() {
        String q = txtFilter.getText().trim();
        if (q.isEmpty()) {
            sorter.setRowFilter(null);
            return;
        }
        // filtra por la columna "Nombre" (índice 1), ignore case
        sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(q), 1));
    }

    // ======== Persistencia CSV ========
    private void onGuardarCSV() {
        var fc = createCSVChooser("Guardar inventario");
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = ensureCsvExtension(fc.getSelectedFile());
            try (var out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8))) {
                out.println("id,nombre,cantidad,precio");
                for (var p : model.all()) {
                    // escapado simple de comas/quotes
                    out.printf("%d,%s,%d,%s%n", p.id, escapeCsv(p.nombre), p.cantidad, p.precio);
                }
                info("Guardado en:\n" + f.getAbsolutePath());
            } catch (IOException ex) {
                error("No se pudo guardar:\n" + ex.getMessage());
            }
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
                NEXT_ID.set(maxId + 1);
                updateTotals();
                info("Cargado desde:\n" + f.getAbsolutePath());
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

    // ======== Diálogo crear/editar ========
    private Producto showProductoDialog(Producto base) {
        var txtNombre = new JTextField(22);
        var spCantidad = new JSpinner(
                new SpinnerNumberModel((base == null ? 0 : base.cantidad), CANT_MIN, CANT_MAX, 1));
        var txtPrecio = new JTextField(10);

        if (base != null) {
            txtNombre.setText(base.nombre);
            txtPrecio.setText(base.precio.toPlainString());
        }

        var form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 8, 6, 8);
        g.gridx = 0;
        g.gridy = 0;
        g.anchor = GridBagConstraints.LINE_END;
        form.add(styledLabel("Nombre:"), g);
        g.gridy++;
        form.add(styledLabel("Cantidad:"), g);
        g.gridy++;
        form.add(styledLabel("Precio:"), g);

        g.gridx = 1;
        g.gridy = 0;
        g.anchor = GridBagConstraints.LINE_START;
        form.add(txtNombre, g);
        g.gridy++;
        form.add(spCantidad, g);
        g.gridy++;
        form.add(txtPrecio, g);

        String titulo = (base == null) ? "Crear producto" : ("Editar producto (ID " + base.id + ")");
        while (true) {
            int opt = JOptionPane.showConfirmDialog(this, form, titulo,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (opt != JOptionPane.OK_OPTION)
                return null;

            String nombre = txtNombre.getText().trim();
            int cantidad = (Integer) spCantidad.getValue();
            String sPrecio = txtPrecio.getText().trim();

            // Validación avanzada
            if (nombre.isBlank()) {
                error("El nombre no puede estar vacío.");
                continue;
            }
            if (nombre.length() > 120) {
                error("Nombre demasiado largo (máx. 120).");
                continue;
            }

            BigDecimal precio;
            try {
                if (!sPrecio.matches("\\d{1,12}([.,]\\d{1,2})?"))
                    throw new NumberFormatException();
                sPrecio = sPrecio.replace(',', '.');
                precio = new BigDecimal(sPrecio);
                if (precio.compareTo(PRECIO_MIN) < 0 || precio.compareTo(PRECIO_MAX) > 0)
                    throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                error("Precio inválido. Ejemplos válidos: 0, 9.99, 12345.50 (máx: " + PRECIO_MAX + ")");
                continue;
            }

            if (base == null) {
                return new Producto(NEXT_ID.getAndIncrement(), nombre, cantidad, precio);
            } else {
                return new Producto(base.id, nombre, cantidad, precio);
            }
        }
    }

    // ======== Totales ========
    private void updateTotals() {
        int items = model.getRowCount();
        BigDecimal total = new BigDecimal("0.00");
        for (var p : model.all()) {
            total = total.add(p.precio.multiply(new BigDecimal(p.cantidad)));
        }
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

        // Escala fuentes del L&F a partir de lafBaseFont (como ya lo dejaste)
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

        // Toolbar explícito (por si lo tienes como campo)
        applyFontRecursively(toolbar, f);

        // Menús
        applyFontToMenuBar(getJMenuBar(), f);

        // Header de la tabla (por si tu renderer lo sobrescribe)
        table.getTableHeader().setFont(f.deriveFont(Font.BOLD));

        // Ajustes de tabla
        table.setRowHeight((int) Math.round(28 * uiScale));
        table.getTableHeader().setPreferredSize(new Dimension(
                table.getTableHeader().getPreferredSize().width,
                (int) Math.round(36 * uiScale)));

        // 🔸 NUEVO: escalar toolbar
        scaleToolbarUI();

        revalidate();
        repaint();
    }

    private void scaleToolbarUI() {
        if (toolbar == null)
            return;

        for (Component c : toolbar.getComponents()) {
            if (c instanceof JButton b) {
                // Reescala padding del botón
                Insets base = (Insets) b.getClientProperty("basePadding");
                if (base == null)
                    base = BUTTON_BASE_PADDING;
                Insets scaled = new Insets(
                        (int) Math.round(base.top * uiScale),
                        (int) Math.round(base.left * uiScale),
                        (int) Math.round(base.bottom * uiScale),
                        (int) Math.round(base.right * uiScale));
                // reconstruye el CompoundBorder con el nuevo inner padding
                b.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(70, 75, 85)),
                        new EmptyBorder(scaled)));
                // limpia preferredSize para que el layout recalcule según fuente+padding
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

        // Reescala el campo de búsqueda
        if (filterBaseSize != null) {
            txtFilter.setPreferredSize(new Dimension(
                    (int) Math.round(filterBaseSize.width * uiScale),
                    (int) Math.round(filterBaseSize.height * uiScale)));
        } else {
            // fallback: limpia para que recalcule
            txtFilter.setPreferredSize(null);
        }

        toolbar.revalidate();
        toolbar.repaint();
    }

    private void applyScaleTo(Component c) {
        Font f = c.getFont();
        if (f != null)
            c.setFont(f.deriveFont((float) (getBaseFontSize(f) * uiScale)));
        if (c instanceof Container cont) {
            for (Component child : cont.getComponents())
                applyScaleTo(child);
        }
    }

    private float getBaseFontSize(Font f) {
        return Math.max(11f, f.getSize2D());
    }

    private void packForScale() {
        // mantén tamaño si ya es grande; de lo contrario ajusta
        if (getWidth() < 900 || getHeight() < 520)
            pack();
        revalidate();
    }

    // ======== Estilo / Look & Feel ========
    private static void installNimbusDarkish() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
        }
        // Ajustes "dark-ish"
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

    private static class HeaderRenderer implements TableCellRenderer {
        private final TableCellRenderer delegate;

        HeaderRenderer(JTable table) {
            this.delegate = table.getTableHeader().getDefaultRenderer();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            Component c = delegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setBackground(new Color(24, 26, 32));
            c.setForeground(new Color(196, 200, 208));
            c.setFont(c.getFont().deriveFont(Font.BOLD));
            if (c instanceof JComponent jc)
                jc.setBorder(new EmptyBorder(6, 8, 6, 8));
            return c;
        }
    }

    private static class FuturisticCellRenderer extends DefaultTableCellRenderer {
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
                c.setBackground((row % 2 == 0) ? new Color(30, 32, 38) : new Color(34, 36, 42));
                setForeground(new Color(225, 230, 235));
            }
            if (c instanceof JComponent jc)
                jc.setBorder(new EmptyBorder(6, 8, 6, 8));
            return c;
        }
    }

    // ======== Util ========
    private JLabel styledLabel(String text) {
        var l = new JLabel(text);
        l.setForeground(new Color(210, 215, 220));
        return l;
    }

    private static final Dimension SEP_BASE_SIZE = new Dimension(12, 28);

    private JSeparator makeSeparator() {
        var sep = new JSeparator(SwingConstants.VERTICAL);
        sep.putClientProperty("baseSize", SEP_BASE_SIZE);
        sep.setPreferredSize(SEP_BASE_SIZE);
        sep.setBackground(new Color(60, 64, 72));
        return sep;
    }

    private static final Insets BUTTON_BASE_PADDING = new Insets(6, 10, 6, 10);

    private JButton makeButton(String text) {
        var b = new JButton(text);
        b.setFocusPainted(false);
        // guardar el padding base como clientProperty para reescalarlo luego..
        b.putClientProperty("basePadding", BUTTON_BASE_PADDING);

        var inner = new EmptyBorder(BUTTON_BASE_PADDING);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 75, 85)),
                inner));
        b.setBackground(new Color(44, 46, 52));
        b.setForeground(new Color(230, 235, 240));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                b.setBackground(new Color(56, 58, 66));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                b.setBackground(new Color(44, 46, 52));
            }
        });
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
        int last = table.getRowCount() - 1;
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
        // ---- ZOOM IN (varias combinaciones) ----
        // Ctrl + Shift + '=' (la manera más común para '+')
        bind(im, am, "zoomIn_eq_shift", KeyStroke.getKeyStroke(
                KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
                e -> zoomIn());

        // Ctrl + '=' (muchas apps aceptan esto como zoom in)
        bind(im, am, "zoomIn_eq_plain", KeyStroke.getKeyStroke(
                KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK),
                e -> zoomIn());

        // Ctrl + ADD (teclado numérico '+')
        bind(im, am, "zoomIn_numpad", KeyStroke.getKeyStroke(
                KeyEvent.VK_ADD, InputEvent.CTRL_DOWN_MASK),
                e -> zoomIn());

        // (Opcional) si tu JDK/teclado reporta VK_PLUS, bindea también:
        try {
            bind(im, am, "zoomIn_plus", KeyStroke.getKeyStroke(
                    KeyEvent.class.getField("VK_PLUS").getInt(null), InputEvent.CTRL_DOWN_MASK),
                    e -> zoomIn());
        } catch (Exception ignore) {
            /* VK_PLUS no existe en todos los JDKs */ }

        // Ctrl + '-'
        bind(im, am, "zoomOut", KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK), e -> zoomOut());
        // Ctrl + '0'
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

    // Fuente escalada según uiScale
    private Font scaledFont() {
        Font base = (lafBaseFont != null) ? lafBaseFont : UIManager.getFont("Label.font");
        if (base == null)
            base = new JLabel().getFont();
        return base.deriveFont((float) (base.getSize2D() * uiScale));
    }

    // Aplica la fuente a todo un árbol de componentes (toolbar, contentPane, etc.)
    private void applyFontRecursively(Component c, Font f) {
        if (c == null)
            return;
        c.setFont(f);

        // Ajustes especiales
        if (c instanceof JTable t) {
            JTableHeader h = t.getTableHeader();
            if (h != null)
                h.setFont(f.deriveFont(Font.BOLD));
        }

        if (c instanceof Container cont) {
            for (Component child : cont.getComponents()) {
                applyFontRecursively(child, f);
            }
        }
    }

    // Aplica la fuente a menús/menubar (los menús no son hijos “normales” del
    // contenedor)
    private void applyFontToMenuBar(JMenuBar mb, Font f) {
        if (mb == null)
            return;
        mb.setFont(f);
        for (int i = 0; i < mb.getMenuCount(); i++) {
            JMenu m = mb.getMenu(i);
            if (m == null)
                continue;
            m.setFont(f);
            for (int j = 0; j < m.getItemCount(); j++) {
                JMenuItem it = m.getItem(j);
                if (it != null)
                    it.setFont(f);
            }
        }
    }

    // ======== Arranque ========
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            installNimbusDarkish();
            Main ui = new Main();
            ui.setVisible(true);
        });
    }

    // ======== DocumentListener simple ========
    private interface SimpleDocumentListener extends javax.swing.event.DocumentListener {
        void update(javax.swing.event.DocumentEvent e);

        @Override
        default void insertUpdate(javax.swing.event.DocumentEvent e) {
            update(e);
        }

        @Override
        default void removeUpdate(javax.swing.event.DocumentEvent e) {
            update(e);
        }

        @Override
        default void changedUpdate(javax.swing.event.DocumentEvent e) {
            update(e);
        }
    }
}
