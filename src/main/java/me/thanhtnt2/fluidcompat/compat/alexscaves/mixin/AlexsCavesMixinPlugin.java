package me.thanhtnt2.fluidcompat.compat.alexscaves.mixin;

import me.thanhtnt2.fluidcompat.compat.GuardedCompatMixinPlugin;

/** Applies the Drain hook only if {@code DrainBlock} still has the {@code drainLogic} method the hook expects. */
public final class AlexsCavesMixinPlugin extends GuardedCompatMixinPlugin {

    private static final String DRAIN_BLOCK = "com.github.alexmodguy.alexscaves.server.block.DrainBlock";
    private static final String DRAIN_LOGIC_DESCRIPTOR =
            "(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V";

    public AlexsCavesMixinPlugin() {
        super("alexscaves", DRAIN_BLOCK,
                target -> declaresMethod(target, "drainLogic", DRAIN_LOGIC_DESCRIPTOR));
    }
}
