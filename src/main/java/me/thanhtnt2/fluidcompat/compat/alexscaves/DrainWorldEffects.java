package me.thanhtnt2.fluidcompat.compat.alexscaves;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import me.thanhtnt2.fluidcompat.logging.FluidCompatLog;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Plays Alex's Caves' own drain sound and bubble effect by sending its world-event packet.
 *
 * <p>Looked up by name at runtime so Fluid Compat has no compile-time link to Alex's Caves. If the packet
 * class ever changes, the effect is simply skipped: it is cosmetic and must never break the Drain.
 */
final class DrainWorldEffects {

    /** Event ids the original Drain uses: 3 = fluid was taken, 4 = fluid was put back. */
    static final int FLUID_TAKEN = 3;
    static final int FLUID_RELEASED = 4;

    private static final String MESSAGE_CLASS = "com.github.alexmodguy.alexscaves.server.message.WorldEventMessage";
    private static final String MOD_CLASS = "com.github.alexmodguy.alexscaves.AlexsCaves";

    private static volatile boolean resolved;
    private static volatile MethodHandle messageFactory;
    private static volatile MethodHandle broadcaster;

    private DrainWorldEffects() {
    }

    static void play(int eventId, BlockPos drainPos) {
        if (!resolveHandles()) {
            return;
        }
        try {
            CustomPacketPayload message = (CustomPacketPayload) messageFactory.invoke(
                    eventId, drainPos.getX(), drainPos.getY(), drainPos.getZ());
            broadcaster.invoke(message);
        } catch (Throwable error) {
            // Cosmetic only: stop trying instead of logging every second
            FluidCompatLog.warn("[alexscaves] Drain effect disabled after an error: {}", error.toString());
            messageFactory = null;
        }
    }

    private static boolean resolveHandles() {
        if (!resolved) {
            resolveOnce();
        }
        // Plain volatile reads: no lock on the hot path once the lookup has run
        return messageFactory != null && broadcaster != null;
    }

    private static synchronized void resolveOnce() {
        if (resolved) {
            return;
        }
        try {
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            Class<?> messageClass = Class.forName(MESSAGE_CLASS);
            Class<?> modClass = Class.forName(MOD_CLASS);
            broadcaster = lookup.findStatic(modClass, "sendMSGToAll",
                    MethodType.methodType(void.class, CustomPacketPayload.class));
            messageFactory = lookup.findConstructor(messageClass,
                    MethodType.methodType(void.class, int.class, int.class, int.class, int.class));
        } catch (ReflectiveOperationException | LinkageError error) {
            FluidCompatLog.warn("[alexscaves] Drain effects unavailable ({}); only the fluid logic is hooked", error.toString());
        } finally {
            resolved = true; // published last, so a reader that sees it also sees the handles
        }
    }
}
