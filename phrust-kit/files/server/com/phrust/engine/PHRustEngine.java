package com.phrust.engine;

import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.server.WorldServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Entry point for PHRust engine-level systems.
 *
 * tick() is called from inside the world-tick try/catch in MinecraftServer. Anything that
 * escapes there becomes a ReportedException and STOPS THE SERVER (looks like a random
 * restart). So this class NEVER throws: each subsystem is isolated, and one that keeps
 * failing is disabled after phrust.yml engine.max-subsystem-failures consecutive failures.
 */
public final class PHRustEngine {

    private static final Logger LOGGER = LogManager.getLogger("PHRust");

    private static final class Entry {
        final PHRustSubsystem subsystem;
        int failures;
        boolean disabled;

        Entry(PHRustSubsystem subsystem) {
            this.subsystem = subsystem;
        }
    }

    // CopyOnWrite: registration is rare, iteration is every tick.
    private static final CopyOnWriteArrayList<Entry> SUBSYSTEMS = new CopyOnWriteArrayList<Entry>();

    private PHRustEngine() {}

    /** Registers a subsystem. Safe to call from any thread. */
    public static void register(PHRustSubsystem subsystem) {
        if (subsystem == null) {
            throw new IllegalArgumentException("subsystem");
        }
        SUBSYSTEMS.add(new Entry(subsystem));
        LOGGER.info("Registered engine subsystem: " + subsystem.getName());
    }

    /** Called by MinecraftServer once per world per tick (main thread). Never throws. */
    public static void tick(WorldServer world) {
        try {
            if (SUBSYSTEMS.isEmpty()) {
                return; // fast path: no iterator allocation
            }
            for (Entry entry : SUBSYSTEMS) {
                if (entry.disabled) {
                    continue;
                }
                try {
                    entry.subsystem.tick(world);
                    entry.failures = 0;
                } catch (Throwable t) {
                    entry.failures++;
                    if (entry.failures == 1) {
                        LOGGER.error("Engine subsystem '" + entry.subsystem.getName() + "' threw during tick", t);
                    }
                    if (entry.failures >= PHRustConfig.maxSubsystemFailures) {
                        entry.disabled = true;
                        LOGGER.error("Engine subsystem '" + entry.subsystem.getName() + "' failed "
                                + entry.failures + " ticks in a row and has been DISABLED to protect the server.");
                    }
                }
            }
        } catch (Throwable outer) {
            LOGGER.error("PHRust engine tick failed (server kept running)", outer);
        }
    }
}
