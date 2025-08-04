package J2FA.just2FA.listeners;

import J2FA.just2FA.Just2FA;
import J2FA.just2FA.utils.MapQRRenderer;
import J2FA.just2FA.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

public class MapSecurityListener implements Listener {
    private final Just2FA plugin;

    public MapSecurityListener(Just2FA plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (MapQRRenderer.isQRMap(item)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(MessageUtil.format("&cYou cannot drop your 2FA QR map! Complete setup or use /2fa remove."));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        
        if (MapQRRenderer.isQRMap(current) || MapQRRenderer.isQRMap(cursor)) {
            if (event.getInventory().getType() != InventoryType.PLAYER && 
                event.getInventory().getType() != InventoryType.CRAFTING) {
                event.setCancelled(true);
                event.getWhoClicked().sendMessage(MessageUtil.format("&cYou cannot store your 2FA QR map!"));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        ItemStack dragged = event.getOldCursor();
        if (MapQRRenderer.isQRMap(dragged)) {
            if (event.getInventory().getType() != InventoryType.PLAYER && 
                event.getInventory().getType() != InventoryType.CRAFTING) {
                event.setCancelled(true);
                event.getWhoClicked().sendMessage(MessageUtil.format("&cYou cannot store your 2FA QR map!"));
            }
        }
    }
    
    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        
        if (MapQRRenderer.isQRMap(item) && MapQRRenderer.getPlayerMap(player) == null) {
            player.getInventory().setItem(event.getNewSlot(), null);
            player.sendMessage(MessageUtil.format("&cThis QR map is not yours! It has been destroyed."));
        }
    }
}