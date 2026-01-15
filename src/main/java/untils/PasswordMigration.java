package untils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import com.conexiones.DatabaseManager;
import com.conexiones.DatabaseManager;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Este script convierte las contraseñas de texto plano a hash seguro
 * EJECUTAR UNA SOLA VEZ después de agregar las columnas en la BD
 */
public class PasswordMigration {
    
    public static void main(String[] args) {
        DatabaseManager dbManager = DatabaseManager.getInstance();
        
        System.out.println("========================================");
        System.out.println("Iniciando migración de contraseñas...");
        System.out.println("========================================\n");
        
        try {
            // Migrar usuarios normales
            int usuariosMigrados = migrarUsuarios(dbManager);
            
            // Migrar administradores
            int adminsMigrados = migrarAdmins(dbManager);
            
            System.out.println("\n========================================");
            System.out.println("✅ Migración completada exitosamente:");
            System.out.println("   Usuarios migrados: " + usuariosMigrados);
            System.out.println("   Admins migrados: " + adminsMigrados);
            System.out.println("========================================");
            System.out.println("\n⚠️  IMPORTANTE:");
            System.out.println("1. Verifica que puedes hacer login");
            System.out.println("2. Si todo funciona, puedes eliminar");
            System.out.println("   las columnas 'password' antiguas con:");
            System.out.println("   ALTER TABLE usuarios DROP COLUMN password;");
            System.out.println("   ALTER TABLE usuarios_admin DROP COLUMN password;");
            
        } catch (Exception e) {
            System.err.println("❌ Error durante la migración: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Migra las contraseñas de usuarios normales
     */
    private static int migrarUsuarios(DatabaseManager dbManager) throws SQLException {
        int migrados = 0;
        
        try (Connection conn = dbManager.getConnection()) {
            
            // PASO 1: Leer todos los usuarios que aún tienen contraseña en texto plano
            String selectSQL = "SELECT id, email, password FROM usuarios WHERE password_hash IS NULL";
            
            try (PreparedStatement psSelect = conn.prepareStatement(selectSQL);
                 ResultSet rs = psSelect.executeQuery()) {
                
                // PASO 2: Preparar query para actualizar con hash
                String updateSQL = "UPDATE usuarios SET password_hash = ?, salt = ? WHERE id = ?";
                PreparedStatement psUpdate = conn.prepareStatement(updateSQL);
                
                // PASO 3: Procesar cada usuario
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String email = rs.getString("email");
                    String passwordPlainText = rs.getString("password");
                    
                    // Validar que tiene contraseña
                    if (passwordPlainText == null || passwordPlainText.isEmpty()) {
                        System.out.println("⚠️  Usuario " + email + " no tiene contraseña, saltando...");
                        continue;
                    }
                    
                    // PASO 4: Generar salt aleatorio (16 bytes)
                    byte[] salt = PasswordHasher.generarSalt();
                    String saltBase64 = Base64.getEncoder().encodeToString(salt);
                    
                    // PASO 5: Crear hash de la contraseña
                    String hash = PasswordHasher.hashPassword(passwordPlainText, salt);
                    
                    // PASO 6: Guardar en base de datos
                    psUpdate.setString(1, hash);
                    psUpdate.setString(2, saltBase64);
                    psUpdate.setInt(3, id);
                    psUpdate.executeUpdate();
                    
                    migrados++;
                    System.out.println("✅ Usuario migrado: " + email);
                }
                
                psUpdate.close();
            } catch (NoSuchAlgorithmException ex) {
                Logger.getLogger(PasswordMigration.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InvalidKeySpecException ex) {
                Logger.getLogger(PasswordMigration.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return migrados;
    }
    
    /**
     * Migra las contraseñas de administradores
     * (Mismo proceso que usuarios normales)
     */
    private static int migrarAdmins(DatabaseManager dbManager) throws SQLException {
        int migrados = 0;
        
        try (Connection conn = dbManager.getConnection()) {
            
            String selectSQL = "SELECT id, email, password FROM usuarios_admin WHERE password_hash IS NULL";
            
            try (PreparedStatement psSelect = conn.prepareStatement(selectSQL);
                 ResultSet rs = psSelect.executeQuery()) {
                
                String updateSQL = "UPDATE usuarios_admin SET password_hash = ?, salt = ? WHERE id = ?";
                PreparedStatement psUpdate = conn.prepareStatement(updateSQL);
                
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String email = rs.getString("email");
                    String passwordPlainText = rs.getString("password");
                    
                    if (passwordPlainText == null || passwordPlainText.isEmpty()) {
                        System.out.println("⚠️  Admin " + email + " no tiene contraseña, saltando...");
                        continue;
                    }
                    
                    byte[] salt = PasswordHasher.generarSalt();
                    String saltBase64 = Base64.getEncoder().encodeToString(salt);
                    String hash = PasswordHasher.hashPassword(passwordPlainText, salt);
                    
                    psUpdate.setString(1, hash);
                    psUpdate.setString(2, saltBase64);
                    psUpdate.setInt(3, id);
                    psUpdate.executeUpdate();
                    
                    migrados++;
                    System.out.println("✅ Admin migrado: " + email);
                }
                
                psUpdate.close();
            } catch (NoSuchAlgorithmException ex) {
                Logger.getLogger(PasswordMigration.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InvalidKeySpecException ex) {
                Logger.getLogger(PasswordMigration.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return migrados;
    }
}