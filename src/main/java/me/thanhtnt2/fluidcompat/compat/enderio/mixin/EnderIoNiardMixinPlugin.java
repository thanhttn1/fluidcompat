package me.thanhtnt2.fluidcompat.compat.enderio.mixin;

import me.thanhtnt2.fluidcompat.compat.GuardedCompatMixinPlugin;
import me.thanhtnt2.fluidcompat.compat.enderio.guard.EnderIoShapes;

/** Applies the Niard hook only while placeFluid and consumeResources still have exactly the calls the hook steers. */
public final class EnderIoNiardMixinPlugin extends GuardedCompatMixinPlugin {

    public static final String MODULE_ID = "enderio_niard";
    public static final String TARGET = "com.enderio.enderio.content.machines.niard.NiardBlockEntity";

    public EnderIoNiardMixinPlugin() {
        super(MODULE_ID, TARGET, EnderIoShapes::isNiardHookable);
    }
}
