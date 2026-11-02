package com.phrust.engine;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import net.minecraft.server.PacketPlayInCustomPayload;
import org.apache.logging.log4j.LogManager;

/**
 * Client-brand whitelist. The client reports its brand over MC|Brand on join. This blocks
 * clients that report honestly; it is NOT proof against a spoofed brand string.
 * Decodes the string itself (varint length + UTF-8) from a duplicate buffer, so it never
 * consumes the packet and does not depend on PacketDataSerializer method names.
 */
public final class PHRustBrand {

    private PHRustBrand() {}

    /** @return kick message (colour codes already translated) if the player must be kicked, else null. */
    public static String check(String playerName, PacketPlayInCustomPayload packet) {
        try {
            if (!"MC|Brand".equals(packet.a())) {
                return null;
            }
            String brand = readBrand(packet.b());
            if (brand == null) {
                return null; // malformed payload: never kick over it
            }
            if (PHRustConfig.logBrands) {
                LogManager.getLogger("PHRust").info(playerName + " reported client brand: \"" + brand + "\"");
            }
            if (!PHRustConfig.brandWhitelistEnabled) {
                return null;
            }
            for (String allowed : PHRustConfig.allowedBrands) {
                if (allowed.equalsIgnoreCase(brand)) {
                    return null;
                }
            }
            return org.bukkit.ChatColor.translateAlternateColorCodes('&', PHRustConfig.brandKickMessage);
        } catch (Throwable t) {
            return null;
        }
    }

    private static String readBrand(ByteBuf data) {
        if (data == null) {
            return null;
        }
        ByteBuf buf = data.duplicate();
        int length = 0;
        int shift = 0;
        byte b;
        do {
            if (!buf.isReadable() || shift >= 5) {
                return null;
            }
            b = buf.readByte();
            length |= (b & 0x7F) << (7 * shift++);
        } while ((b & 0x80) != 0);
        if (length < 0 || length > buf.readableBytes() || length > 32767 * 3) {
            return null;
        }
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
