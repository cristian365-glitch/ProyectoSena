package untils;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Clase para manejar el hash seguro de contraseñas usando PBKDF2
 * 
 * ¿QUÉ ES PBKDF2?
 * Es un algoritmo diseñado para ser LENTO intencionalmente.
 * Esto hace que sea muy difícil probar millones de contraseñas por segundo.
 */
public class PasswordHasher {
    
    // CONFIGURACIÓN DEL ALGORITMO
    private static final int ITERATIONS = 65536;  // 65,536 iteraciones (hace el proceso más lento)
    private static final int KEY_LENGTH = 256;     // 256 bits de longitud
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";  // Algoritmo SHA-256
    
    /**
     * Genera un hash seguro de una contraseña
     * 
     * @param password - La contraseña en texto plano
     * @param salt - El salt (valor aleatorio único)
     * @return String - El hash en Base64
     * 
     * EJEMPLO:
     * password = "miPassword123"
     * salt = [bytes aleatorios]
     * resultado = "A7f3k9Xm2p..."  (irreversible)
     */
    public static String hashPassword(String password, byte[] salt) 
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        
        // PASO 1: Crear especificación con la contraseña, salt e iteraciones
        PBEKeySpec spec = new PBEKeySpec(
            password.toCharArray(),  // Convertir contraseña a array de chars
            salt,                     // Salt único
            ITERATIONS,               // Número de iteraciones
            KEY_LENGTH                // Longitud del hash resultante
        );
        
        // PASO 2: Obtener la fábrica del algoritmo PBKDF2
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
        
        // PASO 3: Generar el hash
        byte[] hash = factory.generateSecret(spec).getEncoded();
        
        // PASO 4: Convertir a Base64 para poder guardarlo como texto
        return Base64.getEncoder().encodeToString(hash);
    }
    
    /**
     * Genera un salt aleatorio de 16 bytes
     * 
     * ¿QUÉ ES EL SALT?
     * Es un valor aleatorio único que se añade a cada contraseña.
     * Dos usuarios con la MISMA contraseña tendrán DIFERENTES hashes.
     * 
     * @return byte[] - Array de 16 bytes aleatorios
     */
    public static byte[] generarSalt() {
        // SecureRandom es criptográficamente seguro (no predecible)
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];  // 16 bytes = 128 bits
        random.nextBytes(salt);
        return salt;
    }
    
    /**
     * Verifica si una contraseña coincide con un hash almacenado
     * 
     * @param password - Contraseña ingresada por el usuario
     * @param storedHash - Hash almacenado en la base de datos
     * @param storedSalt - Salt almacenado en la base de datos (en Base64)
     * @return boolean - true si coinciden, false si no
     */
    public static boolean verificarPassword(String password, String storedHash, String storedSalt) {
        try {
            // PASO 1: Decodificar el salt de Base64 a bytes
            byte[] salt = Base64.getDecoder().decode(storedSalt);
            
            // PASO 2: Generar hash de la contraseña ingresada con el mismo salt
            String hashCalculado = hashPassword(password, salt);
            
            // PASO 3: Comparar los hashes
            // Si son iguales = contraseña correcta
            return hashCalculado.equals(storedHash);
            
        } catch (Exception e) {
            System.err.println("Error verificando password: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * MÉTODO PARA TESTING - Genera hash y salt de una contraseña
     * Útil para crear usuarios manualmente en la BD
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Uso: java PasswordHasher <contraseña>");
            System.out.println("Ejemplo: java PasswordHasher miPassword123");
            return;
        }
        
        try {
            String password = args[0];
            
            // Generar salt
            byte[] salt = generarSalt();
            String saltBase64 = Base64.getEncoder().encodeToString(salt);
            
            // Generar hash
            String hash = hashPassword(password, salt);
            
            System.out.println("========================================");
            System.out.println("Contraseña: " + password);
            System.out.println("========================================");
            System.out.println("Salt (Base64): " + saltBase64);
            System.out.println("Hash (Base64): " + hash);
            System.out.println("========================================");
            System.out.println("\nSQL para insertar usuario:");
            System.out.println("INSERT INTO usuarios (email, nombre, telefono, password_hash, salt)");
            System.out.println("VALUES ('email@example.com', 'Nombre', '123456', '" + hash + "', '" + saltBase64 + "');");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

/*
 * EJEMPLO DE USO:
 * 
 * 1. CREAR NUEVO USUARIO:
 *    byte[] salt = PasswordHasher.generarSalt();
 *    String hash = PasswordHasher.hashPassword("miPassword", salt);
 *    // Guardar hash y salt en BD
 * 
 * 2. VERIFICAR LOGIN:
 *    boolean esValido = PasswordHasher.verificarPassword(
 *        passwordIngresado,
 *        hashGuardadoEnBD,
 *        saltGuardadoEnBD
 *    );
 * 
 * IMPORTANTE: NUNCA uses password = "123" directamente en la BD
 * SIEMPRE usa password_hash y salt
 */
