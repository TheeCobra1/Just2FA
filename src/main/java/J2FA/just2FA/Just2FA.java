package J2FA.just2FA;

import J2FA.just2FA.auth.AuthManager;
import J2FA.just2FA.commands.TwoFACommand;
import J2FA.just2FA.listeners.PlayerRestrictionListener;
import J2FA.just2FA.listeners.MapSecurityListener;
import J2FA.just2FA.storage.StorageManager;
import J2FA.just2FA.utils.MessageUtil;
import J2FA.just2FA.hooks.WorldGuardHook;
import org.bukkit.plugin.java.JavaPlugin;

public final class Just2FA extends JavaPlugin {
    
    private StorageManager storageManager;
    private AuthManager authManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        try {
            Class.forName("com.warrenstrange.googleauth.GoogleAuthenticator");
        } catch (ClassNotFoundException e) {
            getLogger().severe("GoogleAuth library not found! Please ensure all dependencies are properly installed.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        this.storageManager = new StorageManager(this);
        this.authManager = new AuthManager(this, storageManager);
        
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            WorldGuardHook.initialize();
        }
        
        TwoFACommand command = new TwoFACommand(this, authManager);
        getCommand("2fa").setExecutor(command);
        getCommand("2fa").setTabCompleter(command);
        
        getServer().getPluginManager().registerEvents(new PlayerRestrictionListener(this, authManager), this);
        getServer().getPluginManager().registerEvents(new MapSecurityListener(this), this);
        
        String prefix = getConfig().getString("messages.prefix", "&8[&6Just2FA&8] &7");
        getLogger().info("Just2FA v" + getDescription().getVersion() + " has been enabled!");
        getLogger().info("Secure 2FA authentication is now active.");
        
        if (getConfig().getBoolean("security.require-2fa", false)) {
            getLogger().info("2FA is required for all players!");
        }
        
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            storageManager.saveData();
        }, 6000L, 6000L);
    }

    @Override
    public void onDisable() {
        if (storageManager != null) {
            storageManager.saveData();
        }
        
        getServer().getScheduler().cancelTasks(this);
        getLogger().info("Just2FA has been disabled. All data saved.");
    }
    
    public AuthManager getAuthManager() {
        return authManager;
    }
    
    public StorageManager getStorageManager() {
        return storageManager;
    }
}
