package cl.silbom.backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * QUÉ HACE:
 * Utilidad criptográfica para cifrar y descifrar información sensible en reposo (AES-256-GCM).
 *
 * POR QUÉ ESTÁ ESCRITO ASÍ (CONTROL 5):
 * - AES-256-GCM es cifrado autenticado (AEAD): detecta cualquier modificación del dato cifrado.
 * - IV aleatorio de 12 bytes por cada cifrado: el mismo texto cifrado dos veces da salidas distintas.
 * - La clave NO tiene valor por defecto: debe venir de la variable de entorno ENCRYPTION_SECRET,
 *   en Base64, y decodificar a exactamente 32 bytes. Si falta o es inválida, la app no arranca.
 *   Generar una con:  openssl rand -base64 32
 */
@Component
public class CryptoUtils {

    private static final Logger log = LoggerFactory.getLogger(CryptoUtils.class);

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;     // 96 bits, recomendado para GCM
    private static final int GCM_TAG_LENGTH = 128;   // bits del tag de autenticación
    private static final int KEY_LENGTH_BYTES = 32;  // 256 bits
    private static final String NOTA_ILEGIBLE = "[nota no disponible: fallo de integridad]";

    // Una sola instancia: SecureRandom es thread-safe y evita bloqueos de getInstanceStrong()
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKey secretKey;

    public CryptoUtils(@Value("${silbom.security.encryption-secret}") String secretBase64) {
        if (secretBase64 == null || secretBase64.isBlank()) {
            throw new IllegalStateException("silbom.security.encryption-secret no puede estar vacía");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secretBase64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                "silbom.security.encryption-secret debe estar en Base64 (openssl rand -base64 32)");
        }
        if (keyBytes.length != KEY_LENGTH_BYTES) {
            throw new IllegalStateException(
                "silbom.security.encryption-secret debe decodificar a exactamente 32 bytes");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /** Cifra texto plano y devuelve Base64 de [IV (12 bytes) + texto cifrado + tag]. */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return plaintext;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
            buffer.put(iv);
            buffer.put(cipherText);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("Error al cifrar dato sensible", e);
        }
    }

    /**
     * Descifra un valor generado por encrypt().
     * Si el dato fue alterado o la clave no corresponde, NO se devuelve el valor crudo:
     * se registra una advertencia (sin contenido) y se devuelve un marcador.
     */
    public String decrypt(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.isBlank()) {
            return encryptedBase64;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedBase64);
            if (decoded.length <= GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Formato cifrado inválido");
            }

            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("No se pudo descifrar un dato en reposo (posible manipulación o clave incorrecta): {}",
                     e.getClass().getSimpleName());
            return NOTA_ILEGIBLE;
        }
    }
}
