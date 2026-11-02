package net.minecraft.server;

import com.phrust.api.event.VehicleSteerEvent;

/**
 * Single entry point for steering hooks, called from PlayerConnection#a(PacketPlayInSteerVehicle).
 * Order matters: jetpack first (identical to previous behaviour), then VehicleSteerEvent.
 */
public final class PHRustSteer {

    private PHRustSteer() {}

    /** @return true if the packet was consumed and vanilla steering must be skipped. */
    public static boolean handle(PacketPlayInSteerVehicle packet, EntityPlayer player) {
        try {
            if (JetpackEngine.handleJetpackMovement(packet, player)) {
                return true;
            }
            // VehicleSteerEvent was declared in the API but never fired anywhere. Only pay for
            // it when the player is riding AND some plugin actually listens.
            if (player.vehicle != null && VehicleSteerEvent.getHandlerList().getRegisteredListeners().length > 0) {
                VehicleSteerEvent event = new VehicleSteerEvent(player.getBukkitEntity(),
                        player.vehicle.getBukkitEntity(), packet.a(), packet.b(), packet.c(), packet.d());
                org.bukkit.Bukkit.getPluginManager().callEvent(event);
                return event.isCancelled();
            }
        } catch (Throwable t) {
            org.apache.logging.log4j.LogManager.getLogger("PHRust").error("Steer hook failed", t);
        }
        return false;
    }
}
