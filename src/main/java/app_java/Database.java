package app_java;
 //aqui importamos librerias necesarias para la conexion a la base de datos y manejo de consultas sql
// import java.lang.Thread.State;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    public static void CrearDB() { //aqui se crea la base de datos con una consulta de sqlite 
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:inventario.db")) { //esperamos que el archivo encuentre la ruta de la conexion a la base de datos
            Statement stmt = conn.createStatement(); //creamos una sentencia para ejecutar consultas SQL

            String sql = "CREATE TABLE IF NOT EXISTS PRODUCTOS (" +
                    "ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "NOMBRE VARCHAR (200)," +
                    "CANTIDAD INTEGER," +
                    "PRECIO REAL" +
                    ");";
//creamos los elementos que necesitaremos id, nombre, cantidad y precio
            stmt.execute(sql); //ejecutamos la sentencia de sql con stmt para evitar inyecciones sql
            System.out.println("Base de datos creada correctamente"); //mensaje de que todo salio bien
        } catch (Exception e) { //en caso de que haya un error lo atrapamos con catch
            e.printStackTrace();
        }
    }

    public static ResultSet showProducts() throws SQLException { //metodo para para mandar los datos de la base de datos al frontend
        String url = "jdbc:sqlite:inventario.db"; // ruta a tu base
        Connection conn = DriverManager.getConnection(url); //conexion a la base de datos
        Statement stmt = conn.createStatement(); //creamos la sentencia para ejecutar consultas sql
        return stmt.executeQuery("SELECT * FROM PRODUCTOS"); //retornaremos los valores que nos dio stmt despues
        //de la consulta sql
    
}

    public static int insertProduct(String nombre, int cantidad, double precio) { //metodo para insertar productos a la base de datos
        //usamos de parametros nombre, cantidad y precio
        System.out.println("insertProduct llamado con: " + nombre);

        // Imprimir trazas para ver quien lo llama
        new Exception("TRACE insertProduct").printStackTrace(System.out); // debuggeando

        String sql = "INSERT INTO productos(nombre, cantidad, precio) VALUES (?,?,?)"; //consulta sql para insertar los valores protegido con stmt

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:inventario.db"); //esperando la conexiuon con la base de datos
                PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) { //preparamos la sentencia sql

            pstmt.setString(1, nombre);
            pstmt.setInt(2, cantidad); //usamos pstsmt para seguridad contra inyecciones sql
            pstmt.setDouble(3, precio);

            pstmt.executeUpdate(); //ejecutamos la sentencia
            System.out.println("Datos insertados correctamente");

            try (ResultSet rs = pstmt.getGeneratedKeys()) { //obtenemos el id generado automaticamente por sqlite
                if (rs.next()) { // si existe uno con ese id, lo retornamos
                    return rs.getInt(1); // ID generado por SQLite
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al insertar el producto: " + e.getMessage()); //mensaje de error por si falla algo
        }

        return -1;
    }

    public static void DeleteProduct(int id) { //metodo para borrar un elemento por medio de su id
        String url = "jdbc:sqlite:inventario.db"; //ruta para la base de datos

        try (Connection conn = DriverManager.getConnection(url)) { //esperando conexion con la base de datos
            String sql = "DELETE FROM PRODUCTOS WHERE ID = ?"; //consulta sql para borra el producto basado en su id
            PreparedStatement pstmt = conn.prepareStatement(sql); //se prepara la sentencia sql
            pstmt.setInt(1, id); //seteamos el id en la consulta
            int rows = pstmt.executeUpdate(); //ejecutamos la consulta y vemos cuantas filas son afectadas
            if (rows > 0) { //si es mayor a 0, se borro la fila de manera correcta
                System.out.println("Producto con ID: " + id + " eliminado correctamente"); //mensaje de exito
            } else {
                System.out.println("No se encontró producto con ID " + id); //mensaje de error si no encuentra una fila con ese id
            }

        } catch (SQLException e) {
            System.out.println("Error al eliminar el producto: " + e.getMessage());// mensaje de error si falla en algo
        }
    }

    // borrar todos los productos (para resync con CSV/modelo)
    public static void clearProducts() { //metodo para limpiar la tabla productos
        String url = "jdbc:sqlite:inventario.db"; //ruta para la base de datos

        try (Connection conn = DriverManager.getConnection(url); //esperando conexion con la base de datos
                PreparedStatement pstmt = conn.prepareStatement("DELETE FROM PRODUCTOS")) { //sentencia para borrar todos los productos

            int rows = pstmt.executeUpdate(); //ejecutamos la consulta y vemos cuantas filas son afectadas
            System.out.println("Se eliminaron " + rows + " productos antes de sincronizar."); //mensaje de exito con el numero de filas borradas
        } catch (SQLException e) {
            System.out.println("Error al limpiar la tabla PRODUCTOS: " + e.getMessage()); //mensaje de error si falla en algo
        }
    }

    // Actualizar un producto existente por ID (para onEditar)
    public static void updateProduct(int id, String nombre, int cantidad, double precio) { //metodo para actualizar un producto por medio de su id
        String sql = "UPDATE PRODUCTOS SET NOMBRE = ?, CANTIDAD = ?, PRECIO = ? WHERE ID = ?"; //semtencia sql para actualizar protegida con pstmt

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:inventario.db"); //esperando conexion con la base de datos
                PreparedStatement pstmt = conn.prepareStatement(sql)) { //preparamos la sentencia sql

            pstmt.setString(1, nombre);
            pstmt.setInt(2, cantidad);
            pstmt.setDouble(3, precio);
            pstmt.setInt(4, id);

            int rows = pstmt.executeUpdate(); //ejecutamos la consulta y vemos cuantas filas fueron afectadas
            if (rows > 0) {
                System.out.println("Producto con ID " + id + " actualizado correctamente"); //si es mayor a 0 se actualizo correctamente
            } else {
                System.out.println("No se encontró producto con ID " + id + " para actualizar"); //en caso de que no encuentr ninguna fila con ese id
            }

        } catch (SQLException e) {
            System.out.println("Error al actualizar el producto: " + e.getMessage());//error
        }
    }

    // Insertar respetando el ID (para cuando leemos desde CSV)
    public static void insertProductWithId(int id, String nombre, int cantidad, double precio) { 
        String sql = "INSERT INTO PRODUCTOS(ID, NOMBRE, CANTIDAD, PRECIO) VALUES (?,?,?,?)"; //consulta sql para insertar los valores protegido con stmt

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:inventario.db"); //esperando la conexiuon con la base de datos
                PreparedStatement pstmt = conn.prepareStatement(sql)) { //preparamos la sentencia sql

            pstmt.setInt(1, id);
            pstmt.setString(2, nombre);
            pstmt.setInt(3, cantidad);
            pstmt.setDouble(4, precio);

            pstmt.executeUpdate(); //ejecutamos la sentencia
            System.out.println("Producto con ID " + id + " insertado desde CSV."); //mensaje de exito
        } catch (SQLException e) {
            System.out.println("Error al insertar producto con ID fijo: " + e.getMessage()); //mensaje de error por si falla algo
        }
    }
   public static boolean productExist(String nombre) { //metodo para verificar si un producto ya existe por su nombre
        String sql = "SELECT 1 FROM PRODUCTOS WHERE NOMBRE = ? LIMIT 1"; //consulta sql para buscar el nombre del producto
    
    String URL = "jdbc:sqlite:inventario.db"; //ruta para la base de datos
        
 try (Connection conn = DriverManager.getConnection(URL); //esperando conexion con la base de datos
             PreparedStatement pstmt = conn.prepareStatement(sql)) { //preparamos la sentencia sql
                pstmt.setString(1, nombre);
  try(ResultSet res =  pstmt.executeQuery()) {//ejecutamos la consulta
   return res.next(); //si existe alguna lo retornamos como verdadero
  }

 } catch(SQLException e) {
    System.out.println("El producto ya es existente, favor de editarlo."+ e.getMessage());
    return false; //si no existe lo retornamos como falso
}
}
}

