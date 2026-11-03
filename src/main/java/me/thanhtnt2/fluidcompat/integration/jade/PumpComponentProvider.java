package me.thanhtnt2.fluidcompat.integration.jade;

import java.util.Locale;

import me.thanhtnt2.fluidcompat.api.FluidCompatApi;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Draws the finite-fluid lines in Jade's tooltip. */
enum PumpComponentProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(PumpJadeKeys.FINITE)) {
            return;
        }
        boolean finite = data.getBoolean(PumpJadeKeys.FINITE);
        tooltip.add(Component.translatable(finite ? "jade.fluidcompact.pump.finite" : "jade.fluidcompact.pump.infinite")
                .withStyle(finite ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        if (!finite || !data.contains(PumpJadeKeys.TOTAL_DRAINED_LEVELS)) {
            return;
        }
        tooltip.add(Component.translatable("jade.fluidcompact.pump.drained",
                formatBuckets(data.getLong(PumpJadeKeys.TOTAL_DRAINED_LEVELS))));
        long starvedIntakes = data.getLong(PumpJadeKeys.STARVED_INTAKES);
        if (starvedIntakes > 0L) {
            tooltip.add(Component.translatable("jade.fluidcompact.pump.starved", starvedIntakes)
                    .withStyle(ChatFormatting.RED));
        }
    }

    private static String formatBuckets(long levels) {
        return String.format(Locale.ROOT, "%.2f", (double) levels / FluidCompatApi.LEVELS_PER_BUCKET);
    }

    @Override
    public ResourceLocation getUid() {
        return PumpJadeKeys.PROVIDER_ID;
    }
}
