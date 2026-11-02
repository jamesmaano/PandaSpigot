package net.minecraft.server;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PHRust NMS bridge for the jetpack mechanic. PUBLIC API IS FROZEN: the PHRustJetpack plugin
 * compiles against these exact signatures (setListener, setJetpacking, isJetpacking,
 * handleJetpackMovement, Listener.onJetpackSteer). Only additions are allowed.
 */
public class JetpackEngine {

    private static final Set<UUID> jetpacking = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
    private static volatile Listener listener;

    public interface Listener {
        /**
         * @param strafe  packet.a() - positive while holding A, negative while holding D
         * @param forward packet.b() - positive while holding W, negative while holding S
         * @param jump    packet.c() - space bar
         * @param sneak   packet.d() - shift (dismount signal)
         */
        void onJetpackSteer(UUID playerId, float strafe, float forward, boolean jump, boolean sneak);
    }

    private JetpackEngine() {}

    public static void setListener(Listener l) {
        listener = l;
    }

    public static void setJetpacking(UUID playerId, boolean value) {
        if (value) {
            jetpacking.add(playerId);
        } else {
            jetpacking.remove(playerId);
        }
    }

    /** NEW: called on disconnect so offline players never leak in the set. */
    public static void clearPlayer(UUID playerId) {
        jetpacking.remove(playerId);
    }

    public static boolean isJetpacking(UUID playerId) {
        return jetpacking.contains(playerId);
    }

    /** @return true if consumed by the jetpack system (caller skips vanilla steering). */
    public static boolean handleJetpackMovement(PacketPlayInSteerVehicle packet, EntityPlayer player) {
        UUID id = player.getUniqueID();
        if (!jetpacking.contains(id)) {
            return false;
        }
        Listener l = listener;
        if (l != null) {
            try { // a plugin bug must never propagate into packet handling
                l.onJetpackSteer(id, packet.a(), packet.b(), packet.c(), packet.d());
            } catch (Throwable t) {
                org.apache.logging.log4j.LogManager.getLogger("PHRust").error("Jetpack listener threw", t);
            }
        }
        return true;
    }
}
