package me.thanhtnt2.fluidcompat.compat.mekanismelements.mixin.impl;

import me.thanhtnt2.fluidcompat.compat.mekanismelements.SeawaterPumpBridge;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * The only place Fluid Compat touches the Seawater Pump. Target is referenced by name so Fluid Compat
 * loads without Mekanism Elements; {@code MekanismElementsMixinPlugin} decides whether this applies.
 */
@Mixin(targets = "fixdol.mekanismelements.common.tile.machine.TileEntitySeawaterPump", remap = false)
public class TileEntitySeawaterPumpMixin {

    // Hooking the stack handed to the tank means the world pays for exactly what the pump really makes
    @ModifyArg(
            method = "onUpdateServer",
            at = @At(
                    value = "INVOKE",
                    target = "Lmekanism/common/capabilities/fluid/BasicFluidTank;insert(Lnet/neoforged/neoforge/fluids/FluidStack;Lmekanism/api/Action;Lmekanism/api/AutomationType;)Lnet/neoforged/neoforge/fluids/FluidStack;",
                    remap = false),
            index = 0,
            remap = false)
    private FluidStack fluidcompat$chargeWorldForSeawater(FluidStack seawater) {
        return SeawaterPumpBridge.chargeWorld((BlockEntity) (Object) this, seawater);
    }
}
