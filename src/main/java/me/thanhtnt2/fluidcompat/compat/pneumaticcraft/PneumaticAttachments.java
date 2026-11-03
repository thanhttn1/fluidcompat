package me.thanhtnt2.fluidcompat.compat.pneumaticcraft;

import me.thanhtnt2.fluidcompat.api.FluidCompatApi;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Registered from the mod constructor because registries must be filled before setup.
 * It holds no PneumaticCraft types, so registering it is safe when PneumaticCraft is absent.
 */
public final class PneumaticAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, FluidCompatApi.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<HopperPullSettings>> HOPPER_PULL_SETTINGS =
            ATTACHMENT_TYPES.register("hopper_pull_settings", () -> AttachmentType
                    .builder(() -> HopperPullSettings.DEFAULT)
                    .serialize(HopperPullSettings.CODEC)
                    .build());

    private PneumaticAttachments() {
    }
}
