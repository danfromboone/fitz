package dfb.fitz.property;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

/**
 * Utility for encrypting and decrypting property values using {@code AES/GCM/NoPadding}
 * and formatting encrypted values with the {@code ENC(...)} convention.
 */
public final class PropertyEncryptor {
    /** Prefix for encrypted property values. */
    public static final String ENC_PREFIX = "ENC(";
    /** Suffix for encrypted property values. */
    public static final String ENC_SUFFIX = ")";

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int AES_KEY_SIZE_BITS = 256;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PropertyEncryptor() {
    }

    /**
     * Checks whether the given property value is wrapped in {@code ENC(...)}.
     *
     * @param value the property value to inspect
     * @return {@code true} if the value starts with {@code "ENC("} and ends with {@code ")"}; {@code false} otherwise
     */
    public static boolean isEncrypted(String value) {
        return value != null
                && value.startsWith(ENC_PREFIX)
                && value.endsWith(ENC_SUFFIX)
                && value.length() > ENC_PREFIX.length() + ENC_SUFFIX.length();
    }

    /**
     * Generates a new random 256-bit AES key encoded as a Base64 string.
     *
     * @return Base64-encoded 256-bit AES key
     */
    public static String generateKeyString() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(AES_KEY_SIZE_BITS, SECURE_RANDOM);
            SecretKey key = keyGen.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("AES algorithm not available", e);
        }
    }

    /**
     * Loads an AES {@link SecretKey} from an OS file.
     * <p>
     * If the trimmed file contents are valid Base64 decoding to 16, 24, or 32 bytes, those bytes
     * are used directly as the AES key. Otherwise, the UTF-8 bytes of the trimmed content are hashed
     * with SHA-256 to derive a 256-bit AES key.
     *
     * @param keyFilePath path to the key file on the filesystem (must not be {@code null})
     * @return the loaded {@link SecretKey}
     * @throws UncheckedIOException if the key file cannot be read
     * @throws IllegalArgumentException if the key file is empty
     */
    public static SecretKey loadKey(Path keyFilePath) {
        Objects.requireNonNull(keyFilePath, "keyFilePath must not be null");
        String rawContent;
        try {
            rawContent = Files.readString(keyFilePath, StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read encryption key file: " + keyFilePath, e);
        }
        return parseKey(rawContent);
    }

    /**
     * Parses a key string into an AES {@link SecretKey}.
     *
     * @param keyString Base64-encoded AES key or passphrase string (must not be {@code null} or blank)
     * @return the {@link SecretKey}
     */
    public static SecretKey parseKey(String keyString) {
        Objects.requireNonNull(keyString, "keyString must not be null");
        String trimmed = keyString.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Encryption key must not be empty");
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(trimmed);
            if (decoded.length == 16 || decoded.length == 24 || decoded.length == 32) {
                return new SecretKeySpec(decoded, ALGORITHM);
            }
        } catch (IllegalArgumentException ignored) {
            // Fall through to SHA-256 key derivation
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(trimmed.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Encrypts the given plaintext string using the key in the specified OS file and returns
     * the result formatted as {@code ENC(<base64>)}.
     *
     * @param plaintext   the plaintext value to encrypt (must not be {@code null})
     * @param keyFilePath path to the key file on the filesystem (must not be {@code null})
     * @return the encrypted token in {@code ENC(...)} format
     */
    public static String encrypt(String plaintext, Path keyFilePath) {
        return encrypt(plaintext, loadKey(keyFilePath));
    }

    /**
     * Encrypts the given plaintext string using the provided {@link SecretKey} and returns
     * the result formatted as {@code ENC(<base64>)}.
     *
     * @param plaintext the plaintext value to encrypt (must not be {@code null})
     * @param secretKey the AES secret key (must not be {@code null})
     * @return the encrypted token in {@code ENC(...)} format
     */
    public static String encrypt(String plaintext, SecretKey secretKey) {
        Objects.requireNonNull(plaintext, "plaintext must not be null");
        Objects.requireNonNull(secretKey, "secretKey must not be null");

        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return ENC_PREFIX + Base64.getEncoder().encodeToString(combined) + ENC_SUFFIX;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt property value", e);
        }
    }

    /**
     * Decrypts a value formatted as {@code ENC(<base64>)} using the key in the specified OS file.
     * If the value is not wrapped in {@code ENC(...)}, it is returned unchanged.
     *
     * @param value       the property value (encrypted or plain)
     * @param keyFilePath path to the key file on the filesystem (must not be {@code null} if {@code value} is encrypted)
     * @return the decrypted plaintext string
     */
    public static String decrypt(String value, Path keyFilePath) {
        if (!isEncrypted(value)) {
            return value;
        }
        return decrypt(value, loadKey(keyFilePath));
    }

    /**
     * Decrypts a value formatted as {@code ENC(<base64>)} using the provided {@link SecretKey}.
     * If the value is not wrapped in {@code ENC(...)}, it is returned unchanged.
     *
     * @param value     the property value (encrypted or plain)
     * @param secretKey the AES secret key (must not be {@code null} if {@code value} is encrypted)
     * @return the decrypted plaintext string
     */
    public static String decrypt(String value, SecretKey secretKey) {
        if (!isEncrypted(value)) {
            return value;
        }
        Objects.requireNonNull(secretKey, "secretKey must not be null");

        String payload = value.substring(ENC_PREFIX.length(), value.length() - ENC_SUFFIX.length()).trim();
        try {
            byte[] combined = Base64.getDecoder().decode(payload);
            if (combined.length <= GCM_IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("Invalid encrypted payload: too short");
            }

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, combined, 0, GCM_IV_LENGTH_BYTES);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            byte[] decrypted = cipher.doFinal(combined, GCM_IV_LENGTH_BYTES, combined.length - GCM_IV_LENGTH_BYTES);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Failed to decrypt property value", e);
        }
    }
}
