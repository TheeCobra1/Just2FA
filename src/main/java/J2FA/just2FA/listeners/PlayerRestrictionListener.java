package J2FA.just2FA.listeners;

import J2FA.just2FA.Just2FA;
import J2FA.just2FA.auth.AuthManager;
import J2FA.just2FA.utils.MessageUtil;
import J2FA.just2FA.utils.MapQRRenderer;
import J2FA.just2FA.hooks.WorldGuardHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class PlayerRestrictionListener implements Listener {
    private final Just2FA plugin;
    private final AuthManager authManager;
    private final Map<UUID, Long> lastMessageTime;

    public PlayerRestrictionListener(Just2FA plugin, AuthManager authManager) {
        this.plugin = plugin;
        this.authManager = authManager;
        this.lastMessageTime = new ConcurrentHashMap<>();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        if (player.hasPermission("just2fa.bypass")) {
            return;
        }
        
        if (!authManager.hasSetup(player)) {
            if (plugin.getConfig().getBoolean("security.require-2fa", false)) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (plugin.getConfig().getBoolean("restrictions.spawn-restriction.teleport-to-spawn", false)) {
                        org.bukkit.Location spawn = player.getWorld().getSpawnLocation();
                        spawn.setY(player.getWorld().getHighestBlockYAt(spawn) + 1);
                        player.teleport(spawn);
                    }
                    player.sendMessage(MessageUtil.format(plugin.getConfig().getString("messages.authentication-required")));
                    player.sendMessage(MessageUtil.format("&eUse /2fa setup to configure your authenticator"));
                }, 20L);
            }
            return;
        }
        
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (plugin.getConfig().getBoolean("restrictions.spawn-restriction.teleport-to-spawn", false)) {
                org.bukkit.Location spawn = player.getWorld().getSpawnLocation();
                spawn.setY(player.getWorld().getHighestBlockYAt(spawn) + 1);
                player.teleport(spawn);
            }
            
            player.sendMessage(MessageUtil.format(plugin.getConfig().getString("messages.authentication-required")));
            player.sendMessage(MessageUtil.format(plugin.getConfig().getString("messages.verify-prompt")));
            
            long timeout = plugin.getConfig().getLong("authentication.timeout", 300);
            if (plugin.getConfig().getBoolean("security.kick-on-timeout", true)) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (!authManager.isAuthenticated(player) && player.isOnline()) {
                        player.kickPlayer(MessageUtil.format(plugin.getConfig().getString("messages.kicked-timeout")));
                    }
                }, timeout * 20L);
                
                if (timeout > 60) {
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (!authManager.isAuthenticated(player) && player.isOnline()) {
                            player.sendMessage(MessageUtil.format(plugin.getConfig().getString("messages.timeout-warning")
                                    .replace("%time%", String.valueOf(60))));
                        }
                    }, (timeout - 60) * 20L);
                }
            }
        }, 20L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        authManager.removeAuthentication(event.getPlayer().getUniqueId());
        lastMessageTime.remove(event.getPlayer().getUniqueId());
        MapQRRenderer.destroyQRMap(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!shouldRestrict(event.getPlayer())) {
            return;
        }
        
        Player player = event.getPlayer();
        boolean allowMovement = plugin.getConfig().getBoolean("restrictions.allow-movement", true);
        
        if (plugin.getConfig().getBoolean("restrictions.spawn-restriction.enabled", false)) {
            org.bukkit.Location spawn = player.getWorld().getSpawnLocation();
            double radius = plugin.getConfig().getDouble("restrictions.spawn-restriction.radius", 5);
            
            if (player.getLocation().distance(spawn) <= radius) {
                if (plugin.getConfig().getBoolean("restrictions.spawn-restriction.freeze-at-spawn", false)) {
                    allowMovement = false;
                }
            }
        }
        
        if (WorldGuardHook.isEnabled()) {
            Boolean regionOverride = WorldGuardHook.shouldAllowMovement(player);
            if (regionOverride != null) {
                allowMovement = regionOverride;
            }
        }
        
        if (!allowMovement) {
            if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
                event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
                org.bukkit.Location newLoc = event.getFrom().clone();
                newLoc.setPitch(event.getTo().getPitch());
                newLoc.setYaw(event.getTo().getYaw());
                event.setTo(newLoc);
                sendAuthMessage(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (shouldRestrict(event.getPlayer()) && isActionBlocked("BLOCK_BREAK")) {
            event.setCancelled(true);
            sendAuthMessage(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (shouldRestrict(event.getPlayer()) && isActionBlocked("BLOCK_PLACE")) {
            event.setCancelled(true);
            sendAuthMessage(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (shouldRestrict(event.getPlayer()) && isActionBlocked("INTERACT")) {
            event.setCancelled(true);
            sendAuthMessage(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (shouldRestrict(event.getPlayer()) && isActionBlocked("INTERACT_ENTITY")) {
            event.setCancelled(true);
            sendAuthMessage(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            if (shouldRestrict(player) && isActionBlocked("INVENTORY_CLICK")) {
                event.setCancelled(true);
                sendAuthMessage(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (shouldRestrict(event.getPlayer()) && isActionBlocked("ITEM_DROP")) {
            event.setCancelled(true);
            sendAuthMessage(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        if (shouldRestrict(event.getPlayer()) && isActionBlocked("ITEM_PICKUP")) {
            event.setCancelled(true);
            sendAuthMessage(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (shouldRestrict(event.getPlayer()) && isActionBlocked("TELEPORT")) {
            if (event.getCause() != PlayerTeleportEvent.TeleportCause.UNKNOWN &&
                event.getCause() != PlayerTeleportEvent.TeleportCause.PLUGIN) {
                event.setCancelled(true);
                sendAuthMessage(event.getPlayer());
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (shouldRestrict(event.getPlayer()) && isActionBlocked("PORTAL")) {
            event.setCancelled(true);
            sendAuthMessage(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (shouldRestrict(event.getPlayer()) && isActionBlocked("CHAT")) {
            event.setCancelled(true);
            sendAuthMessage(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (shouldRestrict(event.getPlayer())) {
            String command = event.getMessage().toLowerCase();
            if (!command.startsWith("/2fa") && !command.startsWith("/auth") && 
                !command.startsWith("/authenticator") && isActionBlocked("COMMAND")) {
                event.setCancelled(true);
                sendAuthMessage(event.getPlayer());
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (shouldRestrict(player) && isActionBlocked("DAMAGE")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            if (shouldRestrict(player) && isActionBlocked("DAMAGE")) {
                event.setCancelled(true);
                sendAuthMessage(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        if (shouldRestrict(event.getPlayer()) && isActionBlocked("CONSUME")) {
            event.setCancelled(true);
            sendAuthMessage(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerFish(PlayerFishEvent event) {
        if (shouldRestrict(event.getPlayer()) && isActionBlocked("FISH")) {
            event.setCancelled(true);
            sendAuthMessage(event.getPlayer());
        }
    }

    private boolean shouldRestrict(Player player) {
        if (player.hasPermission("just2fa.bypass")) {
            return false;
        }
        
        if (WorldGuardHook.isEnabled() && WorldGuardHook.shouldBypass2FA(player)) {
            return false;
        }
        
        boolean require2FA = plugin.getConfig().getBoolean("security.require-2fa", false);
        if (WorldGuardHook.isEnabled() && WorldGuardHook.shouldRequire2FA(player)) {
            require2FA = true;
        }
        
        if (!authManager.hasSetup(player) && !require2FA) {
            return false;
        }
        
        return !authManager.isAuthenticated(player);
    }

    private boolean isActionBlocked(String action) {
        List<String> blockedActions = plugin.getConfig().getStringList("restrictions.blocked-actions");
        return blockedActions.contains(action);
    }

    private void sendAuthMessage(Player player) {
        UUID uuid = player.getUniqueId();
        Long lastTime = lastMessageTime.get(uuid);
        long currentTime = System.currentTimeMillis();
        
        if (lastTime == null || currentTime - lastTime > 3000) {
            player.sendMessage(MessageUtil.format(plugin.getConfig().getString("messages.authentication-required")));
            lastMessageTime.put(uuid, currentTime);
        }
    }
}