package J2FA.just2FA.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MapQRRenderer extends MapRenderer {
    private final String data;
    private final BufferedImage qrImage;
    private final Map<UUID, Integer> renderCount = new HashMap<>();
    private static final Map<UUID, MapView> playerMaps = new HashMap<>();
    private static final Map<UUID, ItemStack> playerMapItems = new HashMap<>();

    public MapQRRenderer(String data) throws WriterException {
        super();
        this.data = data;
        this.qrImage = generateQRCodeImage(data);
        try {
            MapRenderer.class.getConstructor(boolean.class);
            this.getClass().getSuperclass().getConstructor(boolean.class).newInstance(false);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        if (player == null) {
            return;
        }
        
        int count = renderCount.getOrDefault(player.getUniqueId(), 0);
        if (count >= 3) {
            return;
        }
        
        try {
            canvas.drawImage(0, 0, qrImage);
            renderCount.put(player.getUniqueId(), count + 1);
        } catch (Exception e) {
            canvas.drawText(10, 50, MinecraftFont.Font, "Failed to render");
            canvas.drawText(10, 60, MinecraftFont.Font, "QR Code");
        }
    }

    private static BufferedImage generateQRCodeImage(String data) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.MARGIN, 0);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.L);
        
        BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 0, 0, hints);
        
        int qrWidth = bitMatrix.getWidth();
        int qrHeight = bitMatrix.getHeight();
        
        int mapSize = 128;
        int padding = 10;
        int availableSize = mapSize - (padding * 2);
        
        int cellSize = Math.min(availableSize / qrWidth, availableSize / qrHeight);
        if (cellSize < 1) cellSize = 1;
        
        int totalWidth = qrWidth * cellSize;
        int totalHeight = qrHeight * cellSize;
        int offsetX = (mapSize - totalWidth) / 2;
        int offsetY = (mapSize - totalHeight) / 2;
        
        BufferedImage image = new BufferedImage(mapSize, mapSize, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, mapSize, mapSize);
        
        graphics.setColor(Color.BLACK);
        for (int y = 0; y < qrHeight; y++) {
            for (int x = 0; x < qrWidth; x++) {
                if (bitMatrix.get(x, y)) {
                    graphics.fillRect(
                        offsetX + (x * cellSize), 
                        offsetY + (y * cellSize), 
                        cellSize, 
                        cellSize
                    );
                }
            }
        }
        
        graphics.setColor(Color.DARK_GRAY);
        graphics.drawRect(offsetX - 2, offsetY - 2, totalWidth + 3, totalHeight + 3);
        
        graphics.dispose();
        return image;
    }

    @SuppressWarnings("deprecation")
    public static void giveQRMap(Player player, String data, String issuer) {
        try {
            MapView existingMap = playerMaps.get(player.getUniqueId());
            
            if (existingMap != null) {
                for (MapRenderer renderer : existingMap.getRenderers()) {
                    existingMap.removeRenderer(renderer);
                }
            }
            
            MapView map = Bukkit.createMap(player.getWorld());
            playerMaps.put(player.getUniqueId(), map);
            
            for (MapRenderer renderer : map.getRenderers()) {
                map.removeRenderer(renderer);
            }
            
            MapQRRenderer qrRenderer = new MapQRRenderer(data);
            map.addRenderer(qrRenderer);
            map.setScale(MapView.Scale.CLOSEST);
            map.setTrackingPosition(false);
            map.setCenterX(0);
            map.setCenterZ(0);
            
            try {
                map.getClass().getMethod("setUnlimitedTracking", boolean.class).invoke(map, false);
            } catch (Exception ignored) {
            }
            
            ItemStack mapItem;
            Material mapMaterial = Material.MAP;
            try {
                mapMaterial = Material.valueOf("FILLED_MAP");
                mapItem = new ItemStack(mapMaterial);
            } catch (IllegalArgumentException e) {
                mapItem = new ItemStack(Material.MAP);
            }
            
            ItemMeta meta = mapItem.getItemMeta();
            boolean mapSet = false;
            
            if (meta != null) {
                try {
                    Class<?> mapMetaClass = Class.forName("org.bukkit.inventory.meta.MapMeta");
                    if (mapMetaClass.isInstance(meta)) {
                        try {
                            mapMetaClass.getMethod("setMapView", MapView.class).invoke(meta, map);
                            mapItem.setItemMeta(meta);
                            mapSet = true;
                        } catch (NoSuchMethodException e) {
                            try {
                                mapMetaClass.getMethod("setMapId", Integer.class).invoke(meta, map.getId());
                                mapItem.setItemMeta(meta);
                                mapSet = true;
                            } catch (Exception ignored) {
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            
            if (!mapSet) {
                try {
                    short mapId = (short) map.getId();
                    mapItem.setDurability(mapId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            meta = mapItem.getItemMeta();
            
            if (meta != null) {
                meta.setDisplayName(MessageUtil.format("&6&l2FA Setup - " + issuer));
                meta.setLore(java.util.Arrays.asList(
                    MessageUtil.format("&7Scan this QR code with"),
                    MessageUtil.format("&7your authenticator app"),
                    MessageUtil.format(""),
                    MessageUtil.format("&eApps: Google Authenticator"),
                    MessageUtil.format("&eAuthy, Microsoft Authenticator"),
                    MessageUtil.format(""),
                    MessageUtil.format("&cKeep this map safe!"),
                    MessageUtil.format("&cDo not share with anyone!")
                ));
                mapItem.setItemMeta(meta);
            }
            
            playerMapItems.put(player.getUniqueId(), mapItem);
            
            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(mapItem);
            } else {
                player.getWorld().dropItem(player.getLocation(), mapItem);
                player.sendMessage(MessageUtil.format("&eYour inventory was full! The QR map was dropped."));
            }
            
            player.sendMessage(MessageUtil.format("&aA map with your QR code has been given to you!"));
            player.sendMessage(MessageUtil.format("&eScan it with your authenticator app."));
            player.sendMessage(MessageUtil.format("&7The map will be destroyed after successful setup."));
            
        } catch (Exception e) {
            player.sendMessage(MessageUtil.format("&cFailed to create QR code map: " + e.getMessage()));
            e.printStackTrace();
        }
    }
    
    public static void removePlayerMap(Player player) {
        playerMaps.remove(player.getUniqueId());
        playerMapItems.remove(player.getUniqueId());
    }
    
    public static void destroyQRMap(Player player) {
        UUID uuid = player.getUniqueId();
        MapView map = playerMaps.get(uuid);
        ItemStack mapItem = playerMapItems.get(uuid);
        
        if (mapItem != null) {
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (item != null && item.isSimilar(mapItem)) {
                    player.getInventory().setItem(i, null);
                    player.sendMessage(MessageUtil.format("&aYour 2FA QR map has been securely destroyed."));
                    break;
                }
            }
        }
        
        playerMaps.remove(uuid);
        playerMapItems.remove(uuid);
        
        if (map != null) {
            for (MapRenderer renderer : map.getRenderers()) {
                map.removeRenderer(renderer);
            }
        }
    }
    
    public static boolean isQRMap(ItemStack item) {
        if (item == null || (!item.getType().name().contains("MAP"))) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            String name = meta.getDisplayName();
            return name.contains("2FA Setup");
        }
        
        return false;
    }
    
    public static MapView getPlayerMap(Player player) {
        return playerMaps.get(player.getUniqueId());
    }
}