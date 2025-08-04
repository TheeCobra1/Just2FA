package J2FA.just2FA.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class QRCodeUtil {
    
    private static final String WHITE_BLOCK = "§f█";
    private static final String BLACK_BLOCK = "§0█";
    private static final String EMPTY = "  ";
    
    public static void sendQRCode(Player player, String url) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.MARGIN, 0);
        
        BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, 25, 25, hints);
        
        player.sendMessage("");
        player.sendMessage(MessageUtil.format("&7Scan this QR code with your authenticator app:"));
        player.sendMessage("");
        
        for (int y = 0; y < bitMatrix.getHeight(); y++) {
            StringBuilder line = new StringBuilder();
            line.append("    ");
            
            for (int x = 0; x < bitMatrix.getWidth(); x++) {
                if (bitMatrix.get(x, y)) {
                    line.append(BLACK_BLOCK);
                } else {
                    line.append(WHITE_BLOCK);
                }
            }
            
            player.sendMessage(line.toString());
        }
        
        player.sendMessage("");
    }
    
    public static String generateQRCodeASCII(String url) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.MARGIN, 1);
        
        BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, 21, 21, hints);
        StringBuilder result = new StringBuilder();
        
        for (int y = 0; y < bitMatrix.getHeight(); y++) {
            for (int x = 0; x < bitMatrix.getWidth(); x++) {
                result.append(bitMatrix.get(x, y) ? "██" : "  ");
            }
            result.append("\n");
        }
        
        return result.toString();
    }
}