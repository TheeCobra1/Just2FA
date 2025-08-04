package J2FA.just2FA.auth;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import com.warrenstrange.googleauth.KeyRepresentation;
import J2FA.just2FA.Just2FA;
import J2FA.just2FA.storage.PlayerData;
import J2FA.just2FA.storage.StorageManager;
import org.bukkit.entity.Player;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class AuthManager {
    private final Just2FA plugin;
    private final GoogleAuthenticator authenticator;
    private final StorageManager storageManager;
    private final Map<UUID, String> pendingSetup;
    private final Map<UUID, Long> authenticatedPlayers;
    private final Map<UUID, Integer> failedAttempts;
    private final Map<UUID, Long> lockedOutPlayers;
    private final SecretKey encryptionKey;

    public AuthManager(Just2FA plugin, StorageManager storageManager) {
        this.plugin = plugin;
        this.storageManager = storageManager;
        this.pendingSetup = new ConcurrentHashMap<>();
        this.authenticatedPlayers = new ConcurrentHashMap<>();
        this.failedAttempts = new ConcurrentHashMap<>();
        this.lockedOutPlayers = new ConcurrentHashMap<>();
        
        GoogleAuthenticatorConfig config = new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
                .setCodeDigits(plugin.getConfig().getInt("authentication.code-length", 6))
                .setWindowSize(plugin.getConfig().getInt("authentication.window-size", 3))
                .setTimeStepSizeInMillis(TimeUnit.SECONDS.toMillis(30))
                .setKeyRepresentation(KeyRepresentation.BASE32)
                .build();
        
        this.authenticator = new GoogleAuthenticator(config);
        this.encryptionKey = generateEncryptionKey();
    }

    private SecretKey generateEncryptionKey() {
        try {
            File keyFile = new File(plugin.getDataFolder(), "data/encryption.key");
            
            if (keyFile.exists()) {
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(keyFile))) {
                    byte[] keyBytes = (byte[]) ois.readObject();
                    return new SecretKeySpec(keyBytes, "AES");
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load encryption key, generating new one: " + e.getMessage());
                }
            }
            
            if (!keyFile.getParentFile().exists()) {
                keyFile.getParentFile().mkdirs();
            }
            
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256, new SecureRandom());
            SecretKey key = keyGen.generateKey();
            
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(keyFile))) {
                oos.writeObject(key.getEncoded());
            }
            
            return key;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to generate encryption key: " + e.getMessage());
            return null;
        }
    }

    public String generateSecret() {
        GoogleAuthenticatorKey key = authenticator.createCredentials();
        return key.getKey();
    }

    public String getQRCodeURL(Player player, String secret) {
        try {
            String issuer = plugin.getConfig().getString("authentication.issuer", "Just2FA Server");
            String accountName = player.getName();
            
            String otpUrl = String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s",
                java.net.URLEncoder.encode(issuer, "UTF-8"),
                java.net.URLEncoder.encode(accountName, "UTF-8"),
                secret,
                java.net.URLEncoder.encode(issuer, "UTF-8")
            );
            
            return otpUrl;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to generate QR URL: " + e.getMessage());
            return GoogleAuthenticatorQRGenerator.getOtpAuthURL(
                plugin.getConfig().getString("authentication.issuer", "Just2FA Server"),
                player.getName(),
                new GoogleAuthenticatorKey.Builder(secret).build()
            );
        }
    }

    public boolean setupPlayer(Player player, String secret) {
        if (hasSetup(player)) {
            return false;
        }
        
        PlayerData data = new PlayerData(
                player.getUniqueId(),
                player.getName(),
                encryptSecret(secret),
                System.currentTimeMillis(),
                player.getAddress().getAddress().getHostAddress()
        );
        
        storageManager.savePlayerData(data);
        pendingSetup.remove(player.getUniqueId());
        return true;
    }

    public boolean verifyCode(Player player, int code) {
        UUID uuid = player.getUniqueId();
        
        if (isLockedOut(uuid)) {
            return false;
        }
        
        String secret = null;
        PlayerData data = storageManager.getPlayerData(uuid);
        
        if (data != null) {
            secret = decryptSecret(data.getSecret());
        } else {
            String pendingSecret = pendingSetup.get(uuid);
            if (pendingSecret != null) {
                secret = pendingSecret;
            } else {
                return false;
            }
        }
        
        boolean valid = authenticator.authorize(secret, code);
        
        if (valid) {
            if (data != null) {
                authenticatedPlayers.put(uuid, System.currentTimeMillis());
                failedAttempts.remove(uuid);
                lockedOutPlayers.remove(uuid);
                data.setLastLogin(System.currentTimeMillis());
                data.setLastIp(player.getAddress().getAddress().getHostAddress());
                storageManager.savePlayerData(data);
            }
        } else {
            int attempts = failedAttempts.getOrDefault(uuid, 0) + 1;
            failedAttempts.put(uuid, attempts);
            
            if (attempts >= plugin.getConfig().getInt("security.max-attempts", 5)) {
                long lockoutTime = plugin.getConfig().getLong("security.lockout-time", 300) * 1000;
                lockedOutPlayers.put(uuid, System.currentTimeMillis() + lockoutTime);
                failedAttempts.remove(uuid);
            }
        }
        
        return valid;
    }

    public boolean isAuthenticated(Player player) {
        if (player.hasPermission("just2fa.bypass")) {
            return true;
        }
        
        UUID uuid = player.getUniqueId();
        Long authTime = authenticatedPlayers.get(uuid);
        
        if (authTime == null) {
            return false;
        }
        
        long timeout = plugin.getConfig().getLong("authentication.timeout", 300) * 1000;
        if (System.currentTimeMillis() - authTime > timeout) {
            authenticatedPlayers.remove(uuid);
            return false;
        }
        
        return true;
    }

    public boolean hasSetup(Player player) {
        return storageManager.getPlayerData(player.getUniqueId()) != null;
    }

    public void removeAuthentication(UUID uuid) {
        authenticatedPlayers.remove(uuid);
        failedAttempts.remove(uuid);
        lockedOutPlayers.remove(uuid);
    }

    public void removePlayerData(UUID uuid) {
        storageManager.removePlayerData(uuid);
        removeAuthentication(uuid);
    }

    public void startSetup(Player player, String secret) {
        pendingSetup.put(player.getUniqueId(), secret);
    }

    public String getPendingSetup(Player player) {
        return pendingSetup.get(player.getUniqueId());
    }

    public void cancelSetup(Player player) {
        pendingSetup.remove(player.getUniqueId());
    }

    private boolean isLockedOut(UUID uuid) {
        Long lockoutEnd = lockedOutPlayers.get(uuid);
        if (lockoutEnd == null) {
            return false;
        }
        
        if (System.currentTimeMillis() >= lockoutEnd) {
            lockedOutPlayers.remove(uuid);
            return false;
        }
        
        return true;
    }

    private String encryptSecret(String secret) {
        if (encryptionKey == null || !plugin.getConfig().getBoolean("storage.encryption", true)) {
            return secret;
        }
        
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey);
            byte[] encrypted = cipher.doFinal(secret.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to encrypt secret: " + e.getMessage());
            return secret;
        }
    }

    private String decryptSecret(String encryptedSecret) {
        if (encryptionKey == null || !plugin.getConfig().getBoolean("storage.encryption", true)) {
            return encryptedSecret;
        }
        
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedSecret));
            return new String(decrypted);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to decrypt secret: " + e.getMessage());
            try {
                Base64.getDecoder().decode(encryptedSecret);
                return encryptedSecret;
            } catch (Exception base64Error) {
                return encryptedSecret;
            }
        }
    }

    public Map<UUID, Long> getAuthenticatedPlayers() {
        return new HashMap<>(authenticatedPlayers);
    }

    public int getFailedAttempts(Player player) {
        return failedAttempts.getOrDefault(player.getUniqueId(), 0);
    }
}