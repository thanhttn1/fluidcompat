package me.thanhtnt2.fluidcompat.compat.pneumaticcraft.hopper.mixin.impl;

import me.thanhtnt2.fluidcompat.compat.pneumaticcraft.PneumaticHopperPuller;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The only place Fluid Compat touches the Liquid Hopper. It runs after PneumaticCraft's own import logic and only
 * when that imported nothing, so PneumaticCraft's behaviour is never replaced, only completed.
 */
@Mixin(targets = "me.desht.pneumaticcraft.common.block.entity.hopper.LiquidHopperBlockEntity", remap = false)
public class LiquidHopperBlockEntityMixin {

    @Inject(method = "doImport", at = @At("RETURN"), cancellable = true, remap = false)
    private void fluidcompat$pullLeftoverWater(int rateUnits, CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValueZ()) {
            return;
        }
        if (PneumaticHopperPuller.tryPullLeftover((BlockEntity) (Object) this, rateUnits)) {
            callback.setReturnValue(Boolean.TRUE);
        }
    }
}
