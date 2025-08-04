package J2FA.just2FA.storage;

import J2FA.just2FA.Just2FA;
import org.bukkit.Bukkit;

import java.io.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class StorageManager {
    private final Just2FA plugin;
    private final File dataFile;
    private final Map<UUID, PlayerData> cache;

    public StorageManager(Just2FA plugin) {
        this.plugin = plugin;
        this.cache = new ConcurrentHashMap<>();
        
        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        this.dataFile = new File(dataFolder, "players.dat");
        loadData();
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        if (!dataFile.exists()) {
            return;
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(dataFile))) {
            Map<UUID, PlayerData> data = (Map<UUID, PlayerData>) ois.readObject();
            cache.putAll(data);
            plugin.getLogger().info("Loaded " + cache.size() + " player data entries.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load player data", e);
        }
    }

    public void saveData() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(dataFile))) {
                oos.writeObject(cache);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save player data", e);
            }
        });
    }

    public PlayerData getPlayerData(UUID uuid) {
        return cache.get(uuid);
    }

    public void savePlayerData(PlayerData data) {
        cache.put(data.getUuid(), data);
        saveData();
    }

    public void removePlayerData(UUID uuid) {
        cache.remove(uuid);
        saveData();
    }

    public Map<UUID, PlayerData> getAllPlayerData() {
        return new ConcurrentHashMap<>(cache);
    }

    public void clearCache() {
        cache.clear();
    }
}