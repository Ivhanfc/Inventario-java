package com.ivhanfc.scannerjs.app_java;

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
/**
 * Gestión de Inventario • Neo Swing (con Theme Toggle Light/Dark)
 * - Arranca en LIGHT por defecto
 * - Botón de tema en Toolbar
 * - Renderers duales para tabla/header
 * - Paletas centralizadas
 */
public class UI extends JFrame {

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
    private JPanel status;
    private Dimension filterBaseSize;

    // límites de validación
    private static final int CANT_MIN = 0, CANT_MAX = 1_000_000;
    private static final BigDecimal PRECIO_MIN = new BigDecimal("0.00");
    private static final BigDecimal PRECIO_MAX = new BigDecimal("1000000000.00"); // 1e9

    // ======== THEME: enum + paletas ========
    private enum Theme {
        DARK, LIGHT
    }

    private static final class Palette {
        final Color bg, fg, toolbarBg, statusBg, labelFg,
                btnBg, btnFg, btnHoverBg, border,
                tableRowEven, tableRowOdd, tableHeaderBg, tableHeaderFg, fieldBg, fieldFg;

        Palette(Color bg, Color fg, Color toolbarBg, Color statusBg, Color labelFg,
                Color btnBg, Color btnFg, Color btnHoverBg, Color border,
                Color tableRowEven, Color tableRowOdd, Color tableHeaderBg, Color tableHeaderFg,
                Color fieldBg, Color fieldFg) {
            this.bg = bg;
            this.fg = fg;
            this.toolbarBg = toolbarBg;
            this.statusBg = statusBg;
            this.labelFg = labelFg;
            this.btnBg = btnBg;
            this.btnFg = btnFg;
            this.btnHoverBg = btnHoverBg;
            this.border = border;
            this.tableRowEven = tableRowEven;
            this.tableRowOdd = tableRowOdd;
            this.tableHeaderBg = tableHeaderBg;
            this.tableHeaderFg = tableHeaderFg;
            this.fieldBg = fieldBg;
            this.fieldFg = fieldFg;
        }
    }

    private Theme currentTheme = Theme.LIGHT; // arranca en CLARO
    private final Palette PALETTE_DARK = new Palette(
            new Color(26, 27, 30), new Color(230, 235, 240), // bg, fg
            new Color(26, 27, 30), new Color(26, 27, 30), // toolbar bg, status bg
            new Color(210, 215, 220), // label fg
            new Color(44, 46, 52), new Color(230, 235, 240), // btn bg/fg
            new Color(56, 58, 66), new Color(70, 75, 85), // btn hover, border
            new Color(30, 32, 38), new Color(34, 36, 42), // table even/odd
            new Color(24, 26, 32), new Color(196, 200, 208), // table header bg/fg
            new Color(55, 55, 60), new Color(230, 235, 240) // field bg/fg
    );
    private final Palette PALETTE_LIGHT = new Palette(
            Color.WHITE, Color.BLACK,
            new Color(245, 245, 247), new Color(245, 245, 247),
            new Color(30, 30, 30),
            new Color(230, 230, 230), Color.BLACK,
            new Color(210, 210, 210), new Color(200, 200, 200),
            new Color(250, 250, 250), new Color(242, 242, 242),
            new Color(240, 240, 240), new Color(50, 50, 50),
            Color.WHITE, Color.BLACK);

    // ======== tamaños base Toolbar/Buttons para zoom ========
    private static final Dimension SEP_BASE_SIZE = new Dimension(12, 28);
    private static final Insets BUTTON_BASE_PADDING = new Insets(6, 10, 6, 10);

    public UI() {
        super("Gestión de Inventario • Neo Swing");

        lafBaseFont = UIManager.getFont("Label.font");
        if (lafBaseFont == null)
            lafBaseFont = new JLabel().getFont();

        setDefaultCloseOperation(EXIT_ON_CLOSE);
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
        var btnTheme = makeButton("🌙 Oscuro"); // THEME: botón toggle (inicia en claro, ofrece pasar a oscuro)

        toolbar = new JPanel(new GridBagLayout());
        toolbar.setBorder(new EmptyBorder(10, 12, 10, 12));
        toolbar.setOpaque(true);

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
        gbc.gridx = x++;
        toolbar.add(makeSeparator(), gbc);
        gbc.gridx = x++;
        toolbar.add(btnShortcuts, gbc);

        // THEME: añadir al final
        gbc.gridx = x++;
        toolbar.add(makeSeparator(), gbc);
        gbc.gridx = x++;
        toolbar.add(btnTheme, gbc);

        add(toolbar, BorderLayout.NORTH);

        // ======== Tabla ========
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(28);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setAutoCreateRowSorter(true);
        sorter = (TableRowSorter<TableModel>) table.getRowSorter();
        add(new JScrollPane(table), BorderLayout.CENTER);

        // ======== Status bar ========
        status = new JPanel(new BorderLayout());
        status.setBorder(new EmptyBorder(8, 12, 8, 12));
        status.add(lblTotal, BorderLayout.WEST);
        add(status, BorderLayout.SOUTH);

        // ======== Acciones ========
        btnCrear.addActionListener(e -> onCrear());
        btnEditar.addActionListener(e -> onEditar());
        btnBorrar.addActionListener(e -> onBorrar());
        btnMostrar.addActionListener(e -> onMostrar());
        btnGuardar.addActionListener(e -> onGuardarCSV());
        btnAbrir.addActionListener(e -> onAbrirCSV());
        btnZoomIn.addActionListener(e -> zoomIn());
        btnZoomOut.addActionListener(e -> zoomOut());
        btnZoomReset.addActionListener(e -> zoomReset());
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

        // THEME: toggle
        btnTheme.addActionListener(e -> {
            if (currentTheme == Theme.LIGHT) {
                applyTheme(Theme.DARK);
                btnTheme.setText("☀️ Claro");
            } else {
                applyTheme(Theme.LIGHT);
                btnTheme.setText("🌙 Oscuro");
            }
        });

        // Filtro
        txtFilter.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void update(DocumentEvent e) {
                applyFilter();
            }
        });

        // ======== Atajos ========
        installShortcuts();

        // ======== Datos de muestra ========
        model.add(new Producto(NEXT_ID.getAndIncrement(), "Teclado MK.II", 10, bd("19.99")));
        model.add(new Producto(NEXT_ID.getAndIncrement(), "Mouse Photon", 25, bd("12.50")));
        model.add(new Producto(NEXT_ID.getAndIncrement(), "Monitor Quantum 27\"", 5, bd("299.00")));
        updateTotals();

        // THEME: aplicar tema inicial (CLARO)
        applyTheme(Theme.LIGHT);

        // Guardar tamaño base del campo de búsqueda (para zoom)
        SwingUtilities.invokeLater(() -> {
            if (filterBaseSize == null)
                filterBaseSize = txtFilter.getPreferredSize();
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
                "Gestión de Inventario\nUI con Zoom, Filtros y CSV.\n© 2025, DEVELOPERS\nAdán Pech\nIván Beltrán\nJesús Borbón",
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

            if (base == null)
                return new Producto(NEXT_ID.getAndIncrement(), nombre, cantidad, precio);
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
                // color de borde según tema actual
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

        // Reescala el campo de búsqueda
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

        // 1) Look&Feel base + overrides
        if (theme == Theme.DARK)
            installNimbusDarkish();
        else
            installNimbusLightish();

        // 2) Colores de contenedores principales
        var pal = (theme == Theme.DARK) ? PALETTE_DARK : PALETTE_LIGHT;
        getContentPane().setBackground(pal.bg);
        if (toolbar != null)
            toolbar.setBackground(pal.toolbarBg);
        lblTotal.setForeground(pal.fg);
        if (status != null)
            status.setBackground(pal.statusBg);

        // 3) Tabla: renderers y header según tema
        TableCellRenderer cellR = (theme == Theme.DARK)
                ? new DarkCellRenderer()
                : new LightCellRenderer();

        table.setDefaultRenderer(Object.class, cellR);
        table.setDefaultRenderer(String.class, cellR);
        table.setDefaultRenderer(Number.class, cellR);
        table.setDefaultRenderer(Integer.class, cellR);
        table.setDefaultRenderer(BigDecimal.class, cellR);

        // Colores base/selección de la tabla
        table.setBackground(pal.bg);
        table.setForeground(pal.fg);
        table.setGridColor(pal.border);
        table.setSelectionBackground(theme == Theme.DARK ? new Color(70, 75, 85) : new Color(200, 220, 255));
        table.setSelectionForeground(theme == Theme.DARK ? pal.fg : Color.BLACK);

        // Header
        JTableHeader header = table.getTableHeader();
        header.setDefaultRenderer(theme == Theme.DARK ? new DarkHeaderRenderer(table) : new LightHeaderRenderer(table));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 36));

        // Viewport del JScrollPane (para que el “fondo entre filas” no quede claro)
        Container p = table.getParent();
        if (p != null && p.getParent() instanceof JScrollPane sp) {
            sp.getViewport().setBackground(pal.bg);
            sp.setBackground(pal.bg);
        }

        // 4) Repintar todos los componentes (fondo/primer plano/fields/botones/combos)
        applyPaletteToTree(getContentPane(), pal);

        scaleToolbarUI();

        // 5) Refrescar UI
        SwingUtilities.updateComponentTreeUI(this);
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