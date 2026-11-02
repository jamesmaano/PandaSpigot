package com.phrust.engine;

import net.minecraft.server.WorldServer;

/**
 * An engine-level system ticked once per world per server tick by {@link PHRustEngine}.
 * Runs on the main thread. Must never block (no I/O, no sleeping).
 */
public interface PHRustSubsystem {

    /** Short unique name, used in log messages. */
    String getName();

    /** Called once per world per server tick, on the main thread. */
    void tick(WorldServer world);
}
