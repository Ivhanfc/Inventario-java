package app_java;

// import java.lang.Thread.State;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    public static void CrearDB() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:inventario.db")) {
            Statement stmt = conn.createStatement();

            String sql = "CREATE TABLE IF NOT EXISTS PRODUCTOS (" +
                    "ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "NOMBRE VARCHAR (200)," +
                    "CANTIDAD INTEGER," +
                    "PRECIO REAL" +
                    ");";

            stmt.execute(sql);
            System.out.println("Base de datos creada correctamente");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ResultSet showProducts() throws SQLException {
        String url = "jdbc:sqlite:inventario.db"; // ruta a tu base
        Connection conn = DriverManager.getConnection(url);
        Statement stmt = conn.createStatement();
        return stmt.executeQuery("SELECT * FROM PRODUCTOS");

    }

    public static int insertProduct(String nombre, int cantidad, double precio) {
        System.out.println("insertProduct llamado con: " + nombre);

        // 🔍 Imprimir trazas para ver quién lo llama
        new Exception("TRACE insertProduct").printStackTrace(System.out);

        String sql = "INSERT INTO productos(nombre, cantidad, precio) VALUES (?,?,?)";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:inventario.db");
                PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, nombre);
            pstmt.setInt(2, cantidad);
            pstmt.setDouble(3, precio);

            pstmt.executeUpdate();
            System.out.println("Datos insertados correctamente");

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1); // ID generado por SQLite
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al insertar el producto: " + e.getMessage());
        }

        return -1;
    }

    public static void DeleteProduct(int id) {
        String url = "jdbc:sqlite:inventario.db";

        try (Connection conn = DriverManager.getConnection(url)) {
            String sql = "DELETE FROM PRODUCTOS WHERE ID = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, id);
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                System.out.println("Producto con ID: " + id + " eliminado correctamente");
            } else {
                System.out.println("No se encontró producto con ID " + id);
            }

        } catch (SQLException e) {
            System.out.println("Error al eliminar el producto: " + e.getMessage());
        }
    }

    // borrar todos los productos (para resync con CSV/modelo)
    public static void clearProducts() {
        String url = "jdbc:sqlite:inventario.db";

        try (Connection conn = DriverManager.getConnection(url);
                PreparedStatement pstmt = conn.prepareStatement("DELETE FROM PRODUCTOS")) {

            int rows = pstmt.executeUpdate();
            System.out.println("Se eliminaron " + rows + " productos antes de sincronizar.");
        } catch (SQLException e) {
            System.out.println("Error al limpiar la tabla PRODUCTOS: " + e.getMessage());
        }
    }

    // Actualizar un producto existente por ID (para onEditar)
    public static void updateProduct(int id, String nombre, int cantidad, double precio) {
        String sql = "UPDATE PRODUCTOS SET NOMBRE = ?, CANTIDAD = ?, PRECIO = ? WHERE ID = ?";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:inventario.db");
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nombre);
            pstmt.setInt(2, cantidad);
            pstmt.setDouble(3, precio);
            pstmt.setInt(4, id);

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                System.out.println("Producto con ID " + id + " actualizado correctamente");
            } else {
                System.out.println("No se encontró producto con ID " + id + " para actualizar");
            }

        } catch (SQLException e) {
            System.out.println("Error al actualizar el producto: " + e.getMessage());
        }
    }

    // Insertar respetando el ID (para cuando leemos desde CSV)
    public static void insertProductWithId(int id, String nombre, int cantidad, double precio) {
        String sql = "INSERT INTO PRODUCTOS(ID, NOMBRE, CANTIDAD, PRECIO) VALUES (?,?,?,?)";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:inventario.db");
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            pstmt.setString(2, nombre);
            pstmt.setInt(3, cantidad);
            pstmt.setDouble(4, precio);

            pstmt.executeUpdate();
            System.out.println("Producto con ID " + id + " insertado desde CSV.");
        } catch (SQLException e) {
            System.out.println("Error al insertar producto con ID fijo: " + e.getMessage());
        }
    }
   public static boolean productExist(String nombre) {
        String sql = "SELECT 1 FROM PRODUCTOS WHERE NOMBRE = ? LIMIT 1";
    
    String URL = "jdbc:sqlite:inventario.db";
        
 try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, nombre);
  try(ResultSet res =  pstmt.executeQuery()) {
   return res.next();
  }

 } catch(SQLException e) {
    System.out.println("El producto ya es existente, favor de editarlo."+ e.getMessage());
    return false;
}
}
}

