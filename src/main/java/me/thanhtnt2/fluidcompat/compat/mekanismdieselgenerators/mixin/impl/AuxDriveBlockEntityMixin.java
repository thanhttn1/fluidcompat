package me.thanhtnt2.fluidcompat.compat.mekanismdieselgenerators.mixin.impl;

import me.thanhtnt2.fluidcompat.compat.mekanismdieselgenerators.MdgPumpBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The only place Fluid Compat touches Mekanism Diesel Generators: the Create-powered Shaft Pump and Inline Pump.
 *
 * <p>Their {@code draw(int)} reads the block in front of the inlet and, when it is a water source, fills the tank from
 * it without ever taking anything out of the world. The two hooks below find out which block that is and then make the
 * world pay for exactly the water the tank is about to receive. The target is referenced by name so Fluid Compat loads
 * without the mod; {@code MekanismDieselGeneratorsMixinPlugin} decides whether this applies at all.
 */
@Mixin(targets = "com.jambonbeurre78.mdg.compat.create.AuxDriveBlockEntity", remap = false)
public class AuxDriveBlockEntityMixin {

    // The inlet position is a local variable of draw(), so it is noted here and read by the hook below, which runs
    // later in the same call. Both hooks live in this one mixin, so they are applied together or not at all.
    @Unique
    private BlockPos fluidcompat$inletCell;

    @Redirect(
            method = "draw(I)I",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;",
                    remap = false),
            remap = false)
    private FluidState fluidcompat$noteInletCell(Level level, BlockPos inletCell) {
        this.fluidcompat$inletCell = inletCell;
        return level.getFluidState(inletCell);
    }

    // Ordinal 0 is the fill of the water-source branch; DrawContract guarantees it before this mixin is applied
    @ModifyArg(
            method = "draw(I)I",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/fluids/capability/templates/FluidTank;fill(Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)I",
                    ordinal = 0,
                    remap = false),
            index = 0,
            remap = false)
    private FluidStack fluidcompat$payForDrawnWater(FluidStack requested) {
        return MdgPumpBridge.payForDrawnWater((BlockEntity) (Object) this, this.fluidcompat$inletCell, requested);
    }
}
