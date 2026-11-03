package me.thanhtnt2.fluidcompat.compat.pneumaticcraft;

import me.thanhtnt2.fluidcompat.api.FluidCompatApi;
import me.thanhtnt2.fluidcompat.compat.common.CompatGate;
import me.thanhtnt2.fluidcompat.config.FluidCompatModulesConfig;
import me.thanhtnt2.fluidcompat.logging.FluidCompatLog;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Decision logic for the PneumaticCraft pickup hook. Has no reference to PneumaticCraft classes.
 *
 * <p>PneumaticCraft's shared pickup routine (Liquid Hopper, Gas Lift) skips removing a water source that has
 * two water neighbours, mimicking vanilla's infinite water. That rule is a config value on its side; the hook
 * simply answers "false" to it while water is finite, so PneumaticCraft's own removal code runs unchanged.
 */
public final class PneumaticPickupBridge {

    // PneumaticCraft only calls its source pickup for a hopper that has the Dispenser Upgrade, so seeing the call
    // at a given position on a given tick tells the leftover-water pull that the upgrade is present.
    // Two plain numbers, only ever touched on the server thread.
    // The dimension is part of the marker: the same coordinates exist in every dimension, and the game time is shared.
    // A ResourceKey is a small interned id, so unlike a Level it cannot keep a world alive
    private static ResourceKey<Level> lastPickupDimension;
    private static long lastPickupPos = Long.MIN_VALUE;
    private static long lastPickupGameTime = -1L;

    private PneumaticPickupBridge() {
    }

    /** Called right before PneumaticCraft tries to pick up the fluid block at {@code pos}. */
    public static void notePickupAttempt(Level level, BlockPos pos) {
        if (!level.isClientSide()) {
            lastPickupDimension = level.dimension();
            lastPickupPos = pos.asLong();
            lastPickupGameTime = level.getGameTime();
        }
    }

    /** True when PneumaticCraft itself just tried to pick up the block at {@code pos} (this tick). */
    public static boolean pickupWasAttempted(Level level, BlockPos pos) {
        return lastPickupGameTime == level.getGameTime() && lastPickupPos == pos.asLong()
                && level.dimension().equals(lastPickupDimension);
    }

    /**
     * @param pneumaticCraftOption PneumaticCraft's own "don't update infinite water sources" setting
     * @return the value PneumaticCraft should see: false while Fluid Compat wants every source removed
     */
    public static Object dontUpdateInfiniteWaterSources(ModConfigSpec.BooleanValue pneumaticCraftOption) {
        if (shouldRemoveEverySource()) {
            return Boolean.FALSE;
        }
        return pneumaticCraftOption.get();
    }

    private static boolean shouldRemoveEverySource() {
        if (!FluidCompatApi.isReady()) {
            return false;
        }
        try {
            boolean forced = CompatGate.masterEnabled()
                    && FluidCompatModulesConfig.PNC_PICKUP_ENABLED.get()
                    && FluidCompatApi.service().isFinite(Fluids.WATER);
            if (forced) {
                FluidCompatLog.debugThrottled("pneumaticcraft-pickup",
                        "PneumaticCraft pickup: infinite-water exemption switched off, the source block is removed");
            }
            return forced;
        } catch (RuntimeException | LinkageError error) {
            // Fail open: a broken hook must never stop PneumaticCraft's machines from working
            FluidCompatLog.errorThrottled("pneumaticcraft-pickup", "PneumaticCraft pickup hook failed", error);
            return false;
        }
    }
}
