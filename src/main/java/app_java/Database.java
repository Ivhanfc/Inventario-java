package com.ivhanfc.scannerjs.app_java;
import java.lang.Thread.State;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
public class Database {
    public static void CrearDB(){
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:inventario.db")) {
      Statement stmt = conn.createStatement();

      String sql = "CREATE TABLE IF NOT EXISTS PRODUCTOS (" +
      "ID INTEGER PRIMARY KEY AUTOINCREMENT,"+
      "NOMBRE VARCHAR (200),"+
      "CANTIDAD INTEGER,"+
      "PRECIO REAL"+
      ");";

    stmt.execute(sql);
    System.out.println("Base de datos creada correctamente");
} catch (Exception e){
    e.printStackTrace();
}}

public static ResultSet showProducts() throws SQLException{
     String url = "jdbc:sqlite:inventario.db"; // ruta a tu base
    Connection conn = DriverManager.getConnection(url); 
        Statement stmt = conn.createStatement();
    return stmt.executeQuery("SELECT * FROM PRODUCTOS");

 
    }
    public static void insertProduct(String nombre, int cantidad, double precio){
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:inventario.db")) {
            String sql = "INSERT INTO productos(nombre, cantidad, precio) VALUES (?,?,?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, nombre);
            pstmt.setInt(2, cantidad);
            pstmt.setDouble(3, precio);
            pstmt.executeUpdate();
            System.out.println("Datos insertados correctamente");
        }
        catch(SQLException e){
            System.out.println("Error al insertar el producto: " + e.getMessage());

    }
}
     public static void DeleteProduct(int id) {
             String url = "jdbc:sqlite:inventario.db"; // ruta a tu base

        try (Connection conn = DriverManager.getConnection(url))
         {
            String sql = "DELETE FROM PRODUCTOS WHERE ID = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, id);
            int rows = pstmt.executeUpdate();
            if (rows > 0)            
            {
                System.out.println("Producto con ID: "+ id + " eliminado correctamente");
            }
            else {
                System.out.println("No se encontró producto con ID " + id);
            }

        } catch(SQLException e) {
            System.out.println("Error al eliminar el producto: "+ e.getMessage());   
    }
    }
}
