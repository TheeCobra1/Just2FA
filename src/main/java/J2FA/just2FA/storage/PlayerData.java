package J2FA.just2FA.storage;

import java.io.Serializable;
import java.util.UUID;

public class PlayerData implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final UUID uuid;
    private final String playerName;
    private final String secret;
    private long setupTime;
    private long lastLogin;
    private String lastIp;

    public PlayerData(UUID uuid, String playerName, String secret, long setupTime, String lastIp) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.secret = secret;
        this.setupTime = setupTime;
        this.lastLogin = setupTime;
        this.lastIp = lastIp;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getSecret() {
        return secret;
    }

    public long getSetupTime() {
        return setupTime;
    }

    public long getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getLastIp() {
        return lastIp;
    }

    public void setLastIp(String lastIp) {
        this.lastIp = lastIp;
    }
}