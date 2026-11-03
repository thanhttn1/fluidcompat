package me.thanhtnt2.fluidcompat.compat.pneumaticcraft.mixin.impl;

import me.thanhtnt2.fluidcompat.compat.pneumaticcraft.PneumaticPickupBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The only place Fluid Compat touches PneumaticCraft. The target is referenced by name so Fluid Compat loads
 * without PneumaticCraft; {@code PneumaticCraftMixinPlugin} decides at boot whether this is applied at all.
 */
@Mixin(targets = "me.desht.pneumaticcraft.common.util.FluidUtils", remap = false)
public class FluidUtilsMixin {

    // Pure observer: lets the Liquid Hopper hook know that PneumaticCraft's own source pickup ran for this block
    @Inject(method = "tryPickupFluid", at = @At("HEAD"), remap = false)
    private static void fluidcompat$notePickupAttempt(
            IFluidHandler tank, Level level, BlockPos pos, boolean playSound,
            IFluidHandler.FluidAction action, CallbackInfoReturnable<FluidStack> callback) {
        PneumaticPickupBridge.notePickupAttempt(level, pos);
    }

    // The only config read inside tryPickupFluid is the "don't update infinite water sources" flag
    @Redirect(
            method = "tryPickupFluid",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/common/ModConfigSpec$BooleanValue;get()Ljava/lang/Object;",
                    remap = false),
            remap = false)
    private static Object fluidcompat$ignoreInfiniteWaterRule(ModConfigSpec.BooleanValue pneumaticCraftOption) {
        return PneumaticPickupBridge.dontUpdateInfiniteWaterSources(pneumaticCraftOption);
    }
}
