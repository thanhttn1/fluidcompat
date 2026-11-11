package me.thanhtnt2.fluidcompat.compat.enderio.mixin;

import me.thanhtnt2.fluidcompat.compat.GuardedCompatMixinPlugin;
import me.thanhtnt2.fluidcompat.compat.enderio.guard.EnderIoShapes;

/** Applies the Drain hook only while drainFluids and isActive still have exactly the calls the hook steers. */
public final class EnderIoDrainMixinPlugin extends GuardedCompatMixinPlugin {

    public static final String MODULE_ID = "enderio_drain";
    public static final String TARGET = "com.enderio.enderio.content.machines.drain.DrainBlockEntity";

    public EnderIoDrainMixinPlugin() {
        super(MODULE_ID, TARGET, EnderIoShapes::isDrainHookable);
    }
}
