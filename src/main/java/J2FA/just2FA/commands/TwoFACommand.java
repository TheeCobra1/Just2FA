package J2FA.just2FA.commands;

import J2FA.just2FA.Just2FA;
import J2FA.just2FA.auth.AuthManager;
import J2FA.just2FA.utils.MessageUtil;
import J2FA.just2FA.utils.QRCodeUtil;
import J2FA.just2FA.utils.MapQRRenderer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class TwoFACommand implements CommandExecutor, TabCompleter {
    private final Just2FA plugin;
    private final AuthManager authManager;

    public TwoFACommand(Just2FA plugin, AuthManager authManager) {
        this.plugin = plugin;
        this.authManager = authManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "setup":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(MessageUtil.format("&cThis command can only be used by players!"));
                    return true;
                }
                handleSetup((Player) sender);
                break;

            case "verify":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(MessageUtil.format("&cThis command can only be used by players!"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(MessageUtil.format(plugin.getConfig().getString("messages.verify-prompt")));
                    return true;
                }
                handleVerify((Player) sender, args[1]);
                break;

            case "remove":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(MessageUtil.format("&cThis command can only be used by players!"));
                    return true;
                }
                handleRemove((Player) sender);
                break;

            case "admin":
                if (!sender.hasPermission("just2fa.admin")) {
                    sender.sendMessage(MessageUtil.format(plugin.getConfig().getString("messages.no-permission")));
                    return true;
                }
                if (args.length < 2) {
                    sendAdminHelp(sender);
                    return true;
                }
                handleAdmin(sender, Arrays.copyOfRange(args, 1, args.length));
                break;

            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void handleSetup(Player player) {
        if (!player.hasPermission("just2fa.use")) {
            player.sendMessage(MessageUtil.format(plugin.getConfig().getString("messages.no-permission")));
            return;
        }

        if (authManager.hasSetup(player)) {
            player.sendMessage(MessageUtil.format(plugin.getConfig().getString("messages.already-setup")));
            return;
        }

        String pendingSecret = authManager.getPendingSetup(player);
        if (pendingSecret != null) {
            MapQRRenderer.destroyQRMap(player);
            displayQRCode(player, pendingSecret);
            return;
        }

        player.sendMessage(MessageUtil.format(plugin.getConfig().getString("messages.setup-start")));
        
        MapQRRenderer.destroyQRMap(player);
        
        String secret = authManager.generateSecret();
        authManager.startSetup(player, secret);
        
        displayQRCode(player, secret);
        
        player.sendMessage(MessageUtil.format(plugin.getConfig().getString("messages.setup-manual-key")
                .replace("%key%", secret)));
        player.sendMessage(MessageUtil.format(plugin.getConfig().getString("messages.setup-verify")));
    }

    private void displayQRCode(Player player, String secret) {
        String qrUrl = authManager.getQRCodeURL(player, secret);
        player.sendMessage(MessageUtil.format(plugin.getConfig().getString("messages.setup-qr-code")));
        
        String issuer = plugin.getConfig().getString("authentication.issuer", "Just2FA");
        MapQRRenderer.giveQRMap(player, qrUrl, issuer);
        
        if (plugin.getConfig().getBoolean("qr-code.show-chat", false)) {
            try {
                QRCodeUtil.sendQRCode(player, qrUrl);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send chat QR code to " + player.getName() + ": " + e.getMessage());
            }
        }
    }

    private void handleVerify(Player player, String codeStr) {
        try {
            int code = Integer.parseInt(codeStr);
            
            String pendingSecret = authManager.getPendingSetup(player);
            if (pendingSecret != null) {
                if (authManager.verifyCode(player, code)) {
                    boolean success = authManager.setupPlayer(player, pendingSecret);
                    if (success) {
                        authManager.cancelSetup(player);
                        MapQRRenderer.destroyQRMap(player);
                        player.sendMessage(MessageUtil.format(plugin.getConfig().getString("messages.setup-complete")));
                    } else {
                        player.sendMessage(MessageUtil.format("&cFailed to save 2FA setup. Please try again."));
                    }
                } else {
                    player.sendMessage(MessageUtil.format(plugin.getConfig().getString("messages.verify-failure")));
                    player.sendMessage(MessageUtil.format("&7Make sure your device time is synchronized."));
                }
                return;
            }
            
            if (!authManager.hasSetup(player)) {
                player.sendMessage(MessageUtil.format(plugin.getConfig().getString("messages.not-setup")));
                return;
            }
            
            if (authManager.verifyCode(player, code)) {
                player.sendMessage(MessageUtil.format(plugin.getConfig().getString("messages.verify-success")));
            } else {
                player.sendMessage(MessageUtil.format(plugin.getConfig().getString("messages.verify-failure")));
                
                int attempts = authManager.getFailedAttempts(player);
                int maxAttempts = plugin.getConfig().getInt("security.max-attempts", 5);
                if (attempts > 0 && attempts < maxAttempts) {
                    player.sendMessage(MessageUtil.format("&cAttempts remaining: " + (maxAttempts - attempts)));
                }
            }
        } catch (NumberFormatException e) {
            player.sendMessage(MessageUtil.format("&cInvalid code format! Use 6 digits."));
        }
    }

    private void handleRemove(Player player) {
        if (!authManager.hasSetup(player)) {
            player.sendMessage(MessageUtil.format(plugin.getConfig().getString("messages.not-setup")));
            return;
        }
        
        authManager.removePlayerData(player.getUniqueId());
        MapQRRenderer.destroyQRMap(player);
        player.sendMessage(MessageUtil.format(plugin.getConfig().getString("messages.removed")));
    }

    private void handleAdmin(CommandSender sender, String[] args) {
        String action = args[0].toLowerCase();
        
        switch (action) {
            case "remove":
                if (args.length < 2) {
                    sender.sendMessage(MessageUtil.format("&cUsage: /2fa admin remove <player>"));
                    return;
                }
                
                Player target = Bukkit.getPlayer(args[1]);
                UUID targetUuid = null;
                
                if (target != null) {
                    targetUuid = target.getUniqueId();
                } else {
                    @SuppressWarnings("deprecation")
                    org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[1]);
                    if (offlinePlayer.hasPlayedBefore()) {
                        targetUuid = offlinePlayer.getUniqueId();
                    }
                }
                
                if (targetUuid == null || !authManager.hasSetup(Bukkit.getPlayer(targetUuid))) {
                    sender.sendMessage(MessageUtil.format(plugin.getConfig().getString("messages.player-not-found")));
                    return;
                }
                
                authManager.removePlayerData(targetUuid);
                sender.sendMessage(MessageUtil.format(plugin.getConfig().getString("messages.admin-removed")
                        .replace("%player%", args[1])));
                break;
                
            case "reload":
                plugin.reloadConfig();
                sender.sendMessage(MessageUtil.format(plugin.getConfig().getString("messages.reload-success")));
                break;
                
            case "reset":
                if (args.length < 2) {
                    sender.sendMessage(MessageUtil.format("&cUsage: /2fa admin reset <player>"));
                    return;
                }
                
                Player resetTarget = Bukkit.getPlayer(args[1]);
                UUID resetUuid = null;
                
                if (resetTarget != null) {
                    resetUuid = resetTarget.getUniqueId();
                } else {
                    @SuppressWarnings("deprecation")
                    org.bukkit.OfflinePlayer resetOfflinePlayer = Bukkit.getOfflinePlayer(args[1]);
                    if (resetOfflinePlayer.hasPlayedBefore()) {
                        resetUuid = resetOfflinePlayer.getUniqueId();
                    }
                }
                
                if (resetUuid != null) {
                    authManager.removePlayerData(resetUuid);
                    authManager.cancelSetup(Bukkit.getPlayer(resetUuid));
                    sender.sendMessage(MessageUtil.format("&a2FA has been reset for " + args[1]));
                    sender.sendMessage(MessageUtil.format("&7They will need to set up 2FA again."));
                } else {
                    sender.sendMessage(MessageUtil.format("&cPlayer not found: " + args[1]));
                }
                break;
                
            default:
                sendAdminHelp(sender);
                break;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(MessageUtil.format("&6Just2FA Commands:"));
        sender.sendMessage(MessageUtil.format("&e/2fa setup &7- Set up 2FA for your account"));
        sender.sendMessage(MessageUtil.format("&e/2fa verify <code> &7- Verify your 2FA code"));
        sender.sendMessage(MessageUtil.format("&e/2fa remove &7- Remove 2FA from your account"));
        if (sender.hasPermission("just2fa.admin")) {
            sender.sendMessage(MessageUtil.format("&e/2fa admin &7- Admin commands"));
        }
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage(MessageUtil.format("&6Just2FA Admin Commands:"));
        sender.sendMessage(MessageUtil.format("&e/2fa admin remove <player> &7- Remove 2FA from a player"));
        sender.sendMessage(MessageUtil.format("&e/2fa admin reset <player> &7- Reset 2FA (fixes encryption issues)"));
        sender.sendMessage(MessageUtil.format("&e/2fa admin reload &7- Reload configuration"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("setup", "verify", "remove"));
            if (sender.hasPermission("just2fa.admin")) {
                completions.add("admin");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("admin") && sender.hasPermission("just2fa.admin")) {
            completions.addAll(Arrays.asList("remove", "reset", "reload"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("admin") && 
                   (args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("reset"))) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        }
        
        return completions;
    }
}