package me.thanhtnt2.fluidcompat.core;

import me.thanhtnt2.fluidcompat.api.FluidCompatApi;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Data attachments registered by Fluid Compat. */
public final class FluidCompatAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, FluidCompatApi.MOD_ID);

    // Persisted with the block entity so the statistics survive chunk unloads and restarts
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PumpStatistics>> PUMP_STATISTICS =
            ATTACHMENT_TYPES.register("pump_statistics", () -> AttachmentType
                    .builder(() -> PumpStatistics.EMPTY)
                    .serialize(PumpStatistics.CODEC)
                    .build());

    private FluidCompatAttachments() {
    }
}
