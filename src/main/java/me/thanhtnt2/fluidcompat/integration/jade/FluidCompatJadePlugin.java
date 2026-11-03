package me.thanhtnt2.fluidcompat.integration.jade;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Discovered by Jade through its annotation scan, so it is never loaded when Jade is absent.
 * Providers are registered for all block entities and filter themselves by name,
 * which keeps this class free of Modern Industrialization references.
 */
@WailaPlugin
public final class FluidCompatJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(PumpServerDataProvider.INSTANCE, BlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(PumpComponentProvider.INSTANCE, Block.class);
    }
}
