package J2FA.just2FA.utils;

import org.bukkit.ChatColor;

public class MessageUtil {
    
    public static String format(String message) {
        if (message == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public static String stripColor(String message) {
        if (message == null) {
            return "";
        }
        return ChatColor.stripColor(message);
    }
}