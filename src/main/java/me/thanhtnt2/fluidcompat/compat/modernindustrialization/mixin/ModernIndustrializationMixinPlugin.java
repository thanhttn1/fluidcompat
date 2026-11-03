package me.thanhtnt2.fluidcompat.compat.modernindustrialization.mixin;

import me.thanhtnt2.fluidcompat.compat.GuardedCompatMixinPlugin;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.service.MixinService;

/**
 * Applies the pump hook only when Modern Industrialization is installed and its pump still adds water
 * to its tank from {@code tick()}. A missing or changed MI disables just this hook with a warning.
 */
public final class ModernIndustrializationMixinPlugin extends GuardedCompatMixinPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("FluidCompat/Mixin");

    private static final String TANK_INTERNAL_NAME = "aztech/modern_industrialization/inventory/ConfigurableFluidStack";
    private static final String THIN_WATER_MIXIN = "AbstractWaterPumpThinWaterMixin";
    private static final String PUMP_CLASS = "aztech.modern_industrialization.machines.blockentities.AbstractWaterPumpBlockEntity";
    private static final String FLUID_STATE_INTERNAL_NAME = "net/minecraft/world/level/material/FluidState";
    private static final String FLUID_STATE_TYPE_DESCRIPTOR = "()Lnet/minecraft/world/level/material/Fluid;";

    public ModernIndustrializationMixinPlugin() {
        super("modern_industrialization",
                "aztech.modern_industrialization.machines.blockentities.AbstractWaterPumpBlockEntity",
                ModernIndustrializationMixinPlugin::tickAddsWaterToTank);
    }

    /**
     * The thin-water redirects are optional ({@code require = 0}), so without this check a changed Modern
     * Industrialization would turn the feature off without a word. Here it is decided up front and logged.
     */
    @Override
    public boolean shouldApplyMixin(String targetClass, String mixinClassName) {
        boolean pumpHookApplies = super.shouldApplyMixin(targetClass, mixinClassName);
        if (!pumpHookApplies || !mixinClassName.endsWith(THIN_WATER_MIXIN)) {
            return pumpHookApplies;
        }
        boolean fits = thinWaterHookFits();
        if (!fits) {
            LOGGER.info("[modern_industrialization] getWaterSourceCount() changed shape, thin water pumping disabled. "
                    + "Pumps are still limited by the world; update Fluid Compat to get it back.");
        }
        return fits;
    }

    private static boolean thinWaterHookFits() {
        try {
            ClassNode target = MixinService.getService().getBytecodeProvider().getClassNode(PUMP_CLASS);
            for (MethodNode method : target.methods) {
                if ("getWaterSourceCount".equals(method.name) && "()I".equals(method.desc)) {
                    // Both redirects replace every such call, so each must occur exactly once
                    return countFluidStateCalls(method, "isSource", "()Z") == 1
                            && countFluidStateCalls(method, "getType", FLUID_STATE_TYPE_DESCRIPTOR) == 1;
                }
            }
        } catch (Exception exception) {
            // Same reason as in the base class: the checked exception types differ between Mixin versions
            return false;
        }
        return false;
    }

    private static int countFluidStateCalls(MethodNode method, String name, String descriptor) {
        int calls = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && name.equals(call.name)
                    && descriptor.equals(call.desc)
                    && FLUID_STATE_INTERNAL_NAME.equals(call.owner)) {
                calls++;
            }
        }
        return calls;
    }

    private static boolean tickAddsWaterToTank(ClassNode target) {
        for (MethodNode method : target.methods) {
            if ("tick".equals(method.name) && "()V".equals(method.desc) && callsTankIncrement(method)) {
                return true;
            }
        }
        return false;
    }

    private static boolean callsTankIncrement(MethodNode method) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && "increment".equals(call.name)
                    && TANK_INTERNAL_NAME.equals(call.owner)) {
                return true;
            }
        }
        return false;
    }
}
