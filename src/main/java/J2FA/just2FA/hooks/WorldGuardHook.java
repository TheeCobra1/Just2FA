package J2FA.just2FA.hooks;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class WorldGuardHook {
    private static boolean enabled = false;
    private static StateFlag REQUIRE_2FA_FLAG;
    private static StateFlag BYPASS_2FA_FLAG;
    private static StateFlag ALLOW_MOVEMENT_FLAG;
    private static WorldGuardPlugin worldGuardPlugin;

    public static void initialize() {
        Plugin wgPlugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
        if (wgPlugin instanceof WorldGuardPlugin) {
            worldGuardPlugin = (WorldGuardPlugin) wgPlugin;
            registerFlags();
            enabled = true;
            Bukkit.getLogger().info("[Just2FA] WorldGuard integration enabled!");
        }
    }

    private static void registerFlags() {
        try {
            FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
            
            StateFlag require2faFlag = new StateFlag("require-2fa", false);
            StateFlag bypass2faFlag = new StateFlag("bypass-2fa", false);
            StateFlag allowMovementFlag = new StateFlag("2fa-allow-movement", true);
            
            try {
                registry.register(require2faFlag);
                REQUIRE_2FA_FLAG = require2faFlag;
            } catch (FlagConflictException e) {
                Flag<?> existing = registry.get("require-2fa");
                if (existing instanceof StateFlag) {
                    REQUIRE_2FA_FLAG = (StateFlag) existing;
                }
            }
            
            try {
                registry.register(bypass2faFlag);
                BYPASS_2FA_FLAG = bypass2faFlag;
            } catch (FlagConflictException e) {
                Flag<?> existing = registry.get("bypass-2fa");
                if (existing instanceof StateFlag) {
                    BYPASS_2FA_FLAG = (StateFlag) existing;
                }
            }
            
            try {
                registry.register(allowMovementFlag);
                ALLOW_MOVEMENT_FLAG = allowMovementFlag;
            } catch (FlagConflictException e) {
                Flag<?> existing = registry.get("2fa-allow-movement");
                if (existing instanceof StateFlag) {
                    ALLOW_MOVEMENT_FLAG = (StateFlag) existing;
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[Just2FA] Failed to register WorldGuard flags: " + e.getMessage());
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean shouldRequire2FA(Player player) {
        return checkFlag(player, REQUIRE_2FA_FLAG);
    }

    public static boolean shouldBypass2FA(Player player) {
        return checkFlag(player, BYPASS_2FA_FLAG);
    }
    
    public static Boolean shouldAllowMovement(Player player) {
        if (!enabled || ALLOW_MOVEMENT_FLAG == null) {
            return null;
        }
        
        try {
            LocalPlayer localPlayer = worldGuardPlugin.wrapPlayer(player);
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            
            com.sk89q.worldedit.world.World world = null;
            try {
                Class<?> adapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
                world = (com.sk89q.worldedit.world.World) adapterClass
                        .getMethod("adapt", org.bukkit.World.class)
                        .invoke(null, player.getWorld());
            } catch (Exception e) {
                try {
                    world = (com.sk89q.worldedit.world.World) Class.forName("com.sk89q.worldedit.bukkit.BukkitWorld")
                            .getConstructor(org.bukkit.World.class)
                            .newInstance(player.getWorld());
                } catch (Exception ex) {
                    return null;
                }
            }
            
            RegionManager regions = container.get(world);
            if (regions == null) {
                return null;
            }
            
            com.sk89q.worldedit.math.BlockVector3 vector = com.sk89q.worldedit.math.BlockVector3.at(
                player.getLocation().getBlockX(),
                player.getLocation().getBlockY(),
                player.getLocation().getBlockZ()
            );
            ApplicableRegionSet set = regions.getApplicableRegions(vector);
            
            if (set.size() == 0) {
                return null;
            }
            
            return set.testState(localPlayer, ALLOW_MOVEMENT_FLAG);
        } catch (Exception e) {
            return null;
        }
    }
    
    private static boolean checkFlag(Player player, StateFlag flag) {
        if (!enabled || flag == null) {
            return false;
        }

        try {
            LocalPlayer localPlayer = worldGuardPlugin.wrapPlayer(player);
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            
            com.sk89q.worldedit.world.World world = null;
            try {
                Class<?> adapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
                world = (com.sk89q.worldedit.world.World) adapterClass
                        .getMethod("adapt", org.bukkit.World.class)
                        .invoke(null, player.getWorld());
            } catch (Exception e) {
                try {
                    world = (com.sk89q.worldedit.world.World) Class.forName("com.sk89q.worldedit.bukkit.BukkitWorld")
                            .getConstructor(org.bukkit.World.class)
                            .newInstance(player.getWorld());
                } catch (Exception ex) {
                    return false;
                }
            }
            
            RegionManager regions = container.get(world);
            if (regions == null) {
                return false;
            }
            
            com.sk89q.worldedit.math.BlockVector3 vector = com.sk89q.worldedit.math.BlockVector3.at(
                player.getLocation().getBlockX(),
                player.getLocation().getBlockY(),
                player.getLocation().getBlockZ()
            );
            ApplicableRegionSet set = regions.getApplicableRegions(vector);
            return set.testState(localPlayer, flag);
        } catch (Exception e) {
            return false;
        }
    }
}