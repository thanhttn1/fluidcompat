package me.thanhtnt2.fluidcompat.integration.jade;

import me.thanhtnt2.fluidcompat.api.FluidCompatApi;
import me.thanhtnt2.fluidcompat.core.FluidCompatAttachments;
import me.thanhtnt2.fluidcompat.core.PumpStatistics;
import me.thanhtnt2.fluidcompat.compat.modernindustrialization.MiPumpTargeting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/** Sends finite-fluid information about a pump to the client that is looking at it. */
enum PumpServerDataProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockEntity pump = accessor.getBlockEntity();
        if (!MiPumpTargeting.isWaterPump(pump) || !FluidCompatApi.isReady()) {
            return;
        }
        data.putBoolean(PumpJadeKeys.FINITE, FluidCompatApi.service().isFinite(Fluids.WATER));
        if (!pump.hasData(FluidCompatAttachments.PUMP_STATISTICS.get())) {
            return;
        }
        PumpStatistics statistics = pump.getData(FluidCompatAttachments.PUMP_STATISTICS.get());
        data.putLong(PumpJadeKeys.TOTAL_DRAINED_LEVELS, statistics.totalDrainedLevels());
        data.putLong(PumpJadeKeys.STARVED_INTAKES, statistics.starvedIntakes());
    }

    @Override
    public boolean shouldRequestData(BlockAccessor accessor) {
        // Cheap client-side filter so Jade does not query the server for every hovered block
        return MiPumpTargeting.isWaterPump(accessor.getBlockEntity());
    }

    @Override
    public ResourceLocation getUid() {
        return PumpJadeKeys.PROVIDER_ID;
    }
}
