package com.phrust.api.event;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fires whenever a mounted player sends vehicle-steering input
 * (PacketPlayInSteerVehicle on the wire — forward/sideways/jump/dismount).
 *
 * This is engine-level plumbing, not jetpack-specific: any future vehicle
 * (helicopter, car, boat-with-fuel, ...) listens to this same event rather
 * than each one patching NMS separately.
 *
 * Values match vanilla's raw steer packet:
 *   forward  > 0 = pushing forward, < 0 = pushing back
 *   sideways > 0 = one strafe direction, < 0 = the other
 *   jump     = space bar held while mounted
 *   dismount = shift/sneak held while mounted (vanilla's "get off" signal)
 *
 * What "forward"/"sideways" MEAN is entirely up to the listener — a normal
 * horse uses them as horizontal movement, but nothing stops a listener from
 * reinterpreting sideways as vertical thrust (see the Jetpack plugin).
 */
public class VehicleSteerEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final Entity vehicle;
    private final float sideways;
    private final float forward;
    private final boolean jump;
    private final boolean dismount;
    private boolean cancelled;

    public VehicleSteerEvent(Player player, Entity vehicle, float sideways, float forward, boolean jump, boolean dismount) {
        this.player = player;
        this.vehicle = vehicle;
        this.sideways = sideways;
        this.forward = forward;
        this.jump = jump;
        this.dismount = dismount;
    }

    public Player getPlayer() {
        return player;
    }

    /** The entity the player is currently riding when this input arrived. */
    public Entity getVehicle() {
        return vehicle;
    }

    public float getSideways() {
        return sideways;
    }

    public float getForward() {
        return forward;
    }

    public boolean isJump() {
        return jump;
    }

    public boolean isDismount() {
        return dismount;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
